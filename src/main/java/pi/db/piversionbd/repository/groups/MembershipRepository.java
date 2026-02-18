package pi.db.piversionbd.repository.groups;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pi.db.piversionbd.entities.groups.Membership;

import java.util.List;
import java.util.Optional;

@Repository
public interface MembershipRepository extends JpaRepository<Membership, Long> {

    /** Memberships that are not ended (pending or active). */
    List<Membership> findByMember_IdAndEndedAtIsNull(Long memberId);

    /** Non-cancelled membership for this member in this group, if any (pending or active). */
    Optional<Membership> findByMember_IdAndGroup_IdAndEndedAtIsNull(Long memberId, Long groupId);

    /** Active memberships only (paid; member can file claims). */
    List<Membership> findByMember_IdAndStatusAndEndedAtIsNull(Long memberId, String status);

    /** All memberships for a member (any status), order by id desc. */
    List<Membership> findByMember_IdOrderByIdDesc(Long memberId);
}
