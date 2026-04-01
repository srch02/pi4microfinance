package pi.db.piversionbd.repository.score;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pi.db.piversionbd.entities.score.AdherenceTracking;

import java.util.Optional;

public interface AdherenceTrackingRepository extends JpaRepository<AdherenceTracking, Long> {

    Page<AdherenceTracking> findByMemberIdOrderByCreatedAtDesc(Long memberId, Pageable pageable);

    Page<AdherenceTracking> findByRelatedClaimIdOrderByCreatedAtDesc(Long claimId, Pageable pageable);

    /** Latest adherence event for a member (to derive current score). */
    Optional<AdherenceTracking> findTop1ByMember_IdOrderByCreatedAtDesc(Long memberId);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE AdherenceTracking a SET a.relatedClaim = null WHERE a.relatedClaim.id = :claimId")
    void unlinkRelatedClaim(@Param("claimId") Long claimId);
}
