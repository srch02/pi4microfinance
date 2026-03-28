package pi.db.piversionbd.services;

import pi.db.piversionbd.entities.groups.Member;
import pi.db.piversionbd.repositories.MemberRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class MemberServiceImpl implements IMemberService {

    @Autowired
    private MemberRepository memberRepository;

    @Override
    public Member saveMember(Member member) {
        return memberRepository.save(member);
    }

    @Override
    public Optional<Member> getMemberById(Long id) {
        return memberRepository.findById(id);
    }

    @Override
    public List<Member> getAllMembers() {
        return memberRepository.findAll();
    }

    @Override
    public Member updateMember(Long id, Member member) {
        Optional<Member> existingMember = memberRepository.findById(id);
        if (existingMember.isPresent()) {
            Member m = existingMember.get();
            if (member.getCinNumber() != null) {
                m.setCinNumber(member.getCinNumber());
            }
            if (member.getEmail() != null) {
                m.setEmail(member.getEmail());
            }
            if (member.getPassword() != null) {
                m.setPassword(member.getPassword());
            }
            if (member.getPersonalizedMonthlyPrice() != null) {
                m.setPersonalizedMonthlyPrice(member.getPersonalizedMonthlyPrice());
            }
            if (member.getAdherenceScore() != null) {
                m.setAdherenceScore(member.getAdherenceScore());
            }
            if (member.getCurrentGroup() != null) {
                m.setCurrentGroup(member.getCurrentGroup());
            }
            return memberRepository.save(m);
        }
        return null;
    }

    @Override
    public void deleteMember(Long id) {
        memberRepository.deleteById(id);
    }

    @Override
    public Optional<Member> getMemberByEmail(String email) {
        return memberRepository.findByEmail(email);
    }

    @Override
    public Optional<Member> getMemberByCinNumber(String cinNumber) {
        return memberRepository.findByCinNumber(cinNumber);
    }
}

