package pi.db.piversionbd.repository.score;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pi.db.piversionbd.entities.score.ClaimScoring;

import java.util.Optional;

public interface ClaimScoringRepository extends JpaRepository<ClaimScoring, Long> {

    Optional<ClaimScoring> findByClaimId(Long claimId);

    boolean existsByClaimId(Long claimId);

    @Modifying
    @Query("DELETE FROM ClaimScoring cs WHERE cs.claim.id = :claimId")
    void deleteByClaimId(@Param("claimId") Long claimId);
}
