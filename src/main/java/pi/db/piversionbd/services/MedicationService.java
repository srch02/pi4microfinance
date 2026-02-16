package pi.db.piversionbd.services;

import pi.db.piversionbd.entities.health.Medication;

import java.util.List;

public interface MedicationService {

    Medication create(Medication medication);

    List<Medication> getAll();

    Medication update(Long id, Medication medication);

    void delete(Long id);
}

