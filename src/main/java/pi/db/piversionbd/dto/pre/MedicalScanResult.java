package pi.db.piversionbd.dto.pre;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Résultat du scan OCR d'un formulaire médical papier.
 * - extractedText : texte brut extrait par l'OCR
 * - detectedConditions : conditions exclues détectées
 * - rejected : true si au moins une condition exclue trouvée
 * - confidenceScore : score de confiance (0–1)
 */
@Schema(description = "Résultat du scan d'un formulaire médical (OCR + détection conditions exclues + comparaison avec historique)")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MedicalScanResult {

    @Schema(description = "Texte brut extrait du document (image/PDF) après OCR ou parsing PDF.")
    private String extractedText;

    @Schema(description = "Liste des conditions exclues détectées dans le texte.")
    private List<String> detectedConditions;

    @Schema(description = "true si au moins une condition exclue a été détectée.")
    private boolean rejected;

    @Schema(description = "Raison du rejet (liste des conditions exclues détectées, etc.).")
    private String rejectionReason;

    @Schema(description = "Score de confiance global du scan (0–1).")
    private double confidenceScore;

    @Schema(description = "true si le contenu scanné est cohérent avec l'historique Q&A du patient, false en cas de contradiction, null si pas d'historique.")
    private Boolean consistentWithHistory;

    @Schema(description = "Explication texte de la cohérence / incohérence entre l'historique Q&A et le document scanné.")
    private String consistencyReason;
}
