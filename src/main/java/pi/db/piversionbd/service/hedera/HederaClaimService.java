package pi.db.piversionbd.service.hedera;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pi.db.piversionbd.config.HederaProperties;
import pi.db.piversionbd.entities.score.Claim;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Records claim reimbursements on Hedera blockchain for audit trail.
 */
@Service
@RequiredArgsConstructor
public class HederaClaimService {

    private static final Logger log = LoggerFactory.getLogger(HederaClaimService.class);

    private final HederaAuditService auditService;
    private final HederaProperties properties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Records a reimbursement on Hedera. Returns transaction hash or null.
     * Sets reimbursementCoins and blockchainHash on claim if successful.
     */
    public String recordReimbursement(Claim claim, BigDecimal reimbursementCoins) {
        if (!auditService.isEnabled() || claim == null) {
            if (claim != null) {
                log.info("=== HEDERA SKIP: Hedera is disabled (hedera.enabled=false), not recording reimbursement for claim {}", claim.getId());
            }
            return null;
        }
        claim.setReimbursementCoins(reimbursementCoins);
        float coins = reimbursementCoins != null ? reimbursementCoins.floatValue() : 0f;

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", "REIMBURSEMENT");
        payload.put("claimId", claim.getId());
        payload.put("claimNumber", claim.getClaimNumber());
        payload.put("memberId", claim.getMember() != null ? claim.getMember().getId() : null);
        payload.put("groupId", claim.getGroup() != null ? claim.getGroup().getId() : null);
        payload.put("amountApproved", claim.getAmountApproved());
        payload.put("reimbursementCoins", coins);
        try {
            String json = objectMapper.writeValueAsString(payload);
            String topicId = properties.getTopicTransactions();
            String hash = auditService.submitToTopic(topicId, json);
            if (hash != null) {
                claim.setBlockchainHash(hash);
            } else {
                String detail = auditService.getLastFailureMessage();
                log.warn("=== HEDERA: topic submission returned null for claim {} (topic={}). Cause: {}", claim.getId(), topicId, detail != null ? detail : "unknown");
            }
            return hash;
        } catch (Exception e) {
            log.error("=== HEDERA FAILED (recordReimbursement): claim={}, error={}", claim.getId(), e.getMessage(), e);
            return null;
        }
    }
}
