package pi.db.piversionbd.dto.score;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ClaimRewardSummaryResponse {

    private Long memberId;

    private long submittedClaimsCount;

    private int currentDiscountPercent;

    private Integer nextMilestoneClaims;

    private Integer nextMilestoneDiscountPercent;

    private Integer claimsRemainingToNextMilestone;

    private List<RewardBadgeResponse> badges;
}