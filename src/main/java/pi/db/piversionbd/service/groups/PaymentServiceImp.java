package pi.db.piversionbd.service.groups;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pi.db.piversionbd.entities.admin.SystemAlert;
import pi.db.piversionbd.entities.groups.Group;
import pi.db.piversionbd.entities.groups.GroupPool;
import pi.db.piversionbd.entities.groups.Membership;
import pi.db.piversionbd.entities.groups.Payment;
import pi.db.piversionbd.exception.ResourceNotFoundException;
import pi.db.piversionbd.repository.admin.SystemAlertRepository;
import pi.db.piversionbd.repository.groups.GroupPoolRepository;
import pi.db.piversionbd.repository.groups.PaymentRepository;
import pi.db.piversionbd.repository.groups.MembershipRepository;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class PaymentServiceImp implements IPaymentService {

    private final PaymentRepository paymentRepository;
    private final GroupPoolRepository groupPoolRepository;
    private final MembershipRepository membershipRepository;
    private final IMembershipService membershipService;
    private final SystemAlertRepository systemAlertRepository;

    private static final float POOL_RATIO = 0.7f;
    private static final float PLATFORM_RATIO = 0.2f;
    private static final float NATIONAL_RATIO = 0.1f;

    @Override
    public Membership recordSuccessfulPayment(Long membershipId) {
        Membership membership = membershipRepository.findById(membershipId)
                .orElseThrow(() -> new ResourceNotFoundException("Membership not found with id " + membershipId));
        Float monthlyAmount = membership.getMonthlyAmount();
        if (monthlyAmount == null || monthlyAmount <= 0) {
            throw new IllegalArgumentException("Membership has no monthly amount set (check package and member prices)");
        }
        float amount = monthlyAmount;
        float poolAllocation = amount * POOL_RATIO;
        float platformFee = amount * PLATFORM_RATIO;
        float nationalFund = amount * NATIONAL_RATIO;

        // Create payment record
        Payment payment = new Payment();
        payment.setMember(membership.getMember());
        payment.setGroup(membership.getGroup());
        payment.setAmount(amount);
        payment.setPoolAllocation(poolAllocation);
        payment.setPlatformFee(platformFee);
        payment.setNationalFund(nationalFund);
        payment = paymentRepository.save(payment);

        // Update group pool: get or create pool for this group
        Group group = membership.getGroup();
        GroupPool pool = groupPoolRepository.findByGroup_Id(group.getId())
                .orElseGet(() -> {
                    GroupPool p = new GroupPool();
                    p.setGroup(group);
                    p.setPoolBalance(0f);
                    p.setTotalContributions(0f);
                    p.setTotalPaidOut(0f);
                    return groupPoolRepository.save(p);
                });
        float addToPool = payment.getPoolAllocation();
        pool.setPoolBalance((pool.getPoolBalance() != null ? pool.getPoolBalance() : 0f) + addToPool);
        pool.setTotalContributions((pool.getTotalContributions() != null ? pool.getTotalContributions() : 0f) + addToPool);
        pool.setUpdatedAt(Instant.now());
        groupPoolRepository.save(pool);
        notifyLowPoolIfNeeded(pool, group);

        // Set membership to active
        Membership updated = membershipService.updateMembershipStatus(membershipId, Membership.STATUS_ACTIVE);

        return updated;
    }

    @Override
    public Payment processMonthlyPayment(Long memberId, Long groupId) {
        if (memberId == null || groupId == null) {
            throw new IllegalArgumentException("memberId and groupId are required");
        }
        Membership membership = membershipRepository
                .findByMember_IdAndGroup_IdAndEndedAtIsNull(memberId, groupId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No active membership found for member " + memberId + " in group " + groupId));
        Float monthlyAmount = membership.getMonthlyAmount();
        if (monthlyAmount == null || monthlyAmount <= 0) {
            throw new IllegalArgumentException("Membership has no monthly amount set (check package and member prices)");
        }
        float amount = monthlyAmount;
        float poolAllocation = amount * POOL_RATIO;
        float platformFee = amount * PLATFORM_RATIO;
        float nationalFund = amount * NATIONAL_RATIO;

        Payment payment = new Payment();
        payment.setMember(membership.getMember());
        payment.setGroup(membership.getGroup());
        payment.setAmount(amount);
        payment.setPoolAllocation(poolAllocation);
        payment.setPlatformFee(platformFee);
        payment.setNationalFund(nationalFund);
        payment = paymentRepository.save(payment);

        Group group = membership.getGroup();
        GroupPool pool = groupPoolRepository.findByGroup_Id(group.getId())
                .orElseGet(() -> {
                    GroupPool p = new GroupPool();
                    p.setGroup(group);
                    p.setPoolBalance(0f);
                    p.setTotalContributions(0f);
                    p.setTotalPaidOut(0f);
                    return groupPoolRepository.save(p);
                });
        pool.setPoolBalance((pool.getPoolBalance() != null ? pool.getPoolBalance() : 0f) + poolAllocation);
        pool.setTotalContributions((pool.getTotalContributions() != null ? pool.getTotalContributions() : 0f) + poolAllocation);
        pool.setUpdatedAt(Instant.now());
        groupPoolRepository.save(pool);
        notifyLowPoolIfNeeded(pool, group);

        if (Membership.STATUS_PENDING.equals(membership.getStatus())) {
            membershipService.updateMembershipStatus(membership.getId(), Membership.STATUS_ACTIVE);
        }
        return payment;
    }

    /** Create a system alert when the group pool is low (≤20% of contributions). At most one active LOW_POOL alert per group. */
    private void notifyLowPoolIfNeeded(GroupPool pool, Group group) {
        if (group == null || pool == null || !pool.isLowBalance()) {
            return;
        }
        Long groupId = group.getId();
        if (groupId == null) return;
        boolean alreadyAlerted = systemAlertRepository
                .findByAlertTypeAndSourceEntityTypeAndSourceEntityIdAndActive(
                        "LOW_POOL", "GROUP", groupId, true)
                .isPresent();
        if (alreadyAlerted) {
            return;
        }
        float balance = pool.getPoolBalance() != null ? pool.getPoolBalance() : 0f;
        float contributions = pool.getTotalContributions() != null ? pool.getTotalContributions() : 0f;
        SystemAlert alert = new SystemAlert();
        alert.setAlertType("LOW_POOL");
        alert.setSeverity("high");
        alert.setRegion(group.getRegion());
        alert.setTitle("Group solidarity pool low");
        alert.setMessage(String.format("Group \"%s\" (id=%d) has a low pool balance: %.2f DT (total contributions: %.2f DT). Consider alerting the group or reviewing claims.",
                group.getName() != null ? group.getName() : "—", groupId, balance, contributions));
        alert.setActive(true);
        alert.setSourceEntityType("GROUP");
        alert.setSourceEntityId(groupId);
        systemAlertRepository.save(alert);
    }

    @Override
    public List<Payment> getPaymentHistory(Long memberId, Long groupId) {
        if (memberId == null) {
            throw new IllegalArgumentException("memberId is required");
        }
        if (groupId != null) {
            return paymentRepository.findByMember_IdAndGroup_IdOrderByIdDesc(memberId, groupId);
        }
        return paymentRepository.findByMember_IdOrderByIdDesc(memberId);
    }
}
