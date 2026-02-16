package pi.db.piversionbd.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pi.db.piversionbd.entities.health.PharmacyRecommendation;

@Repository

public interface PharmacyRecommendationRepository extends JpaRepository<PharmacyRecommendation, Long> {
}
