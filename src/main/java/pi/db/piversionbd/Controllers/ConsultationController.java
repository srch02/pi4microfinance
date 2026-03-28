package pi.db.piversionbd.Controllers;

import pi.db.piversionbd.entities.health.Consultation;
import pi.db.piversionbd.entities.health.EtatConsultation;
import pi.db.piversionbd.services.IConsultationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/consultations")
@CrossOrigin(origins = "*")
@Tag(name = "Consultations", description = "API for managing consultations between doctors and members")
public class ConsultationController {

    @Autowired
    private IConsultationService consultationService;


    @PostMapping
    @Operation(summary = "Create a new consultation", description = "Books a new consultation between a doctor and a member")
    @ApiResponse(responseCode = "201", description = "Consultation created successfully")
    public ResponseEntity<Consultation> createConsultation(@RequestBody Consultation consultation) {
        Consultation savedConsultation = consultationService.saveConsultation(consultation);
        return new ResponseEntity<>(savedConsultation, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get consultation by ID", description = "Retrieves a consultation by its ID")
    @ApiResponse(responseCode = "200", description = "Consultation found")
    @ApiResponse(responseCode = "404", description = "Consultation not found")
    public ResponseEntity<Consultation> getConsultationById(@PathVariable Long id) {
        Optional<Consultation> consultation = consultationService.getConsultationById(id);
        if (consultation.isPresent()) {
            return new ResponseEntity<>(consultation.get(), HttpStatus.OK);
        }
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    @GetMapping
    @Operation(summary = "Get all consultations", description = "Retrieves a list of all consultations")
    @ApiResponse(responseCode = "200", description = "Consultations retrieved successfully")
    public ResponseEntity<List<Consultation>> getAllConsultations() {
        List<Consultation> consultations = consultationService.getAllConsultations();
        return new ResponseEntity<>(consultations, HttpStatus.OK);
    }

    @GetMapping("/member/{memberId}")
    @Operation(summary = "Get consultations by member ID", description = "Retrieves all consultations for a specific member")
    @ApiResponse(responseCode = "200", description = "Consultations retrieved successfully")
    public ResponseEntity<List<Consultation>> getConsultationsByMemberId(@PathVariable Long memberId) {
        List<Consultation> consultations = consultationService.getConsultationsByMemberId(memberId);
        return new ResponseEntity<>(consultations, HttpStatus.OK);
    }

    @GetMapping("/doctor/{doctorId}")
    @Operation(summary = "Get consultations by doctor ID", description = "Retrieves all consultations for a specific doctor")
    @ApiResponse(responseCode = "200", description = "Consultations retrieved successfully")
    public ResponseEntity<List<Consultation>> getConsultationsByDoctorId(@PathVariable Long doctorId) {
        List<Consultation> consultations = consultationService.getConsultationsByDoctorId(doctorId);
        return new ResponseEntity<>(consultations, HttpStatus.OK);
    }

    @GetMapping("/etat/{etat}")
    @Operation(summary = "Get consultations by status", description = "Retrieves all consultations with a specific status")
    @ApiResponse(responseCode = "200", description = "Consultations retrieved successfully")
    @ApiResponse(responseCode = "400", description = "Invalid status")
    public ResponseEntity<List<Consultation>> getConsultationsByEtat(@PathVariable String etat) {
        try {
            EtatConsultation etatConsultation = EtatConsultation.valueOf(etat);
            List<Consultation> consultations = consultationService.getConsultationsByEtat(etatConsultation);
            return new ResponseEntity<>(consultations, HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("/member/{memberId}/etat/{etat}")
    @Operation(summary = "Get member consultations by status", description = "Retrieves consultations for a specific member with a specific status")
    @ApiResponse(responseCode = "200", description = "Consultations retrieved successfully")
    @ApiResponse(responseCode = "400", description = "Invalid status")
    public ResponseEntity<List<Consultation>> getConsultationsByMemberIdAndEtat(
            @PathVariable Long memberId,
            @PathVariable String etat) {
        try {
            EtatConsultation etatConsultation = EtatConsultation.valueOf(etat);
            List<Consultation> consultations = consultationService.getConsultationsByMemberIdAndEtat(memberId, etatConsultation);
            return new ResponseEntity<>(consultations, HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update consultation", description = "Updates an existing consultation's information")
    @ApiResponse(responseCode = "200", description = "Consultation updated successfully")
    @ApiResponse(responseCode = "404", description = "Consultation not found")
    public ResponseEntity<Consultation> updateConsultation(@PathVariable Long id, @RequestBody Consultation consultation) {
        Consultation updatedConsultation = consultationService.updateConsultation(id, consultation);
        if (updatedConsultation != null) {
            return new ResponseEntity<>(updatedConsultation, HttpStatus.OK);
        }
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete consultation", description = "Deletes a consultation by its ID")
    @ApiResponse(responseCode = "204", description = "Consultation deleted successfully")
    public ResponseEntity<Void> deleteConsultation(@PathVariable Long id) {
        consultationService.deleteConsultation(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
    // ... existing endpoints ...

}

