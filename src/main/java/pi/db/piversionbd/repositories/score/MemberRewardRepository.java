package pi.db.piversionbd.repositories.score;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import pi.db.piversionbd.entities.score.MemberReward;
import pi.db.piversionbd.entities.score.RewardRedemptionStatus;

public interface MemberRewardRepository extends JpaRepository<MemberReward, Long> {

    Page<MemberReward> findByMemberIdOrderByCreatedAtDesc(Long memberId, Pageable pageable);

    Page<MemberReward> findByStatusOrderByCreatedAtDesc(RewardRedemptionStatus status, Pageable pageable);
}
