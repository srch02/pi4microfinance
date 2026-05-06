package pi.db.piversionbd.controller.score;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import pi.db.piversionbd.dto.score.ClaimRewardSummaryResponse;
import pi.db.piversionbd.security.CurrentMemberResolver;
import pi.db.piversionbd.service.score.ClaimRewardService;

@RestController
@RequestMapping("/api/my-rewards")
@RequiredArgsConstructor
public class ClaimRewardController {

    private final ClaimRewardService claimRewardService;
    private final CurrentMemberResolver currentMemberResolver;

    @GetMapping
    public ResponseEntity<ClaimRewardSummaryResponse> getMyRewards(Authentication auth) {
        Long memberId = currentMemberResolver.requireMemberId(auth);

        return ResponseEntity.ok(
                claimRewardService.getSummary(memberId)
        );
    }
}