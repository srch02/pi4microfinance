package pi.db.piversionbd.dto.score;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RewardBadgeResponse {

    private String code;
    private String title;
    private String description;
    private boolean earned;
    private Integer discountPercent;
    private Integer requiredClaims;
}