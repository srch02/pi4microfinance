package pi.db.piversionbd.controller;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Résultat de l'analyse "IA/ML-like" de l'historique médical Q&A.
 *
 * - qualityScore : qualité / complétude des réponses (0 = très mauvais, 1 = très bon)
 * - crude        : true si l'historique est jugé trop incomplet / pauvre
 * - fraudScore   : niveau de suspicion de fraude (0 = aucune, 1 = très suspect)
 * - fraudReason  : explication texte quand fraudScore est élevé
 */
@Schema(name = "MedicalHistoryAssessment", description = "Résultat (IA/ML-like) pour l'historique médical")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MedicalHistoryAssessmentDTO {

    @Schema(
        description = "Score de qualité des réponses (0 = très incomplet, 1 = très complet)",
        example = "0.65"
    )
    private Float qualityScore;

    @Schema(
        description = "Historique jugé 'crude' (trop incomplet) ou non. true = formulaire trop pauvre, admin doit demander plus d'informations.",
        example = "false"
    )
    private Boolean crude;

    @Schema(
        description = "Score de suspicion de fraude (0 = aucune suspicion, 1 = très forte suspicion)",
        example = "0.2"
    )
    private Float fraudScore;

    @Schema(
        description = "Raison de la suspicion si fraudScore est élevé (ex: contradictions, réponses manquantes, etc.)",
        example = "Many missing answers and contradictions detected."
    )
    private String fraudReason;
}