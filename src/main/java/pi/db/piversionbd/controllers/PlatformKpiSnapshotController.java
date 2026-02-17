package pi.db.piversionbd.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pi.db.piversionbd.entities.admin.PlatformKpiSnapshot;
import pi.db.piversionbd.repositories.PlatformKpiSnapshotRepository;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/platform-kpi-snapshots")
public class PlatformKpiSnapshotController {

    @Autowired
    private PlatformKpiSnapshotRepository platformKpiSnapshotRepository;

    // ✅ CREATE
    @PostMapping
    public ResponseEntity<PlatformKpiSnapshot> createSnapshot(
            @RequestBody PlatformKpiSnapshot snapshot) {

        PlatformKpiSnapshot saved =
                platformKpiSnapshotRepository.save(snapshot);

        return ResponseEntity.ok(saved);
    }

    // ✅ READ ALL
    @GetMapping
    public List<PlatformKpiSnapshot> getAllSnapshots() {
        return platformKpiSnapshotRepository.findAll();
    }

    // ✅ READ BY ID
    @GetMapping("/{id}")
    public ResponseEntity<PlatformKpiSnapshot> getSnapshotById(
            @PathVariable Long id) {

        Optional<PlatformKpiSnapshot> snapshot =
                platformKpiSnapshotRepository.findById(id);

        return snapshot.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // ✅ UPDATE
    @PutMapping("/{id}")
    public ResponseEntity<PlatformKpiSnapshot> updateSnapshot(
            @PathVariable Long id,
            @RequestBody PlatformKpiSnapshot snapshotDetails) {

        Optional<PlatformKpiSnapshot> optionalSnapshot =
                platformKpiSnapshotRepository.findById(id);

        if (optionalSnapshot.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        PlatformKpiSnapshot snapshot = optionalSnapshot.get();

        snapshot.setMetricType(snapshotDetails.getMetricType());
        snapshot.setGroup(snapshotDetails.getGroup());
        snapshot.setPeriodStart(snapshotDetails.getPeriodStart());
        snapshot.setPeriodEnd(snapshotDetails.getPeriodEnd());
        snapshot.setMetrics(snapshotDetails.getMetrics());

        PlatformKpiSnapshot updated =
                platformKpiSnapshotRepository.save(snapshot);

        return ResponseEntity.ok(updated);
    }

    // ✅ DELETE
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSnapshot(@PathVariable Long id) {

        if (!platformKpiSnapshotRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }

        platformKpiSnapshotRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
