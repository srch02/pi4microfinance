package pi.db.piversionbd.service;

import org.springframework.stereotype.Service;
import pi.db.piversionbd.controller.MedicalHistoryAssessmentDTO;

import java.text.Normalizer;
import java.util.Map;

/**
 * "IA/ML-like" scorer (heuristics today, replaceable by real ML later).
 * Produces:
 * - qualityScore (completeness/consistency)
 * - crude flag (insufficient data)
 * - fraudScore + reason (suspicious patterns)
 */
@Service
public class MedicalHistoryAiService {

    public MedicalHistoryAssessmentDTO assess(Map<String, String> answers) {
        if (answers == null || answers.isEmpty()) {
            return MedicalHistoryAssessmentDTO.builder()
                .qualityScore(0.0f)
                .crude(true)
                .fraudScore(0.0f)
                .fraudReason("No answers provided.")
                .build();
        }

        int total = answers.size();
        int blank = 0;
        int shortAns = 0;
        StringBuilder all = new StringBuilder();

        for (var e : answers.entrySet()) {
            String q = safe(e.getKey());
            String a = safe(e.getValue());
            if (a.isBlank()) blank++;
            if (a.trim().length() > 0 && a.trim().length() < 4) shortAns++;
            all.append(q).append(" ").append(a).append(" ");
        }

        String text = normalize(all.toString());

        // Quality: 1 - penalties
        float blankRatio = total == 0 ? 1f : (blank / (float) total);
        float shortRatio = total == 0 ? 1f : (shortAns / (float) total);
        float quality = 1.0f - (0.7f * blankRatio + 0.3f * shortRatio);
        quality = clamp01(quality);

        boolean crude = quality < 0.55f || total < 4;

        // Fraud heuristics: contradictions + extreme/rare signals with "no chronic"
        float fraud = 0.0f;
        String reason = null;

        boolean saysNoChronic = text.contains("no chronic") || text.contains("pas de maladie chronique") || text.contains("aucune maladie chronique");
        boolean hasHeavy = text.contains("dialyse") || text.contains("dialysis") || text.contains("chemotherapy") || text.contains("chimiotherapie")
            || text.contains("radiotherapy") || text.contains("radiotherapie") || text.contains("insulin") || text.contains("insuline");

        if (blankRatio >= 0.5f) {
            fraud += 0.3f;
            reason = append(reason, "Many missing answers.");
        }
        if (saysNoChronic && hasHeavy) {
            fraud += 0.6f;
            reason = append(reason, "Contradiction: 'no chronic disease' but heavy treatments mentioned.");
        }

        fraud = clamp01(fraud);

        return MedicalHistoryAssessmentDTO.builder()
            .qualityScore(quality)
            .crude(crude)
            .fraudScore(fraud)
            .fraudReason(reason)
            .build();
    }

    private static float clamp01(float v) {
        return Math.max(0f, Math.min(v, 1f));
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    private static String normalize(String input) {
        String lower = input == null ? "" : input.toLowerCase();
        String nfd = Normalizer.normalize(lower, Normalizer.Form.NFD);
        return nfd.replaceAll("\\p{M}", "");
    }

    private static String append(String base, String add) {
        if (add == null || add.isBlank()) return base;
        if (base == null || base.isBlank()) return add;
        return base + " " + add;
    }
}

