package pi.db.piversionbd.controllers.score;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import pi.db.piversionbd.entities.score.MemberReward;
import pi.db.piversionbd.entities.score.RewardRedemptionStatus;
import pi.db.piversionbd.services.score.MemberRewardService;

import java.net.URI;

@RestController
@RequestMapping("/api/member-rewards")
@RequiredArgsConstructor
public class MemberRewardController {

    private final MemberRewardService memberRewardService;

    @PostMapping
    public ResponseEntity<MemberReward> create(@RequestBody MemberReward memberReward) {
        MemberReward created = memberRewardService.create(memberReward);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.getId())
                .toUri();

        return ResponseEntity.created(location).body(created);
    }

    @GetMapping("/{id}")
    public ResponseEntity<MemberReward> getById(@PathVariable Long id) {
        return ResponseEntity.ok(memberRewardService.getById(id));
    }

    /**
     * GET /api/member-rewards?memberId=1
     * GET /api/member-rewards?status=PENDING
     * GET /api/member-rewards
     */
    @GetMapping
    public ResponseEntity<Page<MemberReward>> getAllOrFiltered(
            @RequestParam(required = false) Long memberId,
            @RequestParam(required = false) RewardRedemptionStatus status,
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable
    ) {
        if (memberId != null) {
            return ResponseEntity.ok(memberRewardService.getByMember(memberId, pageable));
        }
        if (status != null) {
            return ResponseEntity.ok(memberRewardService.getByStatus(status, pageable));
        }
        return ResponseEntity.ok(memberRewardService.getAll(pageable));
    }

    @PutMapping("/{id}")
    public ResponseEntity<MemberReward> update(
            @PathVariable Long id,
            @RequestBody MemberReward request
    ) {
        return ResponseEntity.ok(memberRewardService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        memberRewardService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
