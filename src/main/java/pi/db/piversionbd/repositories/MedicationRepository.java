package pi.db.piversionbd.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pi.db.piversionbd.entities.health.Medication;

@Repository
public interface MedicationRepository extends JpaRepository<Medication, Long> {
}
