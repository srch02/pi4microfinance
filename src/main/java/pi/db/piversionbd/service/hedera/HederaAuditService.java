package pi.db.piversionbd.service.hedera;

import com.hedera.hashgraph.sdk.AccountId;
import com.hedera.hashgraph.sdk.Client;
import com.hedera.hashgraph.sdk.PrivateKey;
import com.hedera.hashgraph.sdk.TopicId;
import com.hedera.hashgraph.sdk.TopicMessageSubmitTransaction;
import com.hedera.hashgraph.sdk.TransactionResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import pi.db.piversionbd.config.HederaProperties;

import jakarta.annotation.PreDestroy;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeoutException;

/**
 * Low-level Hedera audit service: submits messages to consensus topics.
 * Returns Hedera transaction ID (blockchain hash) or null when disabled/error.
 */
@Service
@RequiredArgsConstructor
public class HederaAuditService {

    private static final Logger log = LoggerFactory.getLogger(HederaAuditService.class);

    private final HederaProperties properties;
    private Client client;
    private volatile String lastFailureMessage;

    /**
     * Last error from submitToTopic when it returned null (for callers to log).
     */
    public String getLastFailureMessage() {
        return lastFailureMessage;
    }

    /**
     * Submits a message to the given topic and returns the Hedera transaction ID.
     * Returns null if Hedera is disabled, misconfigured, or submission fails.
     */
    public String submitToTopic(String topicIdStr, String message) {
        if (!properties.isEnabled()) {
            return null;
        }
        if (topicIdStr == null || topicIdStr.isBlank() || "0.0.0".equals(topicIdStr)) {
            lastFailureMessage = "Topic not configured (topic=" + topicIdStr + ")";
            log.warn("=== HEDERA: topic not configured (topic={}), skipping submission", topicIdStr);
            return null;
        }
        try {
            log.info("=== HEDERA: submitting to topic {} (message length {} bytes)", topicIdStr, message != null ? message.length() : 0);
            Client c = getOrCreateClient();
            TopicId topicId = TopicId.fromString(topicIdStr);
            TopicMessageSubmitTransaction tx = new TopicMessageSubmitTransaction()
                    .setTopicId(topicId)
                    .setMessage(message.getBytes(StandardCharsets.UTF_8));
            // freezeWith + execute is the supported pattern for topic submits (ensures signing with operator).
            TransactionResponse response = tx.freezeWith(c).execute(c);
            String hash = null;
            if (response != null && response.transactionId != null) {
                hash = response.transactionId.toString();
            }
            if (hash != null) {
                lastFailureMessage = null;
                log.info("Hedera topic message submitted, transactionId={}", hash);
            } else {
                lastFailureMessage = "execute() returned null transaction ID";
                log.warn("=== HEDERA: execute() returned null transaction ID for topic {}", topicIdStr);
            }
            return hash;
        } catch (Exception e) {
            lastFailureMessage = e.getClass().getSimpleName() + ": " + e.getMessage();
            log.error("=== HEDERA FAILED: topic={}, error={} (check operator key, topic exists on testnet, and network access)", topicIdStr, e.getMessage(), e);
            return null;
        }
    }

    public boolean isEnabled() {
        return properties.isEnabled();
    }

    /** Returns the Hedera client for contract execution. Use only when isEnabled(). */
    public Client getClient() throws Exception {
        return getOrCreateClient();
    }

    private synchronized Client getOrCreateClient() throws Exception {
        if (client != null) {
            return client;
        }
        Client c;
        if ("testnet".equalsIgnoreCase(properties.getNetwork())) {
            c = Client.forTestnet();
        } else if ("mainnet".equalsIgnoreCase(properties.getNetwork())) {
            c = Client.forMainnet();
        } else {
            c = Client.forTestnet();
        }
        if (properties.getOperatorAccountId() != null && properties.getOperatorPrivateKey() != null
                && !properties.getOperatorPrivateKey().isBlank()) {
            String keyStr = properties.getOperatorPrivateKey().trim();
            // ECDSA keys often use "0x" prefix; Hedera SDK hex decoder expects raw hex only
            String hexOnly = keyStr.startsWith("0x") ? keyStr.substring(2) : keyStr;
            PrivateKey pk = keyStr.startsWith("0x")
                    ? PrivateKey.fromStringECDSA(hexOnly)
                    : PrivateKey.fromString(keyStr);
            c.setOperator(AccountId.fromString(properties.getOperatorAccountId()), pk);
        }
        client = c;
        return client;
    }

    @PreDestroy
    public void close() {
        if (client != null) {
            try {
                client.close();
            } catch (TimeoutException e) {
                log.warn("Hedera client close timeout");
            }
            client = null;
        }
    }
}
