package pi.db.piversionbd.controller;

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
    @Schema(description = "Prix mensuel personnalisé", example = "75.50")
    private Float calculatedPrice;
    @Schema(description = "Coefficient de risque", example = "1.5")
    private Float riskCoefficient;
    @Schema(description = "Note d'exclusion (pour affichage alerte rouge)", example = "This chronic illness is not covered, but other conditions are.")
    private String exclusionsNote;
}
