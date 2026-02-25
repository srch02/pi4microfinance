package pi.db.piversionbd.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class AdherenceTrackingResponse {
    private Long trackingId;
    private Long memberId;
    private Long claimId;
    private String eventType;
    private BigDecimal scoreChange;
    private BigDecimal currentScore;
    private String note;
}