package pi.db.piversionbd.repositories;

import pi.db.piversionbd.entities.health.Consultation;
import pi.db.piversionbd.entities.health.EtatConsultation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ConsultationRepository extends JpaRepository<Consultation, Long> {
    List<Consultation> findByMemberId(Long memberId);
    List<Consultation> findByDoctorId(Long doctorId);
    List<Consultation> findByEtatConsultation(EtatConsultation etatConsultation);
    List<Consultation> findByMemberIdAndEtatConsultation(Long memberId, EtatConsultation etatConsultation);
}

