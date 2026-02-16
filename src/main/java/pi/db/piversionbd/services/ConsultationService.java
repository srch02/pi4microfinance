package pi.db.piversionbd.services;

import pi.db.piversionbd.entities.health.Consultation;
import pi.db.piversionbd.entities.health.ConsultationStatus;

import java.util.List;

public interface ConsultationService {
    Consultation bookConsultation(Long memberId, Long doctorId, Consultation consultation);

    List<Consultation> getMemberConsultations(Long memberId);

    List<Consultation> getDoctorConsultations(Long doctorId);

    Consultation updateStatus(Long consultationId, ConsultationStatus status);

    void deleteConsultation(Long id);
}
