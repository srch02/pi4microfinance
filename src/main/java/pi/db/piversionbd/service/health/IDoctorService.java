package pi.db.piversionbd.service.health;

import pi.db.piversionbd.entities.health.Doctor;
import pi.db.piversionbd.entities.health.Specialite;

import java.util.List;
import java.util.Optional;

public interface IDoctorService {
    Doctor saveDoctors(Doctor doctor);
    Optional<Doctor> getDoctorById(Long id);
    List<Doctor> getAllDoctors();
    Doctor updateDoctor(Long id, Doctor doctor);
    void deleteDoctor(Long id);
    Optional<Doctor> getDoctorByEmail(String email);
    List<Doctor> getDoctorsBySpecialite(Specialite specialite);
}

