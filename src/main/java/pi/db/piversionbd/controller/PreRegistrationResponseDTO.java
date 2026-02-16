package pi.db.piversionbd.controller;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(name = "PreRegistrationResponse", description = "Réponse soumission pré-inscription")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PreRegistrationResponseDTO {

    @Schema(description = "Succès", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private boolean success;
    @Schema(description = "ID pré-inscription")
    private Long preRegistrationId;
    @Schema(description = "Statut")
    private String status;
    @Schema(description = "Message")
    private String message;
    @Schema(description = "Prix mensuel personnalisé")
    private Float calculatedPrice;
    @Schema(description = "Coefficient de risque")
    private Float riskCoefficient;
}
