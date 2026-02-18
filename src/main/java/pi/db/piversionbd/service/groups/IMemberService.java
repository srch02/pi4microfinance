package pi.db.piversionbd.service.groups;

import pi.db.piversionbd.entities.groups.Member;

import java.util.List;

public interface IMemberService {

    List<Member> getAllMembers();

    List<Member> getMembersByGroupId(Long groupId);

    Member getMemberById(Long id);

    Member createMember(Member member);

    Member updateMember(Long id, Member updated);

    void deleteMember(Long id);

    /** Resolve currentGroupId to Group and set it on the member (for create/update). */
    void resolveCurrentGroup(Member member, Long currentGroupId);
}
