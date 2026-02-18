package pi.db.piversionbd.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pi.db.piversionbd.entities.pre.MedicalHistory;

import java.util.List;

public interface MedicalHistoryRepository extends JpaRepository<MedicalHistory, Long> {
    List<MedicalHistory> findByPreRegistration_Id(Long preRegistrationId);
}