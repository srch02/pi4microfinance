package pi.db.piversionbd.Controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import pi.db.piversionbd.entities.health.Medication;
import pi.db.piversionbd.services.MedicationService;

import java.util.List;

@RestController
@RequestMapping("/api/medications")
@RequiredArgsConstructor
public class MedicationController {

    private final MedicationService service;

    @PostMapping
    public Medication create(@RequestBody Medication medication) {
        return service.create(medication);
    }

    @GetMapping
    public List<Medication> getAll() {
        return service.getAll();
    }

    @PutMapping("/{id}")
    public Medication update(@PathVariable Long id,
                             @RequestBody Medication medication) {
        return service.update(id, medication);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}

