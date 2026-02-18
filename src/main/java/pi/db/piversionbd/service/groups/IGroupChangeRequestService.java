package pi.db.piversionbd.service.groups;

import pi.db.piversionbd.entities.groups.GroupChangeRequest;

import java.util.List;
import java.util.Optional;

public interface IGroupChangeRequestService {

    /**
     * List requests by status (e.g. PENDING for admin queue).
     */
    List<GroupChangeRequest> findByStatus(String status);

    /**
     * Get request by ID.
     */
    GroupChangeRequest getById(Long id);

    /**
     * Admin approves the request. Member can then retry "add to group" to complete the switch.
     */
    GroupChangeRequest approve(Long requestId);

    /**
     * Admin rejects the request.
     */
    GroupChangeRequest reject(Long requestId);

    /**
     * Create a PENDING group change request (member fromGroupId → toGroupId). Called when member
     * already has a membership in another group and tries to join toGroupId.
     */
    GroupChangeRequest createRequest(Long memberId, Long fromGroupId, Long toGroupId, String requestedPackageType);

    /**
     * Find an APPROVED request for this member from fromGroupId to toGroupId (to consume on retry).
     */
    Optional<GroupChangeRequest> findApprovedRequest(Long memberId, Long fromGroupId, Long toGroupId);

    /**
     * Mark request as COMPLETED after membership was created (consume the approval).
     */
    void markCompleted(GroupChangeRequest request);
}
