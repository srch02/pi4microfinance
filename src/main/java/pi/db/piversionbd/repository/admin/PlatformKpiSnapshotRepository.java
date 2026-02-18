package pi.db.piversionbd.repository.admin;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pi.db.piversionbd.entities.admin.PlatformKpiSnapshot;

@Repository
public interface PlatformKpiSnapshotRepository
        extends JpaRepository<PlatformKpiSnapshot, Long> {
}

