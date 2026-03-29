package pi.db.piversionbd.dto.pre;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class MedicalFormAnalyzeRequest {
    @Schema(description = "Texte extrait du formulaire médical à analyser", example = "Patient en bonne santé, pas de maladie chronique. Allergies saisonnières légères.")
    private String text;
}
