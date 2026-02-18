package pi.db.piversionbd.controller;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Map;

@Schema(name = "MedicalHistoryQaRequest", description = "Réponses Q&A pour l'historique médical")
@Data
public class MedicalHistoryQaRequestDTO {
    @Schema(description = "ID de la pré-inscription", example = "1")
    private Long preRegistrationId;

    @Schema(description = "Questions/Réponses (clé=question, valeur=réponse)")
    private Map<String, String> answers;
}

