package pi.db.piversionbd.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pi.db.piversionbd.entities.pre.PreRegistration;

import java.util.Optional;

public interface PreRegistrationRepository extends JpaRepository<PreRegistration, Long> {
    Optional<PreRegistration> findByCinNumber(String cinNumber);
    boolean existsByCinNumber(String cinNumber);
}