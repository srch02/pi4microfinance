package pi.db.piversionbd.service.hedera;

import com.hedera.hashgraph.sdk.ContractExecuteTransaction;
import com.hedera.hashgraph.sdk.ContractFunctionParameters;
import com.hedera.hashgraph.sdk.ContractId;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import pi.db.piversionbd.config.HederaProperties;

import java.math.BigInteger;

/**
 * Calls the SolidariHealth Solidity smart contract on Hedera.
 * Maps backend events to contract functions: createMemberPolicy, recordMonthlyPayment, processClaim.
 */
@Service
@RequiredArgsConstructor
public class SolidariHealthContractService {

    private static final Logger log = LoggerFactory.getLogger(SolidariHealthContractService.class);
    private static final long GAS_LIMIT = 500_000L;

    private final HederaAuditService auditService;
    private final HederaProperties properties;

    /**
     * Derives a deterministic EVM address from member ID for use as policy key.
     */
    public static String memberIdToEvmAddress(Long memberId) {
        if (memberId == null) return "0x0000000000000000000000000000000000000000";
        return "0x" + String.format("%040x", memberId);
    }

    /**
     * Calls createMemberPolicy on the contract. Returns tx hash or null.
     */
    public String createMemberPolicy(Long memberId, String cinNumber, String packageType,
                                     long monthlyAmountDt, long annualLimitDt) {
        if (!auditService.isEnabled() || !isContractConfigured()) return null;
        try {
            String memberAddr = memberIdToEvmAddress(memberId);
            var params = new ContractFunctionParameters()
                    .addAddress(memberAddr)
                    .addString(cinNumber != null ? cinNumber : "")
                    .addString(packageType != null ? packageType : "BASIC")
                    .addUint256(BigInteger.valueOf(monthlyAmountDt))
                    .addUint256(BigInteger.valueOf(annualLimitDt));
            return execute("createMemberPolicy", params);
        } catch (Exception e) {
            log.warn("SolidariHealth createMemberPolicy failed: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Calls recordMonthlyPayment on the contract.
     * @param dtAmountCents amount in cents (e.g. 2550 = 25.50 DT). Contract does coinsToAdd = dtAmount/3.
     */
    public String recordMonthlyPayment(Long memberId, long dtAmountCents) {
        if (!auditService.isEnabled() || !isContractConfigured() || memberId == null) return null;
        try {
            String memberAddr = memberIdToEvmAddress(memberId);
            var params = new ContractFunctionParameters()
                    .addAddress(memberAddr)
                    .addUint256(BigInteger.valueOf(dtAmountCents));
            return execute("recordMonthlyPayment", params);
        } catch (Exception e) {
            log.warn("SolidariHealth recordMonthlyPayment failed: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Calls processClaim on the contract. Returns tx hash or null.
     */
    public String processClaim(Long memberId, long amountApproved, String claimNumber,
                               long fraudScoreScaled, String decision) {
        if (!auditService.isEnabled() || !isContractConfigured() || memberId == null) return null;
        try {
            String memberAddr = memberIdToEvmAddress(memberId);
            var params = new ContractFunctionParameters()
                    .addAddress(memberAddr)
                    .addUint256(BigInteger.valueOf(amountApproved))
                    .addString(claimNumber != null ? claimNumber : "")
                    .addUint256(BigInteger.valueOf(fraudScoreScaled))
                    .addString(decision != null ? decision : "APPROVED");
            return execute("processClaim", params);
        } catch (Exception e) {
            log.error("=== CONTRACT FAILED: error={}", e.getMessage(), e);
            return null;
        }
    }

    private boolean isContractConfigured() {
        String id = properties.getContractId();
        return id != null && !id.isBlank() && !"0.0.0".equals(id);
    }

    private String execute(String functionName, ContractFunctionParameters params) throws Exception {
        ContractId contractId = ContractId.fromString(properties.getContractId());
        var tx = new ContractExecuteTransaction()
                .setContractId(contractId)
                .setGas(GAS_LIMIT)
                .setFunction(functionName, params);
        var client = auditService.getClient();
        var response = tx.execute(client);
        String hash = response.transactionId.toString();
        log.info("SolidariHealth {} executed: {}", functionName, hash);
        return hash;
    }
}
