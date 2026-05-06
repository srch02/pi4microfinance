package pi.db.piversionbd.service.score;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pi.db.piversionbd.dto.score.ClaimRewardSummaryResponse;
import pi.db.piversionbd.dto.score.RewardBadgeResponse;
import pi.db.piversionbd.entities.groups.Member;
import pi.db.piversionbd.entities.score.AdherenceEventType;
import pi.db.piversionbd.entities.score.AdherenceTracking;
import pi.db.piversionbd.entities.score.Claim;
import pi.db.piversionbd.exception.ResourceNotFoundException;
import pi.db.piversionbd.repository.groups.MemberRepository;
import pi.db.piversionbd.repository.score.AdherenceTrackingRepository;
import pi.db.piversionbd.repository.score.ClaimRepository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ClaimRewardService {

    private final ClaimRepository claimRepository;
    private final MemberRepository memberRepository;
    private final AdherenceTrackingRepository adherenceTrackingRepository;

    public void handleClaimSubmitted(Claim claim) {
        if (claim == null || claim.getId() == null || claim.getMember() == null) {
            return;
        }

        Long claimId = claim.getId();
        Long memberId = claim.getMember().getId();

        if (memberId == null) {
            return;
        }

        if (!adherenceTrackingRepository.existsByRelatedClaim_IdAndEventType(
                claimId,
                AdherenceEventType.CLAIM_SUBMITTED
        )) {
            createTrackingEvent(
                    claim.getMember(),
                    claim,
                    AdherenceEventType.CLAIM_SUBMITTED,
                    BigDecimal.ZERO,
                    "Claim submitted and counted for reward tracking."
            );
        }

        long claimsCount = claimRepository.countByMember_Id(memberId);

        checkAndCreateMilestone(claim.getMember(), claim, claimsCount, 3, 10, AdherenceEventType.CLAIM_MILESTONE_3);
        checkAndCreateMilestone(claim.getMember(), claim, claimsCount, 5, 15, AdherenceEventType.CLAIM_MILESTONE_5);
        checkAndCreateMilestone(claim.getMember(), claim, claimsCount, 10, 20, AdherenceEventType.CLAIM_MILESTONE_10);
    }

    @Transactional(readOnly = true)
    public ClaimRewardSummaryResponse getSummary(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Member introuvable: " + memberId));

        long claimsCount = claimRepository.countByMember_Id(memberId);

        int currentDiscount = calculateDiscountPercent(claimsCount);

        Integer nextClaims = getNextMilestoneClaims(claimsCount);
        Integer nextDiscount = getNextMilestoneDiscount(claimsCount);
        Integer remaining = nextClaims == null ? null : Math.max(0, nextClaims - (int) claimsCount);

        List<RewardBadgeResponse> badges = buildBadges(claimsCount);

        return new ClaimRewardSummaryResponse(
                member.getId(),
                claimsCount,
                currentDiscount,
                nextClaims,
                nextDiscount,
                remaining,
                badges
        );
    }

    public int calculateDiscountPercent(long claimsCount) {
        if (claimsCount >= 10) {
            return 20;
        }

        if (claimsCount >= 5) {
            return 15;
        }

        if (claimsCount >= 3) {
            return 10;
        }

        return 0;
    }

    private void checkAndCreateMilestone(
            Member member,
            Claim claim,
            long claimsCount,
            int requiredClaims,
            int discountPercent,
            AdherenceEventType milestoneType
    ) {
        if (claimsCount < requiredClaims) {
            return;
        }

        boolean alreadyEarned = adherenceTrackingRepository.existsByMember_IdAndEventType(
                member.getId(),
                milestoneType
        );

        if (alreadyEarned) {
            return;
        }

        createTrackingEvent(
                member,
                claim,
                milestoneType,
                BigDecimal.ZERO,
                "Reward unlocked: " + requiredClaims + " submitted claims = " + discountPercent + "% discount."
        );
    }

    private void createTrackingEvent(
            Member member,
            Claim claim,
            AdherenceEventType eventType,
            BigDecimal scoreChange,
            String note
    ) {
        BigDecimal currentScore = member.getAdherenceScore() != null
                ? BigDecimal.valueOf(member.getAdherenceScore())
                : BigDecimal.ZERO;

        AdherenceTracking event = new AdherenceTracking();
        event.setMember(member);
        event.setRelatedClaim(claim);
        event.setEventType(eventType);
        event.setScoreChange(scoreChange);
        event.setCurrentScore(currentScore);
        event.setNote(note);

        adherenceTrackingRepository.save(event);
    }

    private List<RewardBadgeResponse> buildBadges(long claimsCount) {
        List<RewardBadgeResponse> badges = new ArrayList<>();

        badges.add(new RewardBadgeResponse(
                "CLAIMS_3",
                "3 Claims",
                "10% discount earned",
                claimsCount >= 3,
                10,
                3
        ));

        badges.add(new RewardBadgeResponse(
                "CLAIMS_5",
                "5 Claims",
                "15% discount earned",
                claimsCount >= 5,
                15,
                5
        ));

        badges.add(new RewardBadgeResponse(
                "CLAIMS_10",
                "10 Claims",
                "20% discount earned",
                claimsCount >= 10,
                20,
                10
        ));

        return badges;
    }

    private Integer getNextMilestoneClaims(long claimsCount) {
        if (claimsCount < 3) {
            return 3;
        }

        if (claimsCount < 5) {
            return 5;
        }

        if (claimsCount < 10) {
            return 10;
        }

        return null;
    }

    private Integer getNextMilestoneDiscount(long claimsCount) {
        if (claimsCount < 3) {
            return 10;
        }

        if (claimsCount < 5) {
            return 15;
        }

        if (claimsCount < 10) {
            return 20;
        }

        return null;
    }
}