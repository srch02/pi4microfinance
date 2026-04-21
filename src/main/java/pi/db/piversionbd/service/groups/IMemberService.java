package pi.db.piversionbd.service.groups;

import pi.db.piversionbd.entities.groups.Member;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface IMemberService {

    List<Member> getAllMembers();

    List<Member> getMembersByGroupId(Long groupId);

    Member getMemberById(Long id);

    /** Lookup by email (e.g. health / integrations). */
    Optional<Member> getMemberByEmail(String email);

    /** Lookup by CIN (e.g. health / integrations). */
    Optional<Member> getMemberByCinNumber(String cinNumber);

    Member createMember(Member member);

    Member updateMember(Long id, Member updated);

    /** Link a Telegram chat id to this member (for bot notifications). */
    Member updateTelegramChatId(Long memberId, String telegramChatId);

    void deleteMember(Long id);

    /** Resolve currentGroupId to Group and set it on the member (for create/update). */
    void resolveCurrentGroup(Member member, Long currentGroupId);

    /** Dashboard stats: totalMembers, newMembersToday (auth stats are on AdminUser). */
    Map<String, Long> dashboardStatsForMembers();
}
