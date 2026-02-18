package pi.db.piversionbd.repositories.score;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import pi.db.piversionbd.entities.score.AdherenceTracking;

public interface AdherenceTrackingRepository extends JpaRepository<AdherenceTracking, Long> {

    Page<AdherenceTracking> findByMemberIdOrderByCreatedAtDesc(Long memberId, Pageable pageable);

    Page<AdherenceTracking> findByRelatedClaimIdOrderByCreatedAtDesc(Long claimId, Pageable pageable);
}
