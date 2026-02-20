package pi.db.piversionbd.dto.score;

import java.math.BigDecimal;

public class AdherenceCreateRequest {
    public Long memberId;
    public Long claimId;
    public String eventType;
    public BigDecimal scoreChange;
    public BigDecimal currentScore;
    public String note;
}
