package pi.db.piversionbd.controllers.score;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pi.db.piversionbd.dto.ClaimResponse;
import pi.db.piversionbd.entities.score.AdherenceTracking;
import pi.db.piversionbd.entities.score.Claim;
import pi.db.piversionbd.entities.score.ClaimScoring;
import pi.db.piversionbd.services.score.ClaimService;
import pi.db.piversionbd.services.score.ScoringBusinessService;

@RestController
@RequestMapping("/api/scoring")
@RequiredArgsConstructor
public class ScoringBusinessController {

    private final ScoringBusinessService scoringBusinessService;
    private final ClaimService claimService;

    @PostMapping("/calculate/{claimId}")
    public ResponseEntity<ClaimScoring> calculate(@PathVariable Long claimId) {
        return ResponseEntity.ok(
                scoringBusinessService.calculateScore(claimId)
        );
    }

    @PostMapping("/decision/{claimId}")
    public ResponseEntity<pi.db.piversionbd.dto.ClaimResponse> decision(
            @PathVariable Long claimId) {

        Claim claim = scoringBusinessService.autoDecision(claimId);
        return ResponseEntity.ok(claimService.toResponse(claim));
    }

    @PostMapping("/reward/{claimId}")
    public ResponseEntity<pi.db.piversionbd.dto.AdherenceTrackingResponse> reward(
            @PathVariable Long claimId) {

        AdherenceTracking tracking = scoringBusinessService.rewardMember(claimId);

        pi.db.piversionbd.dto.AdherenceTrackingResponse response =
                pi.db.piversionbd.dto.AdherenceTrackingResponse.builder()
                        .trackingId(tracking.getId())
                        .memberId(tracking.getMember() != null ? tracking.getMember().getId() : null)
                        .claimId(tracking.getRelatedClaim() != null ? tracking.getRelatedClaim().getId() : null)
                        .eventType(tracking.getEventType() != null ? tracking.getEventType().name() : null)
                        .scoreChange(tracking.getScoreChange())
                        .currentScore(tracking.getCurrentScore())
                        .note(tracking.getNote())
                        .build();

        return ResponseEntity.ok(response);
    }

    @PostMapping("/process/{claimId}")
    public ResponseEntity<ClaimResponse> process(@PathVariable Long claimId) {
        Claim claim = scoringBusinessService.processClaim(claimId);
        return ResponseEntity.ok(claimService.toResponse(claim));
    }
}