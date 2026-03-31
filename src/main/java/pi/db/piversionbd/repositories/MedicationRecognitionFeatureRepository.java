package pi.db.piversionbd.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pi.db.piversionbd.entities.health.MedicationRecognitionFeature;
import java.util.Optional;
import java.util.List;

@Repository
public interface MedicationRecognitionFeatureRepository extends JpaRepository<MedicationRecognitionFeature, Long> {
    Optional<MedicationRecognitionFeature> findByIdentificationCode(String identificationCode);
    List<MedicationRecognitionFeature> findByMedicationAnalysisId(Long medicationAnalysisId);
    List<MedicationRecognitionFeature> findByRecognitionConfidence(String confidence);
}

