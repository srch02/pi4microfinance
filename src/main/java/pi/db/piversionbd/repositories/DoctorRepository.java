package pi.db.piversionbd.repositories;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import pi.db.piversionbd.entities.health.Doctor;

import java.util.List;

@Repository
public interface DoctorRepository extends CrudRepository<Doctor,Long> {
    List<Doctor> findBySpecialty(String specialty);
}
