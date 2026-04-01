package pi.db.piversionbd.service.groups;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pi.db.piversionbd.entities.groups.Membership;
import pi.db.piversionbd.entities.score.Claim;
import pi.db.piversionbd.entities.score.ClaimStatus;
import pi.db.piversionbd.repository.groups.MembershipRepository;

import java.math.BigDecimal;

/**
 * When a claim is approved, reduces the member's remaining annual coverage and consultation slots
 * for that membership (same member + group).
 */
@Service
@RequiredArgsConstructor
public class MembershipClaimConsumptionService {

    private static final Logger log = LoggerFactory.getLogger(MembershipClaimConsumptionService.class);

    private final MembershipRepository membershipRepository;

    /**
     * Applies consumption once per claim when status first becomes APPROVED_AUTO or APPROVED_MANUAL.
     */
    @Transactional
    public void consumeOnApproval(Claim claim, ClaimStatus statusBeforeDecision) {
        if (claim == null || claim.getMember() == null || claim.getGroup() == null) {
            return;
        }
        ClaimStatus current = claim.getStatus();
        if (current != ClaimStatus.APPROVED_AUTO && current != ClaimStatus.APPROVED_MANUAL) {
            return;
        }
        if (statusBeforeDecision == ClaimStatus.APPROVED_AUTO || statusBeforeDecision == ClaimStatus.APPROVED_MANUAL) {
            return;
        }
        BigDecimal amount = claim.getAmountApproved();
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("Membership consumption skipped: claim {} has no positive amountApproved", claim.getId());
            return;
        }

        Membership m = membershipRepository
                .findByMember_IdAndGroup_IdAndEndedAtIsNull(claim.getMember().getId(), claim.getGroup().getId())
                .orElse(null);
        if (m == null) {
            log.warn("Membership consumption skipped: no open membership for member {} group {}",
                    claim.getMember().getId(), claim.getGroup().getId());
            return;
        }

        float add = amount.floatValue();
        float prevUsed = m.getAnnualUsedAmount() == null ? 0f : m.getAnnualUsedAmount();
        m.setAnnualUsedAmount(prevUsed + add);

        int prevCons = m.getConsultationsUsed() == null ? 0 : m.getConsultationsUsed();
        m.setConsultationsUsed(prevCons + 1);

        membershipRepository.save(m);
    }
}
