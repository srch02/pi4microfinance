package pi.db.piversionbd.service.health;

import pi.db.piversionbd.entities.health.Consultation;
import pi.db.piversionbd.entities.health.EtatConsultation;

import java.util.List;
import java.util.Optional;

public interface IConsultationService {
    Consultation saveConsultation(Consultation consultation);
    Optional<Consultation> getConsultationById(Long id);
    List<Consultation> getAllConsultations();
    Consultation updateConsultation(Long id, Consultation consultation);
    void deleteConsultation(Long id);
    List<Consultation> getConsultationsByMemberId(Long memberId);
    List<Consultation> getConsultationsByDoctorId(Long doctorId);
    List<Consultation> getConsultationsByEtat(EtatConsultation etatConsultation);
    List<Consultation> getConsultationsByMemberIdAndEtat(Long memberId, EtatConsultation etatConsultation);
}

