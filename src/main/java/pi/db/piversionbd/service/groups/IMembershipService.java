package pi.db.piversionbd.service.groups;

import pi.db.piversionbd.entities.groups.Group;
import pi.db.piversionbd.entities.groups.GroupChangeRequest;
import pi.db.piversionbd.entities.groups.Membership;

import java.util.List;

public interface IMembershipService {

    /**
     * Result of "add member to group" or "join by invite": either a membership was created
     * or a group-change request was created (member already in another group; admin must approve).
     */
    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    final class AddMembershipResult {
        private Membership membership;
        private GroupChangeRequest groupChangeRequest;

        public static AddMembershipResult membership(Membership m) {
            return new AddMembershipResult(m, null);
        }

        public static AddMembershipResult groupChangeRequest(GroupChangeRequest r) {
            return new AddMembershipResult(null, r);
        }
    }

    /**
     * Add a member to a group: create a membership, or create a group-change request if member
     * is already in another group (admin must approve; then member retries to complete).
     */
    AddMembershipResult addMemberToGroup(Long groupId, Long memberId, String packageType);

    /**
     * Join a PRIVATE group using its invite code (QR). Same as addMemberToGroup regarding
     * group-change request when member is already in another group.
     */
    AddMembershipResult addMemberToGroupByInviteCode(String inviteCode, Long memberId, String packageType);

    /**
     * End membership (soft delete). Caller is responsible for admin/self check.
     */
    void removeMemberFromGroup(Long groupId, Long memberId);

    /**
     * Groups the member currently belongs to (active memberships only; usually one).
     */
    List<Group> getGroupsForMember(Long memberId);

    /**
     * All memberships for a member (any status). Use for "groups and payments after approved" view.
     */
    List<Membership> getMembershipsForMember(Long memberId);

    /**
     * Update membership status (e.g. pending → active on first successful payment). Valid: pending, active, suspended, cancelled.
     */
    Membership updateMembershipStatus(Long membershipId, String status);

    /**
     * Get a membership by its ID (used for alerts / dashboards).
     */
    Membership getMembershipById(Long membershipId);
}
