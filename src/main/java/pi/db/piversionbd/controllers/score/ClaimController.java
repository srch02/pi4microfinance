package pi.db.piversionbd.controllers.score;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import pi.db.piversionbd.dto.ClaimResponse;
import pi.db.piversionbd.entities.score.Claim;
import pi.db.piversionbd.entities.score.ClaimDecisionReason;
import pi.db.piversionbd.entities.score.ClaimStatus;
import pi.db.piversionbd.services.score.ClaimService;
import pi.db.piversionbd.dto.ClaimCreateRequest;

import java.net.URI;

@RestController
@RequestMapping("/api/claims")
@RequiredArgsConstructor
public class ClaimController {

    private final ClaimService claimService;

    @PostMapping
    public ResponseEntity<pi.db.piversionbd.dto.ClaimResponse> create(
            @RequestBody pi.db.piversionbd.dto.ClaimCreateRequest req
    ) {
        var created = claimService.createFromIds(req);
        return ResponseEntity.status(201).body(claimService.toResponse(created));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ClaimResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(claimService.getById(id));
    }

    @GetMapping("/{id}/details")
    public ResponseEntity<pi.db.piversionbd.dto.ClaimResponse> getDetailsById(@PathVariable Long id) {
        Claim c = claimService.getDetailsById(id);
        return ResponseEntity.ok(claimService.toResponse(c));
    }

    @GetMapping("/by-number/{claimNumber}")
    public ResponseEntity<pi.db.piversionbd.dto.ClaimResponse> getByClaimNumber(@PathVariable String claimNumber) {
        Claim c = claimService.getByClaimNumber(claimNumber);
        return ResponseEntity.ok(claimService.toResponse(c));
    }

    @GetMapping
    public ResponseEntity<Page<pi.db.piversionbd.dto.ClaimResponse>> getAll(
            @RequestParam(required = false) ClaimStatus status,
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable
    ) {
        if (status != null) {
            return ResponseEntity.ok(claimService.getByStatus(status, pageable).map(claimService::toResponse));
        }
        return ResponseEntity.ok(claimService.getAll(pageable).map(claimService::toResponse));
    }

    @PutMapping("/{id}")
    public ResponseEntity<pi.db.piversionbd.dto.ClaimResponse> update(@PathVariable Long id, @RequestBody Claim request) {
        Claim updated = claimService.update(id, request);
        return ResponseEntity.ok(claimService.toResponse(updated));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<pi.db.piversionbd.dto.ClaimResponse> updateStatus(
            @PathVariable Long id,
            @RequestBody ClaimStatusUpdateRequest request
    ) {
        Claim updated = claimService.updateStatus(id, request.getStatus(), request.getReason(), request.getComment());
        return ResponseEntity.ok(claimService.toResponse(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        claimService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // DTO local pour PATCH status
    public static class ClaimStatusUpdateRequest {
        private ClaimStatus status;
        private ClaimDecisionReason reason;
        private String comment;

        public ClaimStatus getStatus() {
            return status;
        }

        public void setStatus(ClaimStatus status) {
            this.status = status;
        }

        public ClaimDecisionReason getReason() {
            return reason;
        }

        public void setReason(ClaimDecisionReason reason) {
            this.reason = reason;
        }

        public String getComment() {
            return comment;
        }

        public void setComment(String comment) {
            this.comment = comment;
        }
    }
}
