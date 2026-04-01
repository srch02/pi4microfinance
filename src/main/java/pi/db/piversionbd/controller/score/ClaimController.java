package pi.db.piversionbd.controller.score;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import pi.db.piversionbd.dto.score.ClaimCreateRequest;
import pi.db.piversionbd.dto.score.ClaimResponse;
import pi.db.piversionbd.entities.score.Claim;
import pi.db.piversionbd.entities.score.ClaimDecisionReason;
import pi.db.piversionbd.entities.score.ClaimStatus;
import pi.db.piversionbd.security.CurrentMemberResolver;
import pi.db.piversionbd.service.hedera.HederaClaimService;
import pi.db.piversionbd.service.score.ClaimService;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/claims")
@RequiredArgsConstructor
@Tag(
        name = "Claims",
        description = """
                **Authentication:** Click **Authorize** in Swagger and paste your JWT (member or admin login). \
                Requests without `Authorization: Bearer <token>` get **403** with an empty body (blocked by Spring Security before this controller).

                **Members:** On `GET /api/claims`, **omit** `memberId` (your id is taken from the JWT). \
                `GET /api/claims/{id}` only works if that claim belongs to you. Admins may query any member / claim."""
)
public class ClaimController {

    private final ClaimService claimService;
    private final HederaClaimService hederaClaimService;
    private final CurrentMemberResolver currentMemberResolver;

    @PostMapping
    @Operation(
            summary = "Create a new claim (submit)",
            description = """
                    Opens a claim in **SUBMITTED** status. **Member:** `memberId` is optional (filled from JWT). **Admin:** must send `memberId`.

                    Body requires `groupId` and `amountRequested`. **`documentUploadIds` is optional for now** (testing; no proofs attached if omitted). **`claimNumber` is optional** — if omitted, a unique value is generated (e.g. `CLM-20260401-XXXXXXXX`).

                    **JSON:** Use strict JSON — **no comma after the last property** (e.g. not `"amountRequested": 150,` before `}`).

                    Server checks: active membership, at least **5 payments** in that group, **one claim per month** per group, amount within **remaining annual** coverage; if `documentUploadIds` are sent, they must belong to the member.

                    When `claims.auto-score-on-submit=true` (default), a **default score** is applied immediately so **auto-approval** (if ≥90) and **Telegram** can run without calling `/api/claim-scorings`. Set `claims.auto-score-on-submit=false` to use admin-only scoring instead. **Telegram** requires `telegram_chat_id` on the member.""",
            requestBody = @RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(
                                            name = "minimal",
                                            summary = "Member token (no trailing comma)",
                                            value = "{\"groupId\": 2, \"amountRequested\": 150}"
                                    )
                            }
                    )
            )
    )
    public ResponseEntity<ClaimResponse> create(
            Authentication auth,
            @org.springframework.web.bind.annotation.RequestBody ClaimCreateRequest req
    ) {
        if (currentMemberResolver.isAdmin(auth)) {
            if (req.memberId == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "memberId is required for admin-created claims.");
            }
        } else {
            long me = currentMemberResolver.requireMemberId(auth);
            if (req.memberId != null && !req.memberId.equals(me)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "memberId must match the logged-in member.");
            }
            req.memberId = me;
        }
        var created = claimService.createFromIds(req);
        return ResponseEntity.status(201).body(claimService.toResponse(created));
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Get one claim by claim id",
            description = """
                    Returns claim fields (amounts, status, decision, member/group ids). **Member:** only if that claim is yours. **Admin:** any claim.

                    Same payload shape as `GET /{id}/details`; this path loads the claim with scoring-friendly details."""
    )
    public ResponseEntity<ClaimResponse> getById(Authentication auth, @PathVariable Long id) {
        var c = claimService.getDetailsById(id);
        assertClaimOwnedByCurrentMember(auth, c);
        return ResponseEntity.ok(claimService.toResponse(c));
    }

    @GetMapping("/{id}/details")
    @Operation(
            summary = "Get claim by id (with graph details)",
            description = """
                    Like `GET /api/claims/{id}` but loads related data for the same response DTO (e.g. scoring linkage in persistence layer). **Member:** own claim only. **Admin:** any."""
    )
    public ResponseEntity<ClaimResponse> getDetailsById(Authentication auth, @PathVariable Long id) {
        Claim c = claimService.getDetailsById(id);
        assertClaimOwnedByCurrentMember(auth, c);
        return ResponseEntity.ok(claimService.toResponse(c));
    }

    @GetMapping("/by-number/{claimNumber}")
    @Operation(
            summary = "Get claim by business claim number",
            description = """
                    Lookup by **claimNumber** (unique string) instead of the numeric database id. **Member:** only if the claim belongs to you. **Admin:** any."""
    )
    public ResponseEntity<ClaimResponse> getByClaimNumber(Authentication auth, @PathVariable String claimNumber) {
        Claim c = claimService.getByClaimNumber(claimNumber);
        assertClaimOwnedByCurrentMember(auth, c);
        return ResponseEntity.ok(claimService.toResponse(c));
    }

    @GetMapping("/test-hedera/{claimId}")
    @Operation(
            summary = "Test Hedera reimbursement audit (dev)",
            description = """
                    **No JWT required.** Forces a sample reimbursement write to the Hedera audit topic for a claim (debug). Not for production workflows.

                    **GET** only: returns SUCCESS/FAILED text or exception message."""
    )
    public ResponseEntity<String> testHedera(@PathVariable Long claimId) {
        try {
            Claim claim = claimService.getById(claimId);
            claim.setAmountApproved(new BigDecimal("50"));
            String hash = hederaClaimService.recordReimbursement(
                    claim, new BigDecimal("16.67")
            );
            return ResponseEntity.ok(
                    hash != null ? "SUCCESS: " + hash : "FAILED: hash is null"
            );
        } catch (Exception e) {
            return ResponseEntity.ok("EXCEPTION: " + e.getClass().getName() + ": " + e.getMessage());
        }
    }

    @GetMapping
    @Operation(
            summary = "List claims (paginated)",
            description = """
                    **Member:** omit `memberId` — lists **your** claims only. Optional `status` filters (e.g. SUBMITTED). Do not pass another member’s id.

                    **Admin:** optional `memberId` and/or `status`; with neither, returns **all** claims (paged). Use `page`, `size`, `sort` for pagination.

                    **GET** returns a page of claim summaries (same fields as single-claim response)."""
    )
    public ResponseEntity<Page<ClaimResponse>> getAll(
            Authentication auth,
            @RequestParam(required = false) Long memberId,
            @RequestParam(required = false) ClaimStatus status,
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable
    ) {
        if (currentMemberResolver.isAdmin(auth)) {
            if (memberId != null && status != null) {
                return ResponseEntity.ok(claimService.getByMemberIdAndStatus(memberId, status, pageable).map(claimService::toResponse));
            }
            if (memberId != null) {
                return ResponseEntity.ok(claimService.getByMemberId(memberId, pageable).map(claimService::toResponse));
            }
            if (status != null) {
                return ResponseEntity.ok(claimService.getByStatus(status, pageable).map(claimService::toResponse));
            }
            return ResponseEntity.ok(claimService.getAll(pageable).map(claimService::toResponse));
        }
        long me = currentMemberResolver.requireMemberId(auth);
        if (memberId != null && !memberId.equals(me)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only list your own claims (omit memberId).");
        }
        if (status != null) {
            return ResponseEntity.ok(claimService.getByMemberIdAndStatus(me, status, pageable).map(claimService::toResponse));
        }
        return ResponseEntity.ok(claimService.getByMemberId(me, pageable).map(claimService::toResponse));
    }

    @PutMapping("/{id}")
    @Operation(
            summary = "Replace/update claim fields (admin)",
            description = """
                    **Admin only.** Partial update of editable fields on the claim entity (amounts, status, decision fields, etc.). Members cannot call this.

                    **PUT** sends a claim-shaped body; use for admin corrections or back-office edits."""
    )
    public ResponseEntity<ClaimResponse> update(Authentication auth, @PathVariable Long id, @RequestBody Claim request) {
        requireAdmin(auth);
        Claim updated = claimService.update(id, request);
        return ResponseEntity.ok(claimService.toResponse(updated));
    }

    @PatchMapping("/{id}/status")
    @Operation(
            summary = "Change claim status / decision (admin)",
            description = """
                    **Admin only.** Sets **status**, **reason**, **comment**, optional **amountApproved** before approval. Triggers wallet/ledger side effects when the status is an approval (e.g. APPROVED_AUTO / APPROVED_MANUAL) per service rules.

                    **PATCH** body: `ClaimStatusUpdateRequest` (status, reason, comment, amountApproved)."""
    )
    public ResponseEntity<ClaimResponse> updateStatus(
            Authentication auth,
            @PathVariable Long id,
            @RequestBody ClaimStatusUpdateRequest request
    ) {
        requireAdmin(auth);
        if (request.getAmountApproved() != null) {
            Claim claim = claimService.getById(id);
            claim.setAmountApproved(request.getAmountApproved());
            claimService.save(claim);
        }
        Claim updated = claimService.updateStatus(id, request.getStatus(), request.getReason(), request.getComment());
        return ResponseEntity.ok(claimService.toResponse(updated));
    }

    @DeleteMapping("/{id}")
    @Operation(
            summary = "Delete a claim (admin)",
            description = """
                    **Admin only.** Permanently removes the claim record. Members cannot delete claims.

                    **DELETE** returns **204 No Content** on success."""
    )
    public ResponseEntity<Void> delete(Authentication auth, @PathVariable Long id) {
        requireAdmin(auth);
        claimService.delete(id);
        return ResponseEntity.noContent().build();
    }

    private void requireAdmin(Authentication auth) {
        if (!currentMemberResolver.isAdmin(auth)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin only.");
        }
    }

    private void assertClaimOwnedByCurrentMember(Authentication auth, Claim c) {
        if (currentMemberResolver.isAdmin(auth)) {
            return;
        }
        long me = currentMemberResolver.requireMemberId(auth);
        Long claimMemberId = c.getMember() != null ? c.getMember().getId() : null;
        if (claimMemberId == null || me != claimMemberId) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your claim.");
        }
    }

    // DTO local pour PATCH status
    public static class ClaimStatusUpdateRequest {
        private ClaimStatus status;
        private ClaimDecisionReason reason;
        private String comment;
        private BigDecimal amountApproved;

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

        public BigDecimal getAmountApproved() {
            return amountApproved;
        }

        public void setAmountApproved(BigDecimal amountApproved) {
            this.amountApproved = amountApproved;
        }
    }
}
