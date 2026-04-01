package pi.db.piversionbd.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pi.db.piversionbd.config.HederaProperties;
import pi.db.piversionbd.service.hedera.HederaAuditService;

/**
 * Simple endpoint to check if Hedera integration is configured and the client can connect.
 * GET /api/hedera/status — no auth required (read-only config/connectivity check).
 */
@RestController
@RequestMapping("/api/hedera")
@RequiredArgsConstructor
@Tag(name = "Hedera", description = "Blockchain status and health")
public class HederaStatusController {

    private final HederaProperties properties;
    private final HederaAuditService auditService;

    @GetMapping("/status")
    @Operation(summary = "Hedera status", description = "Check if Hedera is enabled, configured, and client can connect")
    public HederaStatus status() {
        HederaStatus s = new HederaStatus();
        s.setEnabled(properties.isEnabled());
        s.setNetwork(properties.getNetwork());
        s.setTopicTransactions(properties.getTopicTransactions());
        s.setTopicConfigured(isTopicConfigured(properties.getTopicTransactions()));
        s.setOperatorConfigured(
                properties.getOperatorAccountId() != null && !properties.getOperatorAccountId().isBlank()
                        && properties.getOperatorPrivateKey() != null && !properties.getOperatorPrivateKey().isBlank());
        s.setClientOk(false);
        s.setMessage("OK");
        if (!s.isEnabled()) {
            s.setMessage("Hedera is disabled (hedera.enabled=false)");
            return s;
        }
        if (!s.isTopicConfigured()) {
            s.setMessage("Topic not configured (set hedera.topic-transactions to a valid topic ID)");
            return s;
        }
        if (!s.isOperatorConfigured()) {
            s.setMessage("Operator not configured (set hedera.operator-account-id and hedera.operator-private-key)");
            return s;
        }
        try {
            auditService.getClient();
            s.setClientOk(true);
            s.setMessage("Hedera client ready; topic submission should work.");
        } catch (Exception e) {
            s.setMessage("Client failed: " + e.getClass().getSimpleName() + " - " + e.getMessage());
        }
        return s;
    }

    private static boolean isTopicConfigured(String topic) {
        return topic != null && !topic.isBlank() && !"0.0.0".equals(topic);
    }

    @Data
    public static class HederaStatus {
        private boolean enabled;
        private String network;
        private String topicTransactions;
        private boolean topicConfigured;
        private boolean operatorConfigured;
        private boolean clientOk;
        private String message;
    }
}
