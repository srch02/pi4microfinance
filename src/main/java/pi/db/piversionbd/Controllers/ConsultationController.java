package pi.db.piversionbd.Controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import pi.db.piversionbd.entities.health.Consultation;
import pi.db.piversionbd.entities.health.ConsultationStatus;
import pi.db.piversionbd.services.ConsultationService;

import java.util.List;

@RestController
@RequestMapping("/api/consultations")
@RequiredArgsConstructor
public class ConsultationController {

    private final ConsultationService consultationService;

    @PostMapping("/book/{memberId}/{doctorId}")
    public Consultation book(@PathVariable Long memberId,
                             @PathVariable Long doctorId,
                             @RequestBody Consultation consultation) {
        return consultationService.bookConsultation(memberId, doctorId, consultation);
    }

    @GetMapping("/member/{memberId}")
    public List<Consultation> byMember(@PathVariable Long memberId) {
        return consultationService.getMemberConsultations(memberId);
    }

    @GetMapping("/doctor/{doctorId}")
    public List<Consultation> byDoctor(@PathVariable Long doctorId) {
        return consultationService.getDoctorConsultations(doctorId);
    }

    @PutMapping("/{id}/status")
    public Consultation updateStatus(@PathVariable Long id,
                                     @RequestParam ConsultationStatus status) {
        return consultationService.updateStatus(id, status);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        consultationService.deleteConsultation(id);
    }
}

