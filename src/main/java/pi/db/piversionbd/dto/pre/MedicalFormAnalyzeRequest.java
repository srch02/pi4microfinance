package pi.db.piversionbd.dto.pre;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(name = "MedicalFormAnalyzeRequest", description = "Request pour analyser un texte extrait d'un formulaire médical")
@Data
public class MedicalFormAnalyzeRequest {

    @Schema(description = "Texte médical extrait", example = "Patient mentionne diabetes et allergies saisonnières...")
    private String text;
}
