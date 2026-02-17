package pi.db.piversionbd.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import pi.db.piversionbd.entities.groups.Membership;

import java.util.Optional;

public interface MembershipRepository extends JpaRepository<Membership, Long> {
    Optional<Membership> findByMember_IdAndActiveTrue(Long memberId);

}
