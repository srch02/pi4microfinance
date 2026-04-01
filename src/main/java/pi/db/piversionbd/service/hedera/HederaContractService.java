package pi.db.piversionbd.service.hedera;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pi.db.piversionbd.config.HederaProperties;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Deploys insurance smart contract metadata to Hedera (audit trail).
 * Stores contract terms as immutable message on blockchain.
 */
@Service
@RequiredArgsConstructor
public class HederaContractService {

    private final HederaAuditService auditService;
    private final HederaProperties properties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Deploys contract metadata to Hedera. Returns transaction hash or null.
     */
    public String deployContract(Long memberId, String cinNumber, String packageType,
                                 float priceBasic, float priceConfort, float pricePremium,
                                 String exclusionsNote) {
        if (!auditService.isEnabled()) {
            return null;
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", "INSURANCE_CONTRACT");
        payload.put("memberId", memberId);
        payload.put("cinNumber", cinNumber);
        payload.put("packageType", packageType != null ? packageType : "BASIC");
        payload.put("priceBasic", priceBasic);
        payload.put("priceConfort", priceConfort);
        payload.put("pricePremium", pricePremium);
        payload.put("exclusions", exclusionsNote != null ? exclusionsNote : "");
        try {
            String json = objectMapper.writeValueAsString(payload);
            return auditService.submitToTopic(properties.getTopicContracts(), json);
        } catch (Exception e) {
            return null;
        }
    }
}
