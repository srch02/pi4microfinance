package pi.db.piversionbd.service.groups;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pi.db.piversionbd.entities.groups.Group;
import pi.db.piversionbd.entities.groups.Member;
import pi.db.piversionbd.entities.pre.PreRegistration;
import pi.db.piversionbd.exception.DuplicateCinException;
import pi.db.piversionbd.exception.ResourceNotFoundException;
import pi.db.piversionbd.repository.groups.GroupRepository;
import pi.db.piversionbd.repository.groups.MemberRepository;
import pi.db.piversionbd.repository.pre.PreRegistrationRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class MemberServiceImp implements IMemberService {

    private final MemberRepository memberRepository;
    private final GroupRepository groupRepository;
    private final PreRegistrationRepository preRegistrationRepository;

    @Override
    public List<Member> getAllMembers() {
        return memberRepository.findAll();
    }

    @Override
    public List<Member> getMembersByGroupId(Long groupId) {
        return memberRepository.findByCurrentGroup_Id(groupId);
    }

    @Override
    public Member getMemberById(Long id) {
        return memberRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found with id " + id));
    }

    @Override
    public Member createMember(Member member) {
        member.setId(null);
        member.setCurrentGroup(null); // Group is set only when member is added via membership
        if (member.getCinNumber() == null || member.getCinNumber().isBlank()) {
            throw new IllegalArgumentException("cinNumber is required");
        }
        // CIN must exist in PreRegistration (accepted in module 5). If not found, deny creation.
        PreRegistration pre = preRegistrationRepository.findByCinNumber(member.getCinNumber())
                .orElseThrow(() -> new ResourceNotFoundException("CIN number doesn't exist in PreRegistration: " + member.getCinNumber()));
        member.setPreRegistration(pre);
        if (member.getCinNumber() != null && memberRepository.existsByCinNumber(member.getCinNumber())) {
            throw new DuplicateCinException("CIN number already in use: " + member.getCinNumber());
        }
        return memberRepository.save(member);
    }

    @Override
    public Member updateMember(Long id, Member updated) {
        Member existing = getMemberById(id);
        // CIN is not updatable – it comes from pre-registration, is verified and admin-approved
        if (updated.getAge() != null) existing.setAge(updated.getAge());
        if (updated.getProfession() != null) existing.setProfession(updated.getProfession());
        if (updated.getRegion() != null) existing.setRegion(updated.getRegion());
        if (updated.getPersonalizedMonthlyPrice() != null) existing.setPersonalizedMonthlyPrice(updated.getPersonalizedMonthlyPrice());
        if (updated.getAdherenceScore() != null) existing.setAdherenceScore(updated.getAdherenceScore());
        existing.setCurrentGroup(updated.getCurrentGroup());
        return memberRepository.save(existing);
    }

    @Override
    public void deleteMember(Long id) {
        Member member = memberRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found with id " + id));

        // Detach member from current group (so group can remain if needed)
        if (member.getCurrentGroup() != null) {
            member.setCurrentGroup(null);
        }

        // Clear only the in-memory inverse link (PreRegistration.member). We do NOT update the PRE_REGISTRATIONS table.
        if (member.getPreRegistration() != null) {
            member.getPreRegistration().setMember(null);
            member.setPreRegistration(null);
        }

        // Clear groups that reference this member as creator (groups.created_by_member_id FK).
        for (Group group : groupRepository.findByCreator_Id(id)) {
            group.setCreator(null);
            groupRepository.save(group);
        }

        // Deletes only the MEMBERS row (and cascades to memberships, payments, etc.).
        memberRepository.delete(member);
    }

    @Override
    public void resolveCurrentGroup(Member member, Long currentGroupId) {
        if (currentGroupId == null) {
            member.setCurrentGroup(null);
            return;
        }
        Group group = groupRepository.findById(currentGroupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group not found with id " + currentGroupId));
        member.setCurrentGroup(group);
    }
}
