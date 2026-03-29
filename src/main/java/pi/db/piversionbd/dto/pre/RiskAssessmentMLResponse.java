package pi.db.piversionbd.dto.pre;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Réponse de l'API ML (Flask) pour l'évaluation du risque.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RiskAssessmentMLResponse {
    private boolean isExcluded;
    private String reason;
    private Double riskCoefficient;
    private Double confidence;
}
