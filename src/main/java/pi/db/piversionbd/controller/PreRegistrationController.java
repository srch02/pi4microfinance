package pi.db.piversionbd.controller;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pi.db.piversionbd.entities.pre.PreRegistration;
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
    public ResponseEntity<PreRegistrationResponseDTO> submit(@RequestBody PreRegistrationRequestDTO request) {
        PreRegistrationResponseDTO response = preRegistrationService.submitPreRegistration(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping(value = "/update/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Met à jour une pré-inscription")
    public ResponseEntity<PreRegistrationSummaryDTO> updatePreRegistration(@PathVariable Long id, @RequestBody PreRegistrationRequestDTO request) {
        return ResponseEntity.ok(preRegistrationService.updatePreRegistration(id, request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Récupère par ID")
    public ResponseEntity<PreRegistrationSummaryDTO> getById(@PathVariable Long id) {
        PreRegistration pre = preRegistrationService.getPreRegistrationById(id);
        return ResponseEntity.ok(toSummary(pre));
    }

    @DeleteMapping("/delete/{id}")
    @Operation(summary = "Supprime une pré-inscription")
    public ResponseEntity<Void> deletePreRegistration(@PathVariable Long id) {
        preRegistrationService.deletePreRegistration(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Met à jour le statut (admin)")
    public ResponseEntity<PreRegistrationSummaryDTO> updateStatus(@PathVariable Long id, @RequestParam String status) {
        PreRegistration pre = preRegistrationService.updatePreRegistrationStatus(id, status);
        return ResponseEntity.ok(toSummary(pre));
    }

    @PostMapping("/{id}/confirm-payment")
    @Operation(summary = "Confirme le paiement et active le compte")
    public ResponseEntity<PreRegistrationSummaryDTO> confirmPayment(@PathVariable Long id, @RequestParam Double paymentAmount) {
        PreRegistration pre = preRegistrationService.confirmPayment(id, paymentAmount);
        return ResponseEntity.ok(toSummary(pre));
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
