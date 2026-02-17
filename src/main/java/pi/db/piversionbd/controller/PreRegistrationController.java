package pi.db.piversionbd.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import pi.db.piversionbd.entities.groups.PackageType;
import pi.db.piversionbd.entities.pre.FinancialStabilityLevel;
import pi.db.piversionbd.entities.pre.PreRegistration;
import pi.db.piversionbd.entities.pre.PreRegistrationStatus;
import pi.db.piversionbd.service.IPreRegistrationService;

import java.util.List;

@RestController
@RequestMapping("/api/pre-registration")
@RequiredArgsConstructor
public class PreRegistrationController {

    private final IPreRegistrationService preRegistrationService;

    @GetMapping("/all")
    @Operation(summary = "Liste toutes les pré-inscriptions")
    public ResponseEntity<List<PreRegistrationSummaryDTO>> getAllPreRegistrations() {
        return ResponseEntity.ok(preRegistrationService.getAllPreRegistrations());
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Crée une pré-inscription")
    public ResponseEntity<PreRegistrationResponseDTO> submit(@Valid @RequestBody PreRegistrationRequestDTO request) {
        PreRegistrationResponseDTO response = preRegistrationService.submitPreRegistration(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping(value = "/form", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Crée une pré-inscription (paramètres séparés, style placeholders)")
    public ResponseEntity<PreRegistrationResponseDTO> submitFromForm(
        @Parameter(description = "Numéro CIN (1 à 8 chiffres uniquement)", example = "12345678")
        @RequestParam String cinNumber,
        @Parameter(description = "Déclaration médicale complète", example = "Recurrent flu, seasonal allergies, no chronic disease")
        @RequestParam(required = false) String medicalDeclarationText,
        @Parameter(description = "Conditions actuelles", example = "rhume saisonnier, allergies légères")
        @RequestParam(required = false) String currentConditions,
        @Parameter(description = "Antécédents familiaux", example = "mère avec allergies, père en bonne santé")
        @RequestParam(required = false) String familyHistory,
        @Parameter(description = "Traitements en cours", example = "antihistaminiques pendant le printemps")
        @RequestParam(required = false) String ongoingTreatments,
        @Parameter(description = "Fréquence des consultations passées", example = "2 à 3 fois par an")
        @RequestParam(required = false) String consultationFrequency,
        @Parameter(description = "Âge en années", example = "35")
        @RequestParam(required = false) Integer age,
        @Parameter(description = "Profession", example = "employé de bureau")
        @RequestParam(required = false) String profession,
        @Parameter(
            description = "Stabilité financière (STABLE, MODERE, INSTABLE)",
            example = "STABLE",
            schema = @Schema(implementation = FinancialStabilityLevel.class)
        )
        @RequestParam(required = false) FinancialStabilityLevel financialStability,
        @Parameter(description = "Mois de maladie saisonnière par an", example = "3")
        @RequestParam(required = false) Integer seasonalIllnessMonthsPerYear
    ) {
        PreRegistrationRequestDTO dto = new PreRegistrationRequestDTO();
        dto.setCinNumber(cinNumber);
        dto.setMedicalDeclarationText(medicalDeclarationText);
        dto.setCurrentConditions(currentConditions);
        dto.setFamilyHistory(familyHistory);
        dto.setOngoingTreatments(ongoingTreatments);
        dto.setConsultationFrequency(consultationFrequency);
        dto.setAge(age);
        dto.setProfession(profession);
        dto.setFinancialStability(financialStability);
        dto.setSeasonalIllnessMonthsPerYear(seasonalIllnessMonthsPerYear);

        PreRegistrationResponseDTO response = preRegistrationService.submitPreRegistration(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping(value = "/update/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Met à jour une pré-inscription")
    public ResponseEntity<PreRegistrationSummaryDTO> updatePreRegistration(
        @Parameter(description = "ID de la pré-inscription", example = "1")
        @PathVariable Long id,
        @Valid @RequestBody PreRegistrationRequestDTO request
    ) {
        return ResponseEntity.ok(preRegistrationService.updatePreRegistration(id, request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Récupère par ID")
    public ResponseEntity<PreRegistrationSummaryDTO> getById(
        @Parameter(description = "ID de la pré-inscription", example = "1")
        @PathVariable Long id
    ) {
        PreRegistration pre = preRegistrationService.getPreRegistrationById(id);
        return ResponseEntity.ok(toSummary(pre));
    }

    @DeleteMapping("/delete/{id}")
    @Operation(summary = "Supprime une pré-inscription")
    public ResponseEntity<Void> deletePreRegistration(
        @Parameter(description = "ID de la pré-inscription", example = "1")
        @PathVariable Long id
    ) {
        preRegistrationService.deletePreRegistration(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Met à jour le statut (admin)")
    public ResponseEntity<PreRegistrationSummaryDTO> updateStatus(
        @Parameter(description = "ID de la pré-inscription", example = "1")
        @PathVariable Long id,
        @Parameter(
            description = "Nouveau statut",
            example = "APPROVED",
            schema = @Schema(implementation = PreRegistrationStatus.class)
        )
        @RequestParam PreRegistrationStatus status
    ) {
        PreRegistration pre = preRegistrationService.updatePreRegistrationStatus(id, status);
        return ResponseEntity.ok(toSummary(pre));
    }

    @PostMapping("/{id}/confirm-payment")
    @Operation(summary = "Confirme le paiement et active le compte")
    public ResponseEntity<PreRegistrationSummaryDTO> confirmPayment(
        @Parameter(description = "ID de la pré-inscription", example = "1")
        @PathVariable Long id,
        @Parameter(description = "Montant de paiement attendu", example = "75.50")
        @RequestParam Double paymentAmount
    ) {
        PreRegistration pre = preRegistrationService.confirmPayment(id, paymentAmount);
        return ResponseEntity.ok(toSummary(pre));
    }

    @PostMapping("/{id}/confirm-payment-by-package")
    @Operation(summary = "Confirme le paiement par type de package (BASIC, CONFORT, PREMIUM) et active le compte")
    public ResponseEntity<PreRegistrationSummaryDTO> confirmPaymentByPackage(
        @Parameter(description = "ID de la pré-inscription", example = "1")
        @PathVariable Long id,
        @Parameter(
            description = "Type de package",
            example = "BASIC",
            schema = @Schema(implementation = PackageType.class)
        )
        @RequestParam PackageType packageType
    ) {
        PreRegistration pre = preRegistrationService.confirmPaymentByPackage(id, packageType);
        return ResponseEntity.ok(toSummary(pre));
    }

    @PostMapping(path = "/medical-history/{medicalHistoryId}/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload d'un document d'historique médical lié à une pré-inscription")
    public ResponseEntity<Void> uploadMedicalHistory(
        @Parameter(description = "ID de l'historique médical", example = "1")
        @PathVariable Long medicalHistoryId,
        @Parameter(description = "Fichier à uploader", required = true)
        @RequestPart("file") MultipartFile file
    ) {
        preRegistrationService.uploadMedicalHistoryDocument(medicalHistoryId, file);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    private static PreRegistrationSummaryDTO toSummary(PreRegistration pre) {
        return PreRegistrationSummaryDTO.builder()
            .id(pre.getId())
            .cinNumber(pre.getCinNumber())
            .status(pre.getStatus())
            .fraudScore(pre.getFraudScore())
            .createdAt(pre.getCreatedAt())
            .build();
    }
}
