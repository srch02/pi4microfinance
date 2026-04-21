package pi.db.piversionbd.service.health;

import pi.db.piversionbd.entities.health.Doctor;
import pi.db.piversionbd.entities.health.Specialite;
import pi.db.piversionbd.repository.health.DoctorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class DoctorServiceImpl implements IDoctorService {

    @Autowired
    private DoctorRepository doctorRepository;

    @Override
    public Doctor saveDoctors(Doctor doctor) {
        return doctorRepository.save(doctor);
    }

    @Override
    public Optional<Doctor> getDoctorById(Long id) {
        return doctorRepository.findById(id);
    }

    @Override
    public List<Doctor> getAllDoctors() {
        return doctorRepository.findAll();
    }

    @Override
    public Doctor updateDoctor(Long id, Doctor doctor) {
        Optional<Doctor> existingDoctor = doctorRepository.findById(id);
        if (existingDoctor.isPresent()) {
            Doctor d = existingDoctor.get();
            if (doctor.getName() != null) {
                d.setName(doctor.getName());
            }
            if (doctor.getEmail() != null) {
                d.setEmail(doctor.getEmail());
            }
            if (doctor.getPassword() != null) {
                d.setPassword(doctor.getPassword());
            }
            if (doctor.getTypeDoctor() != null) {
                d.setTypeDoctor(doctor.getTypeDoctor());
            }
            if (doctor.getSpecialite() != null) {
                d.setSpecialite(doctor.getSpecialite());
            }
            return doctorRepository.save(d);
        }
        return null;
    }

    @Override
    public void deleteDoctor(Long id) {
        doctorRepository.deleteById(id);
    }

    @Override
    public Optional<Doctor> getDoctorByEmail(String email) {
        return doctorRepository.findByEmail(email);
    }

    @Override
    public List<Doctor> getDoctorsBySpecialite(Specialite specialite) {
        return doctorRepository.findBySpecialite(specialite);
    }
}

