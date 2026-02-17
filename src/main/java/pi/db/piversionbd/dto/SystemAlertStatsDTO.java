package pi.db.piversionbd.dto;

import java.util.Map;

public class SystemAlertStatsDTO {
    public long totalAlerts;
    public long activeAlerts;
    public long inactiveAlerts;
    public long criticalAlerts;
    public Map<String, Long> alertsBySeverity;
    public Map<String, Long> alertsByRegion;
}

