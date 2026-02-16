package pi.db.piversionbd.services.score;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pi.db.piversionbd.entities.score.*;
import pi.db.piversionbd.exceptions.NotFoundException;
import pi.db.piversionbd.repositories.score.ClaimRepository;
import pi.db.piversionbd.repositories.score.ClaimScoringRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class ClaimScoringService {

    private final ClaimScoringRepository claimScoringRepository;
    private final ClaimRepository claimRepository;

    @Transactional(readOnly = true)
    public ClaimScoring getById(Long id) {
        return claimScoringRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("ClaimScoring introuvable: " + id));
    }

    @Transactional(readOnly = true)
    public ClaimScoring getByClaimId(Long claimId) {
        return claimScoringRepository.findByClaimId(claimId)
                .orElseThrow(() -> new NotFoundException("ClaimScoring introuvable pour claimId: " + claimId));
    }

    /**
     * Upsert: crée ou met à jour le scoring d'un claim.
     */
    public ClaimScoring upsertByClaimId(Long claimId, ClaimScoring request) {
        Claim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new NotFoundException("Claim introuvable: " + claimId));

        ClaimScoring scoring = claimScoringRepository.findByClaimId(claimId)
                .orElseGet(ClaimScoring::new);

        scoring.setClaim(claim);
        scoring.setReliabilityScore(nullSafe(request.getReliabilityScore()));
        scoring.setDocumentScore(nullSafe(request.getDocumentScore()));
        scoring.setMedicalScore(nullSafe(request.getMedicalScore()));
        scoring.setComplianceScore(nullSafe(request.getComplianceScore()));
        scoring.setTotalScore(nullSafe(request.getTotalScore()));
        scoring.setExcludedConditionDetected(request.isExcludedConditionDetected());
        scoring.setFraudIndicators(request.getFraudIndicators());
        scoring.setScoredAt(request.getScoredAt() == null ? LocalDateTime.now() : request.getScoredAt());

        ClaimScoring saved = claimScoringRepository.save(scoring);

        // Snapshot rapide côté Claim
        claim.setFinalScoreSnapshot(saved.getTotalScore());
        if (claim.getStatus() == ClaimStatus.SUBMITTED || claim.getStatus() == ClaimStatus.SCORED) {
            claim.setStatus(ClaimStatus.SCORED);
        }
        claimRepository.save(claim);

        return saved;
    }

    public void deleteByClaimId(Long claimId) {
        if (!claimScoringRepository.existsByClaimId(claimId)) {
            throw new NotFoundException("ClaimScoring introuvable pour claimId: " + claimId);
        }
        claimScoringRepository.deleteByClaimId(claimId);
    }

    private BigDecimal nullSafe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
