package pi.db.piversionbd.dto;

import java.math.BigDecimal;

public class ClaimCreateRequest {
    public Long memberId;
    public Long groupId;
    public String claimNumber;
    public BigDecimal amountRequested;
}
