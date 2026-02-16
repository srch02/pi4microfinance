package pi.db.piversionbd.services;

import pi.db.piversionbd.entities.health.HealthTrackingEntry;

import java.util.List;

public interface HealthTrackingEntryService {

    HealthTrackingEntry create(Long memberId, HealthTrackingEntry entry);

    List<HealthTrackingEntry> getByMember(Long memberId);

    void delete(Long id);
}

