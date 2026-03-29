package pi.db.piversionbd.controller.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import pi.db.piversionbd.dto.pre.MedicalFormAnalyzeRequest;
import pi.db.piversionbd.dto.pre.MedicalScanResult;
import pi.db.piversionbd.dto.pre.PatientMedicalMatchDTO;
import pi.db.piversionbd.entities.pre.PreRegistration;
import pi.db.piversionbd.entities.pre.PreRegistrationStatus;
import pi.db.piversionbd.repository.PreRegistrationRepository;
import pi.db.piversionbd.service.pre.MedicalFormScannerService;

import java.util.List;

/**
 * API Admin pour le scan de formulaires médicaux papier.
 * POST /api/admin/scan/medical-form/{preRegistrationId} avec image multipart.
 */
@RestController
@RequestMapping("/api/admin/scan")
public class MedicalFormScannerController {

    @Autowired
    private MedicalFormScannerService scannerService;

    @Autowired
    private PreRegistrationRepository preRegistrationRepository;

    @Operation(summary = "Scanner un formulaire médical (image ou PDF)",
            description = "Envoie un document (image JPG/PNG ou PDF) du formulaire. "
                + "Pour les images: OCR via Google Vision (si activé). Pour les PDF: extraction texte via PDFBox, "
                + "puis détection des conditions exclues et comparaison avec l'historique Q&A.")
    @PostMapping(value = "/medical-form/{preRegistrationId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MedicalScanResult> scanForm(
            @Parameter(description = "ID de la pré-inscription à mettre à jour", required = true)
            @PathVariable Long preRegistrationId,
            @Parameter(description = "Fichier du formulaire (image JPG/PNG ou PDF)", required = true,
                    schema = @Schema(type = "string", format = "binary"))
            @RequestPart("file") MultipartFile file) {

        try {
            MedicalScanResult result = scannerService.scanMedicalForm(preRegistrationId, file);

            PreRegistration preReg = preRegistrationRepository.findById(preRegistrationId)
                    .orElse(null);

            if (preReg != null) {
                if (result.isRejected()) {
                    preReg.setStatus(PreRegistrationStatus.REJECTED);
                    preReg.setRejectionReason(result.getRejectionReason());
                } else {
                    preReg.setStatus(PreRegistrationStatus.PENDING_REVIEW);
                    preReg.setRejectionReason(null);
                }
                preRegistrationRepository.save(preReg);
            }

            return ResponseEntity.ok(result);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(503)
                    .body(MedicalScanResult.builder()
                            .rejectionReason("Service OCR non configuré: " + e.getMessage())
                            .rejected(true)
                            .build());
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }

    @Operation(summary = "Analyser un texte extrait",
            description = "Détecte les conditions exclues dans le texte (sans OCR). Utile pour tests ou si l'OCR est fait côté client.")
    @PostMapping("/medical-form/analyze-text")
    public ResponseEntity<MedicalScanResult> analyzeText(@RequestBody MedicalFormAnalyzeRequest request) {
        String text = request != null ? request.getText() : null;
        MedicalScanResult result = scannerService.analyzeExtractedText(text);
        return ResponseEntity.ok(result);
    }

    @Operation(
        summary = "Rechercher les patients par texte médical",
        description = "Exemple: envoyer 'bronchite' et récupérer tous les patients dont l'historique médical contient ce texte."
    )
    @PostMapping("/medical-form/search-patients")
    public ResponseEntity<List<PatientMedicalMatchDTO>> searchPatientsByMedicalText(
            @RequestBody MedicalFormAnalyzeRequest request) {
        String text = request != null ? request.getText() : null;
        return ResponseEntity.ok(scannerService.searchPatientsByMedicalText(text));
    }
}
