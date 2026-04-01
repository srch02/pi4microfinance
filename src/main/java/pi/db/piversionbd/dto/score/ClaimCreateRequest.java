package pi.db.piversionbd.dto.score;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "Create a claim. `documentUploadIds` is optional for now (testing); will be required when upload flow is finalized.")
public class ClaimCreateRequest {

    @Schema(description = "Member id. Optional for portal JWT; required when an admin creates the claim.")
    public Long memberId;

    @Schema(description = "Solidarity group id for this claim.", example = "1")
    public Long groupId;

    @Schema(
            description = """
                    Optional. If omitted or blank, the server generates a unique reference, e.g. **CLM-20260401-A1B2C3D4** (date + random suffix).
                    If you set it, it must be unique (trimmed, no duplicate)."""
            ,
            example = "CLM-20260401-A1B2C3D4",
            nullable = true
    )
    public String claimNumber;

    @Schema(description = "Amount requested in DT (Tunisian dinars).", example = "150.00")
    public BigDecimal amountRequested;

    @Schema(
            description = """
                    Optional for now. When set: primary keys of **DOCUMENT_UPLOADS** rows (proofs). When omitted or null, claim is created without attached proofs (testing)."""
            ,
            example = "[12, 13]",
            nullable = true
    )
    public List<Long> documentUploadIds;
}
