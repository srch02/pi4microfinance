package pi.db.piversionbd.dto.pre;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Payload envoyé à l'API ML (Flask) pour prédiction du risque.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RiskAssessmentMLRequest {
    private Integer age;
    private Integer fluFrequency;
    private boolean hasAllergies;
    private int professionRisk;  // 0=bureau, 1=construction/risqué
    private boolean familyHistory;
    private String ocrText;       // Texte extrait du formulaire (optionnel)
}
