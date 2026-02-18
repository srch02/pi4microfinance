package pi.db.piversionbd.services.score;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pi.db.piversionbd.entities.score.MemberReward;
import pi.db.piversionbd.entities.score.RewardRedemptionStatus;
import pi.db.piversionbd.exceptions.NotFoundException;
import pi.db.piversionbd.repositories.score.MemberRewardRepository;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class MemberRewardService {

    private final MemberRewardRepository memberRewardRepository;

    public MemberReward create(MemberReward memberReward) {
        if (memberReward.getMember() == null) {
            throw new IllegalArgumentException("member est obligatoire.");
        }
        if (memberReward.getReward() == null) {
            throw new IllegalArgumentException("reward est obligatoire.");
        }
        if (memberReward.getPointsSpent() == null || memberReward.getPointsSpent() <= 0) {
            throw new IllegalArgumentException("pointsSpent doit être > 0.");
        }

        if (memberReward.getStatus() == null) {
            memberReward.setStatus(RewardRedemptionStatus.PENDING);
        }

        return memberRewardRepository.save(memberReward);
    }

    @Transactional(readOnly = true)
    public MemberReward getById(Long id) {
        return memberRewardRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("MemberReward introuvable: " + id));
    }

    @Transactional(readOnly = true)
    public Page<MemberReward> getAll(Pageable pageable) {
        return memberRewardRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Page<MemberReward> getByMember(Long memberId, Pageable pageable) {
        return memberRewardRepository.findByMemberIdOrderByCreatedAtDesc(memberId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<MemberReward> getByStatus(RewardRedemptionStatus status, Pageable pageable) {
        return memberRewardRepository.findByStatusOrderByCreatedAtDesc(status, pageable);
    }

    public MemberReward update(Long id, MemberReward request) {
        MemberReward existing = getById(id);

        if (request.getStatus() != null) {
            existing.setStatus(request.getStatus());
        }
        if (request.getValidUntil() != null) {
            existing.setValidUntil(request.getValidUntil());
        }
        if (request.getPointsSpent() != null && request.getPointsSpent() > 0) {
            existing.setPointsSpent(request.getPointsSpent());
        }

        if (request.getStatus() == RewardRedemptionStatus.REDEEMED && existing.getRedeemedAt() == null) {
            existing.setRedeemedAt(LocalDateTime.now());
        }

        return memberRewardRepository.save(existing);
    }

    public void delete(Long id) {
        MemberReward existing = getById(id);
        memberRewardRepository.delete(existing);
    }
}
