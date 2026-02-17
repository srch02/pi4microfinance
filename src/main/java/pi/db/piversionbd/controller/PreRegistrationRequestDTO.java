package pi.db.piversionbd.controller;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import pi.db.piversionbd.entities.pre.FinancialStabilityLevel;

@Schema(name = "PreRegistrationRequest", description = "Demande de pré-inscription (étapes 1-4)")
@Data
public class PreRegistrationRequestDTO {

    @Schema(
        description = "Numéro CIN (8 chiffres maximum)",
        maxLength = 8,
        example = "12345678"
    )
    @Size(max = 8, message = "Le CIN doit contenir au maximum 8 chiffres")
    @Pattern(regexp = "\\d{1,8}", message = "Le CIN doit contenir uniquement des chiffres (1 à 8)")
    private String cinNumber;

    @Schema(
        description = "Déclaration médicale complète",
        example = "Recurrent flu, seasonal allergies, no chronic disease"
    )
    private String medicalDeclarationText;

    @Schema(
        description = "Conditions actuelles",
        example = "rhume saisonnier, allergies légères"
    )
    private String currentConditions;

    @Schema(
        description = "Antécédents familiaux",
        example = "mère avec allergies, père en bonne santé"
    )
    private String familyHistory;

    @Schema(
        description = "Traitements en cours",
        example = "antihistaminiques pendant le printemps"
    )
    private String ongoingTreatments;

    @Schema(
        description = "Fréquence des consultations passées",
        example = "2 à 3 fois par an"
    )
    private String consultationFrequency;

    @Schema(
        description = "Âge en années",
        example = "35"
    )
    private Integer age;

    @Schema(
        description = "Profession",
        example = "employé de bureau"
    )
    private String profession;

    @Schema(
        description = "Stabilité financière (STABLE, MODERE, INSTABLE)",
        example = "STABLE"
    )
    private FinancialStabilityLevel financialStability;

    @Schema(
        description = "Mois de maladie saisonnière par an",
        example = "3"
    )
    private Integer seasonalIllnessMonthsPerYear;
}
