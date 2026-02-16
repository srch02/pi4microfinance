package pi.db.piversionbd.repositories.score;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pi.db.piversionbd.entities.score.Claim;
import pi.db.piversionbd.entities.score.ClaimStatus;

import java.util.Optional;

public interface ClaimRepository extends JpaRepository<Claim, Long> {

    Optional<Claim> findByClaimNumber(String claimNumber);

    boolean existsByClaimNumber(String claimNumber);

    Page<Claim> findByStatusOrderByCreatedAtDesc(ClaimStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"member", "group", "claimScoring"})
    @Query("select c from Claim c where c.id = :id")
    Optional<Claim> findDetailsById(@Param("id") Long id);
}
