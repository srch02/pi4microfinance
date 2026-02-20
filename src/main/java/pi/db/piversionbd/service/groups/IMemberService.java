package pi.db.piversionbd.service.groups;

import pi.db.piversionbd.entities.groups.Member;

import java.util.List;
import java.util.Map;

public interface IMemberService {

    List<Member> getAllMembers();

    List<Member> getMembersByGroupId(Long groupId);

    Member getMemberById(Long id);

    Member createMember(Member member);

    Member updateMember(Long id, Member updated);

    void deleteMember(Long id);

    /** Resolve currentGroupId to Group and set it on the member (for create/update). */
    void resolveCurrentGroup(Member member, Long currentGroupId);

    /** Register member with email + password (auth). CIN optional; generated if blank. */
    Member register(String email, String rawPassword, String cinNumber);

    /** Login with email + password; updates failed attempts and lastLogin. */
    Member login(String email, String rawPassword);

    /** Reset password and send by email. */
    void resetPassword(String email);

    /** Dashboard stats: totalMembers, blockedMembers, lockedMembers, newMembersToday. */
    Map<String, Long> dashboardStatsForMembers();
}
