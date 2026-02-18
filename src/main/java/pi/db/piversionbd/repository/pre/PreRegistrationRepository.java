package pi.db.piversionbd.repository.pre;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pi.db.piversionbd.entities.pre.PreRegistration;

import java.util.Optional;

@Repository
public interface PreRegistrationRepository extends JpaRepository<PreRegistration, Long> {

    Optional<PreRegistration> findByCinNumber(String cinNumber);

    boolean existsByCinNumber(String cinNumber);
}

