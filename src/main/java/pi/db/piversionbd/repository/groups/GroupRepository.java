package pi.db.piversionbd.repository.groups;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pi.db.piversionbd.entities.groups.Group;

import java.util.List;
import java.util.Optional;

@Repository
public interface GroupRepository extends JpaRepository<Group, Long> {

    List<Group> findByType(String type);

    Optional<Group> findByInviteCode(String inviteCode);

    boolean existsByInviteCode(String inviteCode);

    /** Groups created by this member (so we can clear created_by_member_id before deleting the member). */
    List<Group> findByCreator_Id(Long memberId);
}

