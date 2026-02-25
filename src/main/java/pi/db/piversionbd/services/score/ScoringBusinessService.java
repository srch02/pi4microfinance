package pi.db.piversionbd.services.score;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pi.db.piversionbd.entities.groups.Member;
import pi.db.piversionbd.entities.score.*;
import pi.db.piversionbd.exceptions.NotFoundException;
import pi.db.piversionbd.repositories.score.AdherenceTrackingRepository;
import pi.db.piversionbd.repositories.score.ClaimRepository;
import pi.db.piversionbd.repositories.score.ClaimScoringRepository;
import pi.db.piversionbd.repositories.score.MemberRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class ScoringBusinessService {

    private final ClaimRepository claimRepository;
    private final ClaimScoringRepository claimScoringRepository;
    private final MemberRepository memberRepository;
    private final AdherenceTrackingRepository adherenceTrackingRepository;

    // ===== Constantes métier =====
    private static final BigDecimal AUTO_APPROVAL_THRESHOLD = BigDecimal.valueOf(80);
    private static final BigDecimal MANUAL_REVIEW_THRESHOLD = BigDecimal.valueOf(50);
    private static final float ADHERENCE_BONUS = 5f;

    /**
     * 1) CALCUL DU SCORE
     */
    @Transactional
    public ClaimScoring calculateScore(Long claimId) {
        Claim claim = findClaimOrThrow(claimId);

        // Si la claim est déjà dans un état final, on évite de recalculer (règle métier simple)
        if (isFinalDecisionStatus(claim.getStatus())) {
            throw new IllegalStateException("La claim a déjà une décision finale, recalcul non autorisé.");
        }

        // Récupérer existant ou créer nouveau
        ClaimScoring scoring = claimScoringRepository.findByClaimId(claimId)
                .orElseGet(ClaimScoring::new);

        scoring.setClaim(claim);

        // --- logique scoring (exemple statique) ---
        BigDecimal reliability = BigDecimal.valueOf(20);
        BigDecimal medical = BigDecimal.valueOf(30);
        BigDecimal document = BigDecimal.valueOf(25);
        BigDecimal compliance = BigDecimal.valueOf(15);

        BigDecimal total = reliability.add(medical)
                .add(document)
                .add(compliance);

        scoring.setReliabilityScore(reliability);
        scoring.setMedicalScore(medical);
        scoring.setDocumentScore(document);
        scoring.setComplianceScore(compliance);
        scoring.setTotalScore(total);
        scoring.setScoredAt(LocalDateTime.now());
        // -----------------------------------------

        // Snapshot côté claim
        claim.setFinalScoreSnapshot(total);
        claim.setStatus(ClaimStatus.SCORED);

        // (optionnel) on nettoie d'anciennes infos de décision si on rescrore avant décision finale
        claim.setDecisionReason(null);
        claim.setDecisionAt(null);

        claimRepository.save(claim);

        return claimScoringRepository.save(scoring);
    }

    /**
     * 2) DECISION AUTO
     */
    public Claim autoDecision(Long claimId) {
        Claim claim = findClaimOrThrow(claimId);

        // Si déjà en décision finale => on retourne tel quel (idempotence simple)
        if (isFinalDecisionStatus(claim.getStatus())) {
            return claim;
        }

        BigDecimal score = claim.getFinalScoreSnapshot();
        if (score == null) {
            throw new IllegalArgumentException("Claim pas scorée");
        }

        if (score.compareTo(AUTO_APPROVAL_THRESHOLD) >= 0) {
            claim.setStatus(ClaimStatus.APPROVED_AUTO);
            claim.setDecisionReason(ClaimDecisionReason.AUTO_APPROVAL);

        } else if (score.compareTo(MANUAL_REVIEW_THRESHOLD) >= 0) {
            claim.setStatus(ClaimStatus.MANUAL_REVIEW);
            // pas de raison de rejet/approval auto dans ce cas
            claim.setDecisionReason(null);

        } else {
            claim.setStatus(ClaimStatus.REJECTED_LOW_SCORE);
            claim.setDecisionReason(ClaimDecisionReason.LOW_SCORE);
        }

        claim.setDecisionAt(LocalDateTime.now());

        return claimRepository.save(claim);
    }

    /**
     * 3) BONUS ADHERENCE
     */
    public AdherenceTracking rewardMember(Long claimId) {
        Claim claim = findClaimOrThrow(claimId);

        // Validation métier: bonus seulement si APPROVED_AUTO
        if (claim.getStatus() != ClaimStatus.APPROVED_AUTO) {
            throw new IllegalStateException(
                    "Bonus non autorisé: la claim doit être APPROVED_AUTO.");
        }

        Member member = claim.getMember();
        if (member == null) {
            throw new IllegalStateException("Aucun membre lié à cette claim.");
        }

        Float oldScore = member.getAdherenceScore();
        if (oldScore == null) {
            oldScore = 0f;
        }

        Float newScore = oldScore + ADHERENCE_BONUS;

        member.setAdherenceScore(newScore);
        memberRepository.save(member);

        AdherenceTracking tracking = new AdherenceTracking();
        tracking.setMember(member);
        tracking.setRelatedClaim(claim);
        tracking.setEventType(AdherenceEventType.CLAIM_APPROVED_BONUS);
        tracking.setScoreChange(BigDecimal.valueOf(ADHERENCE_BONUS));
        tracking.setCurrentScore(BigDecimal.valueOf(newScore));
        tracking.setNote("Bonus claim approuvé");

        return adherenceTrackingRepository.save(tracking);
    }

    /**
     * 4) PROCESS COMPLET CLAIM
     * - calcul score
     * - décision auto
     * - bonus si APPROVED_AUTO
     */
    public Claim processClaim(Long claimId) {
        Claim existingClaim = findClaimOrThrow(claimId);

        // Idempotence simple: si déjà décidé (final), on ne retrait e pas
        if (isFinalDecisionStatus(existingClaim.getStatus())) {
            return existingClaim;
        }

        // Si pas encore scorée, on calcule
        if (existingClaim.getStatus() != ClaimStatus.SCORED
                || existingClaim.getFinalScoreSnapshot() == null) {
            calculateScore(claimId);
        }

        Claim claim = autoDecision(claimId);

        if (claim.getStatus() == ClaimStatus.APPROVED_AUTO) {
            // NB: sans check "déjà récompensé" en repository, un appel direct /reward peut doubler le bonus.
            // Ici, le process reste sûr grâce à l'idempotence sur statut final (retour direct plus haut).
            rewardMember(claimId);
        }

        return claim;
    }

    // ===== Helpers métier =====

    private Claim findClaimOrThrow(Long claimId) {
        return claimRepository.findById(claimId)
                .orElseThrow(() -> new NotFoundException("Claim introuvable: " + claimId));
    }

    private boolean isFinalDecisionStatus(ClaimStatus status) {
        return status == ClaimStatus.APPROVED_AUTO
                || status == ClaimStatus.MANUAL_REVIEW
                || status == ClaimStatus.REJECTED_LOW_SCORE;
    }
}