package pi.db.piversionbd.repository.score;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pi.db.piversionbd.entities.score.Claim;
import pi.db.piversionbd.entities.score.ClaimStatus;

import java.time.LocalDateTime;
import java.util.Optional;

public interface ClaimRepository extends JpaRepository<Claim, Long> {

    Optional<Claim> findByClaimNumber(String claimNumber);

    @EntityGraph(attributePaths = {"member", "group"})
    @Query("select c from Claim c where c.claimNumber = :claimNumber")
    Optional<Claim> findDetailsByClaimNumber(@Param("claimNumber") String claimNumber);

    boolean existsByClaimNumber(String claimNumber);

    Page<Claim> findByStatusOrderByCreatedAtDesc(ClaimStatus status, Pageable pageable);

    Page<Claim> findByMember_IdOrderByCreatedAtDesc(Long memberId, Pageable pageable);

    Page<Claim> findByMember_IdAndStatusOrderByCreatedAtDesc(Long memberId, ClaimStatus status, Pageable pageable);
    boolean existsByMember_IdAndGroup_IdAndCreatedAtBetween(Long memberId, Long groupId, LocalDateTime from, LocalDateTime to);

    @EntityGraph(attributePaths = {"member", "group", "claimScoring"})
    @Query("select c from Claim c where c.id = :id")
    Optional<Claim> findDetailsById(@Param("id") Long id);
}
