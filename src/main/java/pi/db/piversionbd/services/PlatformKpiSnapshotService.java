package pi.db.piversionbd.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pi.db.piversionbd.entities.admin.PlatformKpiSnapshot;
import pi.db.piversionbd.repositories.PlatformKpiSnapshotRepository;

import java.util.List;
import java.util.Optional;

@Service
public class PlatformKpiSnapshotService {

    @Autowired
    private PlatformKpiSnapshotRepository platformKpiSnapshotRepository;

    // ✅ CREATE
    public PlatformKpiSnapshot createSnapshot(PlatformKpiSnapshot snapshot) {
        return platformKpiSnapshotRepository.save(snapshot);
    }

    // ✅ READ ALL
    public List<PlatformKpiSnapshot> getAllSnapshots() {
        return platformKpiSnapshotRepository.findAll();
    }

    // ✅ READ BY ID
    public Optional<PlatformKpiSnapshot> getSnapshotById(Long id) {
        return platformKpiSnapshotRepository.findById(id);
    }

    // ✅ UPDATE
    public PlatformKpiSnapshot updateSnapshot(Long id, PlatformKpiSnapshot snapshotDetails) {

        Optional<PlatformKpiSnapshot> optionalSnapshot =
                platformKpiSnapshotRepository.findById(id);

        if (optionalSnapshot.isEmpty()) {
            return null;
        }

        PlatformKpiSnapshot snapshot = optionalSnapshot.get();

        snapshot.setMetricType(snapshotDetails.getMetricType());
        snapshot.setGroup(snapshotDetails.getGroup());
        snapshot.setPeriodStart(snapshotDetails.getPeriodStart());
        snapshot.setPeriodEnd(snapshotDetails.getPeriodEnd());
        snapshot.setMetrics(snapshotDetails.getMetrics());

        return platformKpiSnapshotRepository.save(snapshot);
    }

    // ✅ DELETE
    public boolean deleteSnapshot(Long id) {

        if (!platformKpiSnapshotRepository.existsById(id)) {
            return false;
        }

        platformKpiSnapshotRepository.deleteById(id);
        return true;
    }
}
