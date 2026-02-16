package pi.db.piversionbd.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pi.db.piversionbd.entities.groups.Member;
import pi.db.piversionbd.entities.health.HealthChallenge;
import pi.db.piversionbd.entities.health.MemberChallengeParticipation;
import pi.db.piversionbd.repositories.HealthChallengeRepository;
import pi.db.piversionbd.repositories.MemberChallengeParticipationRepository;
import pi.db.piversionbd.repositories.MemberRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class HealthChallengeServiceImpl implements HealthChallengeService {

    private final HealthChallengeRepository challengeRepository;
    private final MemberChallengeParticipationRepository participationRepository;
    private final MemberRepository memberRepository;

    @Override
    public HealthChallenge create(HealthChallenge challenge) {
        challenge.setActive(true);
        return challengeRepository.save(challenge);
    }

    @Override
    public List<HealthChallenge> getAll() {
        return challengeRepository.findAll();
    }

    @Override
    public void participate(Long challengeId, Long memberId) {

        HealthChallenge challenge = challengeRepository.findById(challengeId)
                .orElseThrow(() -> new RuntimeException("Challenge not found"));

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("Member not found"));

        boolean alreadyParticipating =
                participationRepository
                        .existsByChallenge_IdAndMember_Id(challengeId, memberId);

        if (alreadyParticipating) {
            throw new RuntimeException("Member already participating");
        }

        MemberChallengeParticipation participation =
                new MemberChallengeParticipation();

        participation.setChallenge(challenge);
        participation.setMember(member);

        participationRepository.save(participation);
    }
}
