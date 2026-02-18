package pi.db.piversionbd.repository.groups;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pi.db.piversionbd.entities.groups.GroupChangeRequest;

import java.util.List;
import java.util.Optional;

@Repository
public interface GroupChangeRequestRepository extends JpaRepository<GroupChangeRequest, Long> {

    /** For admin: list requests by status (e.g. PENDING). */
    List<GroupChangeRequest> findByStatusOrderByCreatedAtDesc(String status);

    /** Find an approved request for this member to join this group (to be consumed on retry). */
    Optional<GroupChangeRequest> findByMember_IdAndFromGroup_IdAndToGroup_IdAndStatus(
            Long memberId, Long fromGroupId, Long toGroupId, String status);

    /** Check if member already has a pending request to the same to_group (avoid duplicates). */
    boolean existsByMember_IdAndToGroup_IdAndStatus(Long memberId, Long toGroupId, String status);
}
