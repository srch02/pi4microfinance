package pi.db.piversionbd.repositories;

import pi.db.piversionbd.entities.health.Doctor;
import pi.db.piversionbd.entities.health.Specialite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DoctorRepository extends JpaRepository<Doctor, Long> {
    Optional<Doctor> findByEmail(String email);
    List<Doctor> findBySpecialite(Specialite specialite);
}

