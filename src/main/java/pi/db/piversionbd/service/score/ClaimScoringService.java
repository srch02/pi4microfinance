package pi.db.piversionbd.service.score;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pi.db.piversionbd.entities.groups.Member;
import pi.db.piversionbd.entities.score.*;
import pi.db.piversionbd.exception.ResourceNotFoundException;
import pi.db.piversionbd.repository.groups.MemberRepository;
import pi.db.piversionbd.repository.score.ClaimRepository;
import pi.db.piversionbd.service.groups.MembershipClaimConsumptionService;
import pi.db.piversionbd.service.hedera.HederaClaimService;
import pi.db.piversionbd.service.hedera.SolidariHealthContractService;
import pi.db.piversionbd.repository.score.ClaimScoringRepository;
import pi.db.piversionbd.service.notifications.TelegramClaimMessages;
import pi.db.piversionbd.service.notifications.TelegramNotificationService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class ClaimScoringService {

    private static final Logger log = LoggerFactory.getLogger(ClaimScoringService.class);

    private static final BigDecimal AUTO_APPROVE_MIN_SCORE = BigDecimal.valueOf(90);
    private static final BigDecimal LOW_SCORE_THRESHOLD = BigDecimal.valueOf(50);

    private final ClaimScoringRepository claimScoringRepository;
    private final ClaimRepository claimRepository;
    private final MemberRepository memberRepository;
    private final TelegramNotificationService telegramNotificationService;
    private final HederaClaimService hederaClaimService;
    private final SolidariHealthContractService solidariHealthContractService;
    private final MembershipClaimConsumptionService membershipClaimConsumptionService;

    @Transactional(readOnly = true)
    public ClaimScoring getById(Long id) {
        return claimScoringRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ClaimScoring introuvable: " + id));
    }

    @Transactional(readOnly = true)
    public ClaimScoring getByClaimId(Long claimId) {
        return claimScoringRepository.findByClaimId(claimId)
                .orElseThrow(() -> new ResourceNotFoundException("ClaimScoring introuvable pour claimId: " + claimId));
    }

    /**
     * After claim creation: applies a default high score so {@link #applyAutomaticDecision} runs (auto-approve if ≥90, Telegram, wallet).
     * Used when {@code claims.auto-score-on-submit=true}; replace later with real scoring input.
     */
    public void applyDefaultScoringOnSubmit(Long claimId) {
        ClaimScoring body = new ClaimScoring();
        BigDecimal s = BigDecimal.valueOf(95);
        body.setReliabilityScore(s);
        body.setDocumentScore(s);
        body.setMedicalScore(s);
        body.setComplianceScore(s);
        body.setTotalScore(s);
        body.setExcludedConditionDetected(false);
        body.setFraudIndicators(null);
        upsertByClaimId(claimId, body);
    }

    /**
     * Upsert: crée ou met à jour le scoring d'un claim.
     */
    public ClaimScoring upsertByClaimId(Long claimId, ClaimScoring request) {
        Claim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new ResourceNotFoundException("Claim introuvable: " + claimId));

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
        if (isFinalClaimStatus(claim.getStatus())) {
            claimRepository.save(claim);
            return saved;
        }
        applyAutomaticDecision(claim, saved);
        if (claim.getStatus() == ClaimStatus.SUBMITTED) {
            claim.setStatus(ClaimStatus.SCORED);
        }
        claimRepository.save(claim);

        return saved;
    }

    public void deleteByClaimId(Long claimId) {
        if (!claimScoringRepository.existsByClaimId(claimId)) {
            throw new ResourceNotFoundException("ClaimScoring introuvable pour claimId: " + claimId);
        }
        claimScoringRepository.deleteByClaimId(claimId);
    }

    private BigDecimal nullSafe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private static boolean isFinalClaimStatus(ClaimStatus status) {
        return status == ClaimStatus.APPROVED_AUTO
                || status == ClaimStatus.APPROVED_MANUAL
                || status == ClaimStatus.REJECTED_EXCLUSION
                || status == ClaimStatus.REJECTED_FRAUD
                || status == ClaimStatus.REJECTED_LOW_SCORE
                || status == ClaimStatus.PAID
                || status == ClaimStatus.CANCELLED;
    }

    private void applyAutomaticDecision(Claim claim, ClaimScoring scoring) {
        if (scoring.isExcludedConditionDetected()) {
            setDecision(
                    claim,
                    ClaimStatus.REJECTED_EXCLUSION,
                    ClaimDecisionReason.EXCLUDED_CONDITION,
                    "Auto-rejected: excluded medical condition detected."
            );
            claim.setExcludedConditionDetected(true);
            return;
        }

        String fraudIndicators = scoring.getFraudIndicators() == null ? "" : scoring.getFraudIndicators().toLowerCase();
        boolean fraudFlag = fraudIndicators.contains("fraud")
                || fraudIndicators.contains("suspicious")
                || fraudIndicators.contains("forg")
                || fraudIndicators.contains("fake");
        if (fraudFlag) {
            setDecision(
                    claim,
                    ClaimStatus.REJECTED_FRAUD,
                    ClaimDecisionReason.FRAUD_SUSPECTED,
                    "Auto-rejected: fraud indicators detected by scoring."
            );
            return;
        }

        BigDecimal total = nullSafe(scoring.getTotalScore());
        if (total.compareTo(LOW_SCORE_THRESHOLD) < 0) {
            // Your rule: under 50 => admin approval required (not auto-reject).
            claim.setStatus(ClaimStatus.MANUAL_REVIEW);
            claim.setDecisionReason(ClaimDecisionReason.LOW_SCORE);
            claim.setDecisionComment("Admin approval required (score below " + LOW_SCORE_THRESHOLD + "). Handled within 24 hours.");
            claim.setDecisionAt(LocalDateTime.now());
            notifyMember(claim, TelegramClaimMessages.buildAdminApprovalLowScore(claim));
            return;
        }
        if (total.compareTo(AUTO_APPROVE_MIN_SCORE) >= 0) {
            ClaimStatus previous = claim.getStatus();
            setDecision(
                    claim,
                    ClaimStatus.APPROVED_AUTO,
                    ClaimDecisionReason.AUTO_APPROVAL,
                    "Auto-approved by scoring engine."
            );
            // If no explicit approved amount was set, approve the requested amount for now.
            if (claim.getAmountApproved() == null && claim.getAmountRequested() != null) {
                claim.setAmountApproved(claim.getAmountRequested());
            }
            membershipClaimConsumptionService.consumeOnApproval(claim, previous);
            processAutoApprovalToWallet(claim);
            notifyMember(claim, TelegramClaimMessages.buildAutoApproved(claim));
            return;
        }

        claim.setStatus(ClaimStatus.MANUAL_REVIEW);
        claim.setDecisionReason(null);
        claim.setDecisionComment("Your claim will be handled within 24 hours.");
        claim.setDecisionAt(LocalDateTime.now());
        notifyMember(claim, TelegramClaimMessages.buildManual24h(claim));
    }

    private static void setDecision(Claim claim, ClaimStatus status, ClaimDecisionReason reason, String comment) {
        claim.setStatus(status);
        claim.setDecisionReason(reason);
        claim.setDecisionComment(comment);
        claim.setDecisionAt(LocalDateTime.now());
    }

    private void notifyMember(Claim claim, String message) {
        try {
            if (claim == null || claim.getMember() == null || claim.getMember().getId() == null) {
                return;
            }
            // Reload member from DB so telegram_chat_id is current (lazy proxy may be stale or unloaded).
            String chatId = memberRepository.findById(claim.getMember().getId())
                    .map(Member::getTelegramChatId)
                    .orElse(null);
            if (chatId == null || chatId.isBlank()) {
                log.info(
                        "Telegram claim notification skipped: member id={} has no telegram_chat_id (claim id={}). Link via PATCH /api/members/me/telegram",
                        claim.getMember().getId(), claim.getId());
                return;
            }
            telegramNotificationService.sendMessage(chatId, message);
        } catch (Exception e) {
            log.warn("Telegram notify failed for claim {}: {}", claim != null ? claim.getId() : null, e.getMessage());
        }
    }

    private void processAutoApprovalToWallet(Claim claim) {
        try {
            if (claim == null || claim.getAmountApproved() == null) return;
            if (claim.getAmountApproved().compareTo(BigDecimal.ZERO) <= 0) return;

            BigDecimal reimbursementCoins = claim.getAmountApproved()
                    .divide(BigDecimal.valueOf(3), 4, RoundingMode.HALF_UP);
            // Topic audit (CLAIMS.blockchain_hash); may be null if topic submit fails.
            hederaClaimService.recordReimbursement(claim, reimbursementCoins);

            Long memberId = claim.getMember() != null ? claim.getMember().getId() : null;
            if (memberId != null) {
                long amountCents = claim.getAmountApproved().multiply(BigDecimal.valueOf(100)).longValue();
                long fraudScore = claim.getFinalScoreSnapshot() != null
                        ? claim.getFinalScoreSnapshot().longValue() : 90L;
                String contractTx = solidariHealthContractService.processClaim(
                        memberId, amountCents, claim.getClaimNumber(), fraudScore, "APPROVED_AUTO");
                // If topic audit did not persist a hash, store the contract call transaction id instead.
                if (claim.getBlockchainHash() == null && contractTx != null && !contractTx.isBlank()) {
                    claim.setBlockchainHash(contractTx);
                }
            }
            if (claim.getBlockchainHash() == null) {
                log.warn(
                        "Claim id={}: no Hedera tx id stored (topic audit and/or contract call failed). Check logs for HEDERA / CONTRACT; verify hedera.topic-transactions, hedera.contract-id, operator key, and network.",
                        claim.getId());
            }
        } catch (Exception e) {
            log.warn("Blockchain processing failed for claim {}: {}", claim != null ? claim.getId() : null, e.getMessage(), e);
        }
    }
}
