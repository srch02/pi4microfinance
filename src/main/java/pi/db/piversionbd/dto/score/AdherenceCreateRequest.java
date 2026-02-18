package pi.db.piversionbd.dto;

import java.math.BigDecimal;

public class AdherenceCreateRequest {
    public Long memberId;
    public Long claimId;
    public String eventType;
    public BigDecimal scoreChange;
    public BigDecimal currentScore;
    public String note;
}
