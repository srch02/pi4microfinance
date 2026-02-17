package pi.db.piversionbd.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pi.db.piversionbd.dto.SystemAlertStatsDTO;
import pi.db.piversionbd.entities.admin.SystemAlert;
import pi.db.piversionbd.repositories.SystemAlertRepository;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class SystemAlertService {

    @Autowired
    private SystemAlertRepository systemAlertRepository;

    public List<SystemAlert> getAllAlerts() {
        return systemAlertRepository.findAll();
    }

    public Optional<SystemAlert> getAlertById(Long id) {
        return systemAlertRepository.findById(id);
    }

    public SystemAlert createAlert(SystemAlert alert) {
        return systemAlertRepository.save(alert);
    }

    public SystemAlert updateAlert(Long id, SystemAlert alertDetails) {

        Optional<SystemAlert> optional = systemAlertRepository.findById(id);

        if (optional.isPresent()) {

            SystemAlert alert = optional.get();

            alert.setAlertType(alertDetails.getAlertType());
            alert.setSeverity(alertDetails.getSeverity());
            alert.setRegion(alertDetails.getRegion());
            alert.setTitle(alertDetails.getTitle());
            alert.setMessage(alertDetails.getMessage());
            alert.setActive(alertDetails.getActive());
            alert.setExpirationDate(alertDetails.getExpirationDate());

            return systemAlertRepository.save(alert);
        }

        return null;
    }

    public boolean deleteAlert(Long id) {

        if (systemAlertRepository.existsById(id)) {

            systemAlertRepository.deleteById(id);

            return true;
        }

        return false;
    }

    public List<SystemAlert> getByActive(Boolean active) {
        return systemAlertRepository.findByActive(active);
    }

    public List<SystemAlert> getBySeverity(String severity) {
        return systemAlertRepository.findBySeverity(severity);
    }

    public List<SystemAlert> getByRegion(String region) {
        return systemAlertRepository.findByRegion(region);
    }

    // PART 1: Stats
    public SystemAlertStatsDTO stats() {
        SystemAlertStatsDTO dto = new SystemAlertStatsDTO();
        dto.totalAlerts = systemAlertRepository.totalAlerts();
        dto.activeAlerts = systemAlertRepository.activeAlerts();
        dto.inactiveAlerts = systemAlertRepository.inactiveAlerts();
        dto.criticalAlerts = systemAlertRepository.criticalAlerts();
        dto.alertsBySeverity = toMap(systemAlertRepository.countGroupBySeverity());
        dto.alertsByRegion = toMap(systemAlertRepository.countGroupByRegion());
        return dto;
    }

    private Map<String, Long> toMap(List<Object[]> tuples) {
        Map<String, Long> map = new LinkedHashMap<>();
        for (Object[] t : tuples) {
            if (t[0] != null) {
                map.put(String.valueOf(t[0]), ((Number) t[1]).longValue());
            } else {
                map.put("UNKNOWN", ((Number) t[1]).longValue());
            }
        }
        return map;
    }

    // PART 2: Advanced search
    public List<SystemAlert> search(String severity, String region, Boolean active) {
        return systemAlertRepository.search(severity, region, active);
    }

    // PART 3: Auto deactivate expired alerts
    public int deactivateExpiredAlerts() {
        List<SystemAlert> expired = systemAlertRepository.findExpiredActive(LocalDateTime.now());
        for (SystemAlert sa : expired) {
            sa.setActive(false);
        }
        systemAlertRepository.saveAll(expired);
        return expired.size();
    }

    // PART 4: CSV Export
    public String exportCsv() {
        List<SystemAlert> alerts = systemAlertRepository.findAll();
        StringWriter sw = new StringWriter();
        try (PrintWriter pw = new PrintWriter(sw)) {
            pw.println("id,alertType,severity,region,title,active,expirationDate");
            for (SystemAlert a : alerts) {
                pw.printf("%d,%s,%s,%s,%s,%s,%s\n",
                        a.getId(),
                        safe(a.getAlertType()),
                        safe(a.getSeverity()),
                        safe(a.getRegion()),
                        safe(a.getTitle()),
                        String.valueOf(a.getActive()),
                        a.getExpirationDate() != null ? a.getExpirationDate().toString() : ""
                );
            }
        }
        return sw.toString();
    }

    private String safe(String s) {
        if (s == null) return "";
        return s.replace('\n', ' ').replace('\r', ' ').replace(',', ';');
    }
}
