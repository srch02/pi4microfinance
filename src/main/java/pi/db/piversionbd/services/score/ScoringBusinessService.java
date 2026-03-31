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
import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class ScoringBusinessService {

    private final ClaimRepository claimRepository;
    private final ClaimScoringRepository claimScoringRepository;
    private final MemberRepository memberRepository;
    private final AdherenceTrackingRepository adherenceTrackingRepository;

    // ===== Seuils de décision =====
    private static final BigDecimal AUTO_APPROVAL_THRESHOLD = BigDecimal.valueOf(80);
    private static final BigDecimal MANUAL_REVIEW_THRESHOLD = BigDecimal.valueOf(50);
    private static final float ADHERENCE_BONUS = 5f;

    // ===== Maximums par sous-score =====
    private static final int MAX_RELIABILITY_SCORE = 20;
    private static final int MAX_MEDICAL_SCORE = 30;
    private static final int MAX_DOCUMENT_SCORE = 25;
    private static final int MAX_COMPLIANCE_SCORE = 25;

    /**
     * 1) CALCUL DU SCORE
     */
    public ClaimScoring calculateScore(Long claimId) {
        Claim claim = findClaimOrThrow(claimId);

        if (isFinalDecisionStatus(claim.getStatus())) {
            throw new IllegalStateException("La claim a déjà une décision finale, recalcul non autorisé.");
        }

        ClaimScoring scoring = claimScoringRepository.findByClaimId(claimId)
                .orElseGet(ClaimScoring::new);

        scoring.setClaim(claim);

        BigDecimal reliability = calculateReliabilityScore(claim);
        BigDecimal medical = calculateMedicalScore(claim);
        BigDecimal document = calculateDocumentScore(claim);
        BigDecimal compliance = calculateComplianceScore(claim);

        BigDecimal total = reliability
                .add(medical)
                .add(document)
                .add(compliance);

        boolean excludedConditionDetected = hasExcludedCondition(claim);
        String fraudIndicators = buildFraudIndicators(claim);

        scoring.setReliabilityScore(reliability);
        scoring.setMedicalScore(medical);
        scoring.setDocumentScore(document);
        scoring.setComplianceScore(compliance);
        scoring.setTotalScore(total);
        scoring.setExcludedConditionDetected(excludedConditionDetected);
        scoring.setFraudIndicators(fraudIndicators);
        scoring.setScoredAt(LocalDateTime.now());

        claim.setFinalScoreSnapshot(total);
        claim.setStatus(ClaimStatus.SCORED);

        // reset décision précédente si on rescrore avant décision finale
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
     */
    public Claim processClaim(Long claimId) {
        Claim existingClaim = findClaimOrThrow(claimId);

        if (isFinalDecisionStatus(existingClaim.getStatus())) {
            return existingClaim;
        }

        if (existingClaim.getStatus() != ClaimStatus.SCORED
                || existingClaim.getFinalScoreSnapshot() == null) {
            calculateScore(claimId);
        }

        Claim claim = autoDecision(claimId);

        if (claim.getStatus() == ClaimStatus.APPROVED_AUTO) {
            rewardMember(claimId);
        }

        return claim;
    }

    // =========================================================
    // =============== CALCUL DES SOUS-SCORES ==================
    // =========================================================

    /**
     * Reliability sur 20
     */
    private BigDecimal calculateReliabilityScore(Claim claim) {
        int score = 0;

        Member member = claim.getMember();

        // membre présent
        if (member != null) {
            score += 5;
        }

        // score d’adhérence correct
        if (member != null && member.getAdherenceScore() != null) {
            if (member.getAdherenceScore() >= 80) {
                score += 10;
            } else if (member.getAdherenceScore() >= 50) {
                score += 5;
            }
        }

        // pas d’indicateur de fraude
        if (!hasFraudIndicators(claim)) {
            score += 5;
        }

        return BigDecimal.valueOf(Math.min(score, MAX_RELIABILITY_SCORE));
    }

    /**
     * Medical sur 30
     */
    private BigDecimal calculateMedicalScore(Claim claim) {
        int score = 0;

        // Exemple: adapte ces getters à ton vrai modèle
        if (hasDiagnosis(claim)) {
            score += 10;
        }

        if (hasTreatmentDate(claim)) {
            score += 5;
        }

        if (hasMedicalJustification(claim)) {
            score += 10;
        }

        if (isMedicalDataConsistent(claim)) {
            score += 5;
        }

        return BigDecimal.valueOf(Math.min(score, MAX_MEDICAL_SCORE));
    }

    /**
     * Document sur 25
     */
    private BigDecimal calculateDocumentScore(Claim claim) {
        int score = 0;

        if (hasAnyDocument(claim)) {
            score += 10;
        }

        if (hasIdentityDocument(claim)) {
            score += 5;
        }

        if (hasInvoiceDocument(claim)) {
            score += 5;
        }

        if (hasMedicalCertificateDocument(claim)) {
            score += 5;
        }

        return BigDecimal.valueOf(Math.min(score, MAX_DOCUMENT_SCORE));
    }

    /**
     * Compliance sur 25
     */
    private BigDecimal calculateComplianceScore(Claim claim) {
        int score = 0;

        if (isWithinCoveragePeriod(claim)) {
            score += 10;
        }

        if (isAmountWithinAllowedLimit(claim)) {
            score += 10;
        }

        if (!hasExcludedCondition(claim)) {
            score += 5;
        }

        return BigDecimal.valueOf(Math.min(score, MAX_COMPLIANCE_SCORE));
    }

    // =========================================================
    // ==================== HELPERS METIER =====================
    // =========================================================

    private Claim findClaimOrThrow(Long claimId) {
        return claimRepository.findById(claimId)
                .orElseThrow(() -> new NotFoundException("Claim introuvable: " + claimId));
    }

    private boolean isFinalDecisionStatus(ClaimStatus status) {
        return status == ClaimStatus.APPROVED_AUTO
                || status == ClaimStatus.MANUAL_REVIEW
                || status == ClaimStatus.REJECTED_LOW_SCORE;
    }

    // =========================================================
    // =========== METHODES A ADAPTER A TON MODELE ============
    // =========================================================

    private boolean hasFraudIndicators(Claim claim) {
        // Exemples de règles:
        // - montant anormalement élevé
        // - doublon possible
        // - données incohérentes
        return isAmountSuspicious(claim) || isPotentialDuplicate(claim) || !isClaimDataConsistent(claim);
    }

    private String buildFraudIndicators(Claim claim) {
        StringBuilder sb = new StringBuilder();

        if (isAmountSuspicious(claim)) {
            sb.append("Montant suspect; ");
        }
        if (isPotentialDuplicate(claim)) {
            sb.append("Doublon potentiel; ");
        }
        if (!isClaimDataConsistent(claim)) {
            sb.append("Données incohérentes; ");
        }

        return sb.toString().trim();
    }

    private boolean hasDiagnosis(Claim claim) {
        // Exemple à adapter:
        // return claim.getDiagnosis() != null && !claim.getDiagnosis().isBlank();
        return true;
    }

    private boolean hasTreatmentDate(Claim claim) {
        // Exemple à adapter:
        // return claim.getTreatmentDate() != null;
        return true;
    }

    private boolean hasMedicalJustification(Claim claim) {
        // Exemple à adapter:
        // return claim.getMedicalReport() != null || claim.getPrescription() != null;
        return true;
    }

    private boolean isMedicalDataConsistent(Claim claim) {
        // Exemple à adapter selon règles métier
        return true;
    }

    private boolean hasAnyDocument(Claim claim) {
        // Exemple à adapter:
        // return claim.getDocuments() != null && !claim.getDocuments().isEmpty();
        return true;
    }

    private boolean hasIdentityDocument(Claim claim) {
        // Exemple à adapter
        return true;
    }

    private boolean hasInvoiceDocument(Claim claim) {
        // Exemple à adapter
        return true;
    }

    private boolean hasMedicalCertificateDocument(Claim claim) {
        // Exemple à adapter
        return true;
    }

    private boolean isWithinCoveragePeriod(Claim claim) {
        // Exemple à adapter:
        // vérifier date de soin / date sinistre par rapport à la couverture
        return true;
    }

    private boolean isAmountWithinAllowedLimit(Claim claim) {
        // Exemple à adapter:
        // return claim.getClaimAmount() != null &&
        //        claim.getClaimAmount().compareTo(BigDecimal.valueOf(5000)) <= 0;
        return true;
    }

    private boolean hasExcludedCondition(Claim claim) {
        // Exemple à adapter:
        // vérifier si la pathologie / acte / situation est exclu(e) du contrat
        return false;
    }

    private boolean isAmountSuspicious(Claim claim) {
        // Exemple à adapter
        return false;
    }

    private boolean isPotentialDuplicate(Claim claim) {
        // Exemple à adapter:
        // recherche d’une claim proche pour le même member, même date, même montant
        return false;
    }

    private boolean isClaimDataConsistent(Claim claim) {
        // Exemple à adapter:
        // cohérence entre montant, dates, docs, bénéficiaire, etc.
        return true;
    }
}