package pi.db.piversionbd.controller.score;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pi.db.piversionbd.dto.score.AdherenceCreateRequest;
import pi.db.piversionbd.dto.score.AdherenceResponse;
import pi.db.piversionbd.entities.score.AdherenceTracking;
import pi.db.piversionbd.service.score.AdherenceTrackingService;


@RestController
@RequestMapping("/api/adherence-tracking")
@RequiredArgsConstructor
public class AdherenceTrackingController {

    private final AdherenceTrackingService adherenceTrackingService;

    @PostMapping
    public ResponseEntity<AdherenceResponse> create(
            @RequestBody AdherenceCreateRequest req
    ) {
        AdherenceTracking created = adherenceTrackingService.createFromIds(req);
        return ResponseEntity.status(201).body(adherenceTrackingService.toResponse(created));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AdherenceResponse> getById(@PathVariable Long id) {
        AdherenceTracking e = adherenceTrackingService.getById(id);
        return ResponseEntity.ok(adherenceTrackingService.toResponse(e));
    }

    /**
     * GET /api/adherence-tracking?memberId=1
     * GET /api/adherence-tracking?claimId=10
     */
    @GetMapping
    public ResponseEntity<org.springframework.data.domain.Page<AdherenceResponse>> getByMemberOrClaim(
            @RequestParam(required = false) Long memberId,
            @RequestParam(required = false) Long claimId,
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable
    ) {
        if (memberId != null) {
            return ResponseEntity.ok(
                    adherenceTrackingService.getByMember(memberId, pageable)
                            .map(adherenceTrackingService::toResponse)
            );
        }
        if (claimId != null) {
            return ResponseEntity.ok(
                    adherenceTrackingService.getByClaim(claimId, pageable)
                            .map(adherenceTrackingService::toResponse)
            );
        }
        throw new IllegalArgumentException("Fournissez memberId ou claimId.");
    }

    @PutMapping("/{id}")
    public ResponseEntity<AdherenceResponse> update(
            @PathVariable Long id,
            @RequestBody AdherenceTracking request
    ) {
        AdherenceTracking updated = adherenceTrackingService.update(id, request);
        return ResponseEntity.ok(adherenceTrackingService.toResponse(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        adherenceTrackingService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
