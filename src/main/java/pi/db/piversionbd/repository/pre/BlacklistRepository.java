package pi.db.piversionbd.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pi.db.piversionbd.entities.pre.BlacklistEntry;

import java.util.Optional;

public interface BlacklistRepository extends JpaRepository<BlacklistEntry, Long> {
    Optional<BlacklistEntry> findByCinNumber(String cinNumber);
    boolean existsByCinNumber(String cinNumber);
}
