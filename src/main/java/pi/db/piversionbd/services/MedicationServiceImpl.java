package pi.db.piversionbd.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pi.db.piversionbd.entities.health.Medication;
import pi.db.piversionbd.repositories.MedicationRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MedicationServiceImpl implements MedicationService {

    private final MedicationRepository medicationRepository;

    @Override
    public Medication create(Medication medication) {
        return medicationRepository.save(medication);
    }

    @Override
    public List<Medication> getAll() {
        return medicationRepository.findAll();
    }

    @Override
    public Medication update(Long id, Medication medication) {
        Medication existing = medicationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Medication not found"));

        existing.setName(medication.getName());
        existing.setCategory(medication.getCategory());

        return medicationRepository.save(existing);
    }

    @Override
    public void delete(Long id) {
        medicationRepository.deleteById(id);
    }
}

