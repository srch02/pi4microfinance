package pi.db.piversionbd.controller;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(name = "PreRegistrationRequest", description = "Demande de pré-inscription (étapes 1-4)")
@Data
public class PreRegistrationRequestDTO {

    @Schema(description = "Numéro CIN (OCR ou saisie)")
    private String cinNumber;
    @Schema(description = "Déclaration médicale complète")
    private String medicalDeclarationText;
    @Schema(description = "Conditions actuelles (ex: rhume, allergies)")
    private String currentConditions;
    @Schema(description = "Antécédents familiaux")
    private String familyHistory;
    @Schema(description = "Traitements en cours")
    private String ongoingTreatments;
    @Schema(description = "Fréquence des consultations passées")
    private String consultationFrequency;
    @Schema(description = "Âge en années")
    private Integer age;
    @Schema(description = "Profession")
    private String profession;
    @Schema(description = "Stabilité financière (stable, modéré, instable)")
    private String financialStability;
    @Schema(description = "Mois de maladie saisonnière par an (ex: 3)")
    private Integer seasonalIllnessMonthsPerYear;
}
