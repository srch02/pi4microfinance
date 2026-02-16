package pi.db.piversionbd.services;

import pi.db.piversionbd.entities.health.Doctor;

import java.util.List;

public interface DoctorService {
    Doctor createDoctor(Doctor doctor);

    List<Doctor> getAllDoctors();

    Doctor getDoctorById(Long id);

    Doctor updateDoctor(Long id, Doctor doctor);

    void deleteDoctor(Long id);

    List<Doctor> findBySpecialty(String specialty);
}
