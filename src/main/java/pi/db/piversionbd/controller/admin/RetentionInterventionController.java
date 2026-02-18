package pi.db.piversionbd.controller.admin;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pi.db.piversionbd.entities.admin.RetentionIntervention;
import pi.db.piversionbd.repository.admin.RetentionInterventionRepository;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/retention-interventions")
public class RetentionInterventionController {

    @Autowired
    private RetentionInterventionRepository retentionInterventionRepository;

    @PostMapping
    public ResponseEntity<RetentionIntervention> createIntervention(
            @RequestBody RetentionIntervention intervention) {

        RetentionIntervention saved =
                retentionInterventionRepository.save(intervention);

        return ResponseEntity.ok(saved);
    }

    @GetMapping
    public List<RetentionIntervention> getAllInterventions() {
        return retentionInterventionRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<RetentionIntervention> getInterventionById(
            @PathVariable Long id) {

        Optional<RetentionIntervention> intervention =
                retentionInterventionRepository.findById(id);

        return intervention.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/member/{memberId}")
    public List<RetentionIntervention> getByMemberId(
            @PathVariable Long memberId) {

        return retentionInterventionRepository.findByMemberId(memberId);
    }

    @GetMapping("/executed/{status}")
    public List<RetentionIntervention> getByExecuted(
            @PathVariable Boolean status) {

        return retentionInterventionRepository.findByExecuted(status);
    }

    @PutMapping("/{id}")
    public ResponseEntity<RetentionIntervention> updateIntervention(
            @PathVariable Long id,
            @RequestBody RetentionIntervention interventionDetails) {

        Optional<RetentionIntervention> optionalIntervention =
                retentionInterventionRepository.findById(id);

        if (optionalIntervention.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        RetentionIntervention intervention = optionalIntervention.get();

        intervention.setMember(interventionDetails.getMember());
        intervention.setPrediction(interventionDetails.getPrediction());
        intervention.setActionType(interventionDetails.getActionType());
        intervention.setDiscountPercentage(interventionDetails.getDiscountPercentage());
        intervention.setExecuted(interventionDetails.getExecuted());
        intervention.setResult(interventionDetails.getResult());

        RetentionIntervention updated =
                retentionInterventionRepository.save(intervention);

        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteIntervention(@PathVariable Long id) {

        if (!retentionInterventionRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }

        retentionInterventionRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}

