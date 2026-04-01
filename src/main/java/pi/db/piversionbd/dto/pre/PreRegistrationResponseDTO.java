package pi.db.piversionbd.dto.pre;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import pi.db.piversionbd.entities.pre.PreRegistrationStatus;

@Schema(name = "PreRegistrationResponse", description = "Réponse soumission pré-inscription")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PreRegistrationResponseDTO {

    @Schema(description = "Succès", requiredMode = Schema.RequiredMode.NOT_REQUIRED, example = "true")
    private boolean success;
    @Schema(description = "ID pré-inscription", example = "1")
    private Long preRegistrationId;
    @Schema(description = "Statut", example = "PENDING_REVIEW")
    private PreRegistrationStatus status;
    @Schema(description = "Message", example = "Application submitted. Admin will review within 2-48h.")
    private String message;
    @Schema(
        description = "Prix mensuel indicatif (BASIC), à utiliser plus tard à l’adhésion / au groupe — pas de paiement sur cette étape.",
        example = "25.00"
    )
    private Float calculatedPrice;
    @Schema(description = "Prix BASIC (identique à calculatedPrice)", example = "25.00")
    private Float priceBasic;
    @Schema(description = "Prix CONFORT indicatif (base × 1.3)", example = "32.50")
    private Float priceConfort;
    @Schema(description = "Prix PREMIUM indicatif (base × 1.6)", example = "40.00")
    private Float pricePremium;
    @Schema(description = "Coefficient de risque", example = "1.5")
    private Float riskCoefficient;
    @Schema(description = "Note d'exclusion (pour affichage alerte rouge)", example = "This chronic illness is not covered, but other conditions are.")
    private String exclusionsNote;
}
