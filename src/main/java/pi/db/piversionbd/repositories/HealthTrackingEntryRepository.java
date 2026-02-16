package pi.db.piversionbd.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pi.db.piversionbd.entities.health.HealthTrackingEntry;

import java.util.List;

@Repository
public interface HealthTrackingEntryRepository
        extends JpaRepository<HealthTrackingEntry, Long> {

    List<HealthTrackingEntry> findByMemberId(Long memberId);
}

