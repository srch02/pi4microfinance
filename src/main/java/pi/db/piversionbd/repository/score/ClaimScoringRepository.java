package pi.db.piversionbd.repository.score;

import org.springframework.data.jpa.repository.JpaRepository;
import pi.db.piversionbd.entities.score.ClaimScoring;

import java.util.Optional;

public interface ClaimScoringRepository extends JpaRepository<ClaimScoring, Long> {

    Optional<ClaimScoring> findByClaimId(Long claimId);

    void deleteByClaimId(Long claimId);
}