package pi.db.piversionbd.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pi.db.piversionbd.dto.SystemAlertStatsDTO;
import pi.db.piversionbd.entities.admin.SystemAlert;
import pi.db.piversionbd.services.SystemAlertService;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/system-alerts")
@CrossOrigin("*")
public class SystemAlertController {

    @Autowired
    private SystemAlertService systemAlertService;

    // ✅ Get all alerts
    @GetMapping
    public ResponseEntity<List<SystemAlert>> getAllAlerts() {

        List<SystemAlert> alerts = systemAlertService.getAllAlerts();

        return ResponseEntity.ok(alerts);
    }

    // ✅ Get alert by ID
    @GetMapping("/{id}")
    public ResponseEntity<SystemAlert> getAlertById(@PathVariable Long id) {

        Optional<SystemAlert> alert = systemAlertService.getAlertById(id);

        return alert.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // ✅ Create alert
    @PostMapping
    public ResponseEntity<SystemAlert> createAlert(@RequestBody SystemAlert alert) {

        SystemAlert createdAlert = systemAlertService.createAlert(alert);

        return ResponseEntity.ok(createdAlert);
    }

    // ✅ Update alert
    @PutMapping("/{id}")
    public ResponseEntity<SystemAlert> updateAlert(
            @PathVariable Long id,
            @RequestBody SystemAlert alertDetails) {

        SystemAlert updatedAlert = systemAlertService.updateAlert(id, alertDetails);

        if (updatedAlert == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(updatedAlert);
    }

    // ✅ Delete alert
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAlert(@PathVariable Long id) {

        boolean deleted = systemAlertService.deleteAlert(id);

        if (!deleted) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.noContent().build();
    }

    // ✅ Get active alerts
    @GetMapping("/active/{active}")
    public ResponseEntity<List<SystemAlert>> getByActive(@PathVariable Boolean active) {

        List<SystemAlert> alerts = systemAlertService.getByActive(active);

        return ResponseEntity.ok(alerts);
    }

    // ✅ Get alerts by severity
    @GetMapping("/severity/{severity}")
    public ResponseEntity<List<SystemAlert>> getBySeverity(@PathVariable String severity) {

        List<SystemAlert> alerts = systemAlertService.getBySeverity(severity);

        return ResponseEntity.ok(alerts);
    }

    // ✅ Get alerts by region
    @GetMapping("/region/{region}")
    public ResponseEntity<List<SystemAlert>> getByRegion(@PathVariable String region) {

        List<SystemAlert> alerts = systemAlertService.getByRegion(region);

        return ResponseEntity.ok(alerts);
    }

    // PART 1: Stats
    @GetMapping("/stats")
    public ResponseEntity<SystemAlertStatsDTO> stats() {
        return ResponseEntity.ok(systemAlertService.stats());
    }

    // PART 2: Advanced search
    @GetMapping("/search")
    public ResponseEntity<List<SystemAlert>> search(
            @RequestParam(value = "severity", required = false) String severity,
            @RequestParam(value = "region", required = false) String region,
            @RequestParam(value = "active", required = false) Boolean active
    ) {
        return ResponseEntity.ok(systemAlertService.search(severity, region, active));
    }

    // PART 3: Auto deactivate expired
    @PutMapping("/deactivate-expired")
    public ResponseEntity<Map<String, Integer>> deactivateExpired() {
        int count = systemAlertService.deactivateExpiredAlerts();
        Map<String, Integer> payload = new java.util.HashMap<>();
        payload.put("deactivated", count);
        return ResponseEntity.ok(payload);
    }

    // PART 4: CSV export
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportCsv() {
        String csv = systemAlertService.exportCsv();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_PLAIN);
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=system-alerts.csv");
        return ResponseEntity.ok().headers(headers).body(csv.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }
}
