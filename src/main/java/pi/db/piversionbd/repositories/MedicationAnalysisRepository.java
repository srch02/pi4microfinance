package pi.db.piversionbd.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pi.db.piversionbd.entities.health.MedicationAnalysis;
import java.util.List;

@Repository
public interface MedicationAnalysisRepository extends JpaRepository<MedicationAnalysis, Long> {
    List<MedicationAnalysis> findByMemberId(Long memberId);
    List<MedicationAnalysis> findByMedicationNameIgnoreCase(String medicationName);
}

