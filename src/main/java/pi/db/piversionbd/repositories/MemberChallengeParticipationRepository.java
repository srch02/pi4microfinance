package pi.db.piversionbd.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pi.db.piversionbd.entities.health.MemberChallengeParticipation;

@Repository
public interface MemberChallengeParticipationRepository
        extends JpaRepository<MemberChallengeParticipation, Long> {

    boolean existsByChallenge_IdAndMember_Id(Long challengeId, Long memberId);
}
