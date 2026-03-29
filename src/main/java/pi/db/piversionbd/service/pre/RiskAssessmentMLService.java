package pi.db.piversionbd.service.pre;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import pi.db.piversionbd.controller.PreRegistrationRequestDTO;
import pi.db.piversionbd.dto.pre.RiskAssessmentMLRequest;
import pi.db.piversionbd.dto.pre.RiskAssessmentMLResponse;

import java.util.Map;

/**
 * Service d'appel à l'API ML (Flask) pour calcul du coefficient de risque.
 * Active uniquement si ml.api.enabled=true.
 */
@Service
@ConditionalOnProperty(name = "ml.api.enabled", havingValue = "true")
public class RiskAssessmentMLService {

    @Value("${ml.api.url:http://localhost:5000}")
    private String mlApiUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Prédit le risque à partir du DTO de pré-inscription.
     */
    public RiskAssessmentMLResponse predictRisk(PreRegistrationRequestDTO dto) {
        return predictRisk(RiskAssessmentMLRequest.builder()
                .age(dto.getAge())
                .fluFrequency(dto.getSeasonalIllnessMonthsPerYear() != null
                        ? dto.getSeasonalIllnessMonthsPerYear() : 0)
                .hasAllergies(hasAllergies(dto))
                .professionRisk(professionRiskLevel(dto.getProfession()))
                .familyHistory(dto.getFamilyHistory() != null && !dto.getFamilyHistory().isBlank())
                .ocrText(buildDeclarationText(dto))
                .build());
    }

    /**
     * Appel direct à l'API ML avec un payload structuré.
     */
    @SuppressWarnings("unchecked")
    public RiskAssessmentMLResponse predictRisk(RiskAssessmentMLRequest request) {
        Map<String, Object> body = Map.of(
                "age", request.getAge() != null ? request.getAge() : 0,
                "flu_frequency", request.getFluFrequency() != null ? request.getFluFrequency() : 0,
                "has_allergies", request.isHasAllergies(),
                "profession_risk", request.getProfessionRisk(),
                "family_history", request.isFamilyHistory(),
                "ocr_text", request.getOcrText() != null ? request.getOcrText() : ""
        );

        ResponseEntity<Map<String, Object>> response = restTemplate.postForEntity(
                mlApiUrl + "/predict",
                body,
                (Class<Map<String, Object>>) (Class<?>) Map.class
        );

        Map<String, Object> result = response.getBody();
        if (result == null) {
            return RiskAssessmentMLResponse.builder()
                    .isExcluded(false)
                    .riskCoefficient(1.0)
                    .confidence(0.0)
                    .build();
        }

        return RiskAssessmentMLResponse.builder()
                .isExcluded(Boolean.TRUE.equals(result.get("is_excluded")))
                .reason((String) result.get("reason"))
                .riskCoefficient(toDouble(result.get("risk_coefficient")))
                .confidence(toDouble(result.get("confidence")))
                .build();
    }

    private static boolean hasAllergies(PreRegistrationRequestDTO dto) {
        String fh = dto.getFamilyHistory();
        String cc = dto.getCurrentConditions();
        String md = dto.getMedicalDeclarationText();
        String lower = ((fh != null ? fh : "") + " " + (cc != null ? cc : "") + " " + (md != null ? md : "")).toLowerCase();
        return lower.contains("allerg") || lower.contains("asthma") || lower.contains("asthme");
    }

    private static int professionRiskLevel(String profession) {
        if (profession == null) return 0;
        String p = profession.toLowerCase();
        if (p.contains("construction") || p.contains("mining") || p.contains("heavy") || p.contains("minier")) {
            return 1;
        }
        return 0;
    }

    private static String buildDeclarationText(PreRegistrationRequestDTO dto) {
        StringBuilder sb = new StringBuilder();
        if (dto.getMedicalDeclarationText() != null) sb.append(dto.getMedicalDeclarationText()).append(" ");
        if (dto.getCurrentConditions() != null) sb.append(dto.getCurrentConditions()).append(" ");
        if (dto.getFamilyHistory() != null) sb.append(dto.getFamilyHistory()).append(" ");
        if (dto.getOngoingTreatments() != null) sb.append(dto.getOngoingTreatments()).append(" ");
        return sb.toString().trim();
    }

    private static Double toDouble(Object o) {
        if (o == null) return null;
        if (o instanceof Number n) return n.doubleValue();
        try {
            return Double.parseDouble(o.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
