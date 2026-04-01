package pi.db.piversionbd.service.hedera;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pi.db.piversionbd.config.HederaProperties;
import pi.db.piversionbd.entities.groups.Payment;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Records payment transactions on Hedera blockchain for audit trail.
 */
@Service
@RequiredArgsConstructor
public class HederaPaymentService {

    private final HederaAuditService auditService;
    private final HederaProperties properties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Records a payment on Hedera. Returns transaction hash or null.
     * Sets dtAmount and coinAmount on payment, and blockchainHash if successful.
     */
    public String recordPayment(Payment payment) {
        if (!auditService.isEnabled() || payment == null) {
            return null;
        }
        float dtAmount = payment.getAmount() != null ? payment.getAmount() : 0f;
        float coinAmount = HederaWalletService.dtToCoins(dtAmount);
        payment.setDtAmount(dtAmount);
        payment.setCoinAmount(coinAmount);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", "PAYMENT");
        payload.put("paymentId", payment.getId());
        payload.put("memberId", payment.getMember() != null ? payment.getMember().getId() : null);
        payload.put("groupId", payment.getGroup() != null ? payment.getGroup().getId() : null);
        payload.put("dtAmount", dtAmount);
        payload.put("coinAmount", coinAmount);
        payload.put("poolAllocation", payment.getPoolAllocation());
        payload.put("platformFee", payment.getPlatformFee());
        payload.put("nationalFund", payment.getNationalFund());
        try {
            String json = objectMapper.writeValueAsString(payload);
            String hash = auditService.submitToTopic(properties.getTopicTransactions(), json);
            if (hash != null) {
                payment.setBlockchainHash(hash);
            }
            return hash;
        } catch (Exception e) {
            return null;
        }
    }
}
