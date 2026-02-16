package pi.db.piversionbd.controllers.score;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import pi.db.piversionbd.entities.score.Claim;
import pi.db.piversionbd.entities.score.ClaimDecisionReason;
import pi.db.piversionbd.entities.score.ClaimStatus;
import pi.db.piversionbd.services.score.ClaimService;

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
    public ResponseEntity<pi.db.piversionbd.dto.ClaimResponse> getById(@PathVariable Long id) {
        var c = claimService.getById(id);
        return ResponseEntity.ok(claimService.toResponse(c));
    }

    @GetMapping("/{id}/details")
    public ResponseEntity<Claim> getDetailsById(@PathVariable Long id) {
        return ResponseEntity.ok(claimService.getDetailsById(id));
    }

    @GetMapping("/by-number/{claimNumber}")
    public ResponseEntity<Claim> getByClaimNumber(@PathVariable String claimNumber) {
        return ResponseEntity.ok(claimService.getByClaimNumber(claimNumber));
    }

    @GetMapping
    public ResponseEntity<Page<Claim>> getAll(
            @RequestParam(required = false) ClaimStatus status,
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable
    ) {
        if (status != null) {
            return ResponseEntity.ok(claimService.getByStatus(status, pageable));
        }
        return ResponseEntity.ok(claimService.getAll(pageable));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Claim> update(@PathVariable Long id, @RequestBody Claim request) {
        return ResponseEntity.ok(claimService.update(id, request));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Claim> updateStatus(@PathVariable Long id, @RequestBody ClaimStatusUpdateRequest request) {
        return ResponseEntity.ok(
                claimService.updateStatus(
                        id,
                        request.getStatus(),
                        request.getReason(),
                        request.getComment()
                )
        );
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
