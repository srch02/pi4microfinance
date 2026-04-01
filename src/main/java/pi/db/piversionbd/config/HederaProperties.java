package pi.db.piversionbd.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Hedera config. Use kebab-case in application.properties so values bind correctly:
 * hedera.enabled, hedera.network, hedera.operator-account-id, hedera.operator-private-key,
 * hedera.topic-contracts, hedera.topic-transactions, hedera.contract-id
 */
@Component
@ConfigurationProperties(prefix = "hedera")
public class HederaProperties {

    private boolean enabled = true;
    private String network = "testnet";
    private String operatorAccountId;
    private String operatorPrivateKey;
    private String topicContracts = "0.0.0";
    private String topicTransactions = "0.0.0";
    private String contractId = "0.0.0";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getNetwork() {
        return network;
    }

    public void setNetwork(String network) {
        this.network = network;
    }

    public String getOperatorAccountId() {
        return operatorAccountId;
    }

    public void setOperatorAccountId(String operatorAccountId) {
        this.operatorAccountId = operatorAccountId;
    }

    public String getOperatorPrivateKey() {
        return operatorPrivateKey;
    }

    public void setOperatorPrivateKey(String operatorPrivateKey) {
        this.operatorPrivateKey = operatorPrivateKey;
    }

    public String getTopicContracts() {
        return topicContracts;
    }

    public void setTopicContracts(String topicContracts) {
        this.topicContracts = topicContracts;
    }

    public String getTopicTransactions() {
        return topicTransactions;
    }

    public void setTopicTransactions(String topicTransactions) {
        this.topicTransactions = topicTransactions;
    }

    public String getContractId() {
        return contractId;
    }

    public void setContractId(String contractId) {
        this.contractId = contractId;
    }
}
