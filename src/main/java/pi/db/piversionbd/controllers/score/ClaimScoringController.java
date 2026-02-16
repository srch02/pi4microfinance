package pi.db.piversionbd.controllers.score;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pi.db.piversionbd.entities.score.ClaimScoring;
import pi.db.piversionbd.services.score.ClaimScoringService;

@RestController
@RequestMapping("/api/claim-scorings")
@RequiredArgsConstructor
public class ClaimScoringController {

    private final ClaimScoringService claimScoringService;

    @GetMapping("/{id}")
    public ResponseEntity<ClaimScoring> getById(@PathVariable Long id) {
        return ResponseEntity.ok(claimScoringService.getById(id));
    }

    @GetMapping("/by-claim/{claimId}")
    public ResponseEntity<ClaimScoring> getByClaimId(@PathVariable Long claimId) {
        return ResponseEntity.ok(claimScoringService.getByClaimId(claimId));
    }

    /**
     * Upsert scoring for a claim:
     * - crée si absent
     * - met à jour si existe
     */
    @PutMapping("/by-claim/{claimId}")
    public ResponseEntity<ClaimScoring> upsertByClaimId(
            @PathVariable Long claimId,
            @RequestBody ClaimScoring request
    ) {
        return ResponseEntity.ok(claimScoringService.upsertByClaimId(claimId, request));
    }

    @DeleteMapping("/by-claim/{claimId}")
    public ResponseEntity<Void> deleteByClaimId(@PathVariable Long claimId) {
        claimScoringService.deleteByClaimId(claimId);
        return ResponseEntity.noContent().build();
    }
}
