package pi.db.piversionbd.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pi.db.piversionbd.entities.health.Consultation;
import pi.db.piversionbd.entities.health.ConsultationStatus;

import java.util.List;

@Repository
public interface ConsultationRepository extends JpaRepository<Consultation, Long> {

    List<Consultation> findByMemberId(Long memberId);

    List<Consultation> findByDoctorId(Long doctorId);

    long countByMemberIdAndStatus(Long memberId, ConsultationStatus status);
}

