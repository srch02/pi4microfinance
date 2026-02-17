package pi.db.piversionbd.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pi.db.piversionbd.entities.admin.RetentionIntervention;
import pi.db.piversionbd.repositories.RetentionInterventionRepository;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/retention-interventions")
public class RetentionInterventionController {

    @Autowired
    private RetentionInterventionRepository retentionInterventionRepository;

    // ✅ CREATE
    @PostMapping
    public ResponseEntity<RetentionIntervention> createIntervention(
            @RequestBody RetentionIntervention intervention) {

        RetentionIntervention saved =
                retentionInterventionRepository.save(intervention);

        return ResponseEntity.ok(saved);
    }

    // ✅ READ ALL
    @GetMapping
    public List<RetentionIntervention> getAllInterventions() {
        return retentionInterventionRepository.findAll();
    }

    // ✅ READ BY ID
    @GetMapping("/{id}")
    public ResponseEntity<RetentionIntervention> getInterventionById(
            @PathVariable Long id) {

        Optional<RetentionIntervention> intervention =
                retentionInterventionRepository.findById(id);

        return intervention.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // ✅ READ BY MEMBER ID
    @GetMapping("/member/{memberId}")
    public List<RetentionIntervention> getByMemberId(
            @PathVariable Long memberId) {

        return retentionInterventionRepository.findByMemberId(memberId);
    }

    // ✅ READ BY EXECUTION STATUS
    @GetMapping("/executed/{status}")
    public List<RetentionIntervention> getByExecuted(
            @PathVariable Boolean status) {

        return retentionInterventionRepository.findByExecuted(status);
    }

    // ✅ UPDATE
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

    // ✅ DELETE
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteIntervention(@PathVariable Long id) {

        if (!retentionInterventionRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }

        retentionInterventionRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
