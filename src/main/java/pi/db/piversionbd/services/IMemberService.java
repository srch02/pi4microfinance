package pi.db.piversionbd.services;

import pi.db.piversionbd.entities.groups.Member;

import java.util.List;
import java.util.Optional;

public interface IMemberService {
    Member saveMember(Member member);
    Optional<Member> getMemberById(Long id);
    List<Member> getAllMembers();
    Member updateMember(Long id, Member member);
    void deleteMember(Long id);
    Optional<Member> getMemberByEmail(String email);
    Optional<Member> getMemberByCinNumber(String cinNumber);
}

