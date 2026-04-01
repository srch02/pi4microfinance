package pi.db.piversionbd.service.groups;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pi.db.piversionbd.entities.admin.SystemAlert;
import pi.db.piversionbd.entities.pre.PreRegistration;
import pi.db.piversionbd.entities.pre.PreRegistrationStatus;
import pi.db.piversionbd.entities.pre.RiskAssessment;
import pi.db.piversionbd.entities.groups.Member;
import pi.db.piversionbd.entities.groups.Group;
import pi.db.piversionbd.entities.groups.GroupPool;
import pi.db.piversionbd.entities.groups.Membership;
import pi.db.piversionbd.entities.groups.PackageType;
import pi.db.piversionbd.entities.groups.Payment;
import pi.db.piversionbd.exception.ResourceNotFoundException;
import pi.db.piversionbd.repository.admin.SystemAlertRepository;
import pi.db.piversionbd.repository.groups.MemberRepository;
import pi.db.piversionbd.repository.groups.GroupPoolRepository;
import pi.db.piversionbd.repository.groups.PaymentRepository;
import pi.db.piversionbd.repository.groups.MembershipRepository;
import pi.db.piversionbd.repository.pre.PreRegistrationRepository;
import pi.db.piversionbd.repository.pre.RiskAssessmentRepository;
import pi.db.piversionbd.service.hedera.HederaContractService;
import pi.db.piversionbd.service.hedera.HederaPaymentService;
import pi.db.piversionbd.service.hedera.SolidariHealthContractService;
import pi.db.piversionbd.service.notifications.TelegramNotificationService;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class PaymentServiceImp implements IPaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentServiceImp.class);

    private final PaymentRepository paymentRepository;
    private final GroupPoolRepository groupPoolRepository;
    private final MembershipRepository membershipRepository;
    private final IMembershipService membershipService;
    private final SystemAlertRepository systemAlertRepository;
    private final HederaPaymentService hederaPaymentService;
    private final HederaContractService hederaContractService;
    private final SolidariHealthContractService solidariHealthContractService;
    private final MemberRepository memberRepository;
    private final PreRegistrationRepository preRegistrationRepository;
    private final RiskAssessmentRepository riskAssessmentRepository;
    private final TelegramNotificationService telegramNotificationService;

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

        boolean isFirstPayment = Membership.STATUS_PENDING.equals(membership.getStatus());
        if (isFirstPayment) {
            ensureMemberPolicyProvisioned(membership, amount);
        }

        // Create payment record
        Payment payment = new Payment();
        payment.setMember(membership.getMember());
        payment.setGroup(membership.getGroup());
        payment.setAmount(amount);
        payment.setPoolAllocation(poolAllocation);
        payment.setPlatformFee(platformFee);
        payment.setNationalFund(nationalFund);
        payment.setCreatedAt(LocalDateTime.now());
        payment = paymentRepository.save(payment);
        hederaPaymentService.recordPayment(payment);
        long dtCents = Math.round(amount * 100);
        solidariHealthContractService.recordMonthlyPayment(
                payment.getMember() != null ? payment.getMember().getId() : null, dtCents);
        paymentRepository.save(payment);

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

        if (isFirstPayment) {
            Member mem = membership.getMember() != null && membership.getMember().getId() != null
                    ? memberRepository.findById(membership.getMember().getId()).orElse(membership.getMember())
                    : null;
            if (mem != null) {
                try {
                    telegramNotificationService.sendWelcomeForNewMember(mem);
                } catch (Exception e) {
                    log.warn("Telegram welcome failed for member {}: {}", mem.getId(), e.getMessage());
                }
            }
        }

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

        boolean isFirstPayment = Membership.STATUS_PENDING.equals(membership.getStatus());
        if (isFirstPayment) {
            ensureMemberPolicyProvisioned(membership, amount);
        }

        Payment payment = new Payment();
        payment.setMember(membership.getMember());
        payment.setGroup(membership.getGroup());
        payment.setAmount(amount);
        payment.setPoolAllocation(poolAllocation);
        payment.setPlatformFee(platformFee);
        payment.setNationalFund(nationalFund);
        payment.setCreatedAt(LocalDateTime.now());
        payment = paymentRepository.save(payment);
        hederaPaymentService.recordPayment(payment);
        long dtCents = Math.round(amount * 100);
        solidariHealthContractService.recordMonthlyPayment(
                payment.getMember() != null ? payment.getMember().getId() : null, dtCents);
        paymentRepository.save(payment);

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
            if (isFirstPayment) {
                Member mem = memberRepository.findById(memberId).orElse(membership.getMember());
                if (mem != null) {
                    try {
                        telegramNotificationService.sendWelcomeForNewMember(mem);
                    } catch (Exception e) {
                        log.warn("Telegram welcome failed for member {}: {}", memberId, e.getMessage());
                    }
                }
            }
        }
        return payment;
    }

    /**
     * First payment (pending -> active):
     * - provisions Hedera member policy (contract metadata + on-chain policy)
     * - marks the linked pre-registration as ACTIVATED
     * - sends a deduped system alert for admin dashboards
     */
    private void ensureMemberPolicyProvisioned(Membership membership, float monthlyAmountDt) {
        if (membership == null || membership.getMember() == null) return;

        Member member = membership.getMember();
        PackageType pkg = membership.getPackageType() != null ? membership.getPackageType() : PackageType.BASIC;

        PreRegistration pre = member.getPreRegistration();

        // Hedera policy provisioning (idempotent)
        boolean policyAlreadyProvisioned = member.getBlockchainContractHash() != null && !member.getBlockchainContractHash().isBlank();
        if (!policyAlreadyProvisioned) {
            String evmAddress = SolidariHealthContractService.memberIdToEvmAddress(member.getId());
            member.setWalletAddress(evmAddress);

            float initialCoins = Math.round(monthlyAmountDt * 100f / 3f) / 100f; // 1 coin = 3 DT
            member.setCoinBalance(initialCoins);

            // Fetch exclusions note for Hedera metadata
            String exclusionsNote = null;
            if (pre != null && pre.getId() != null) {
                exclusionsNote = riskAssessmentRepository.findByPreRegistration_Id(pre.getId()).stream()
                        .findFirst()
                        .map(RiskAssessment::getExclusions)
                        .orElse(null);
            }

            // Topic audit (metadata)
            String topicHash = hederaContractService.deployContract(
                    member.getId(),
                    member.getCinNumber(),
                    pkg.name(),
                    member.getPriceBasic(),
                    member.getPriceConfort(),
                    member.getPricePremium(),
                    exclusionsNote);
            if (topicHash != null) {
                member.setBlockchainContractHash(topicHash);
            }

            // SolidariHealth contract: createMemberPolicy
            long monthlyCents = Math.round(monthlyAmountDt * 100);
            long annualLimitDt = getAnnualLimitDt(pkg);
            long annualCents = annualLimitDt * 100;

            String contractHash = solidariHealthContractService.createMemberPolicy(
                    member.getId(),
                    member.getCinNumber(),
                    pkg.name(),
                    monthlyCents,
                    annualCents);
            if (contractHash != null) {
                member.setBlockchainContractHash(contractHash);
            }

            memberRepository.save(member);
        }

        // Link to pre-registration lifecycle (idempotent)
        if (pre != null && pre.getId() != null && pre.getStatus() != null && pre.getStatus() != PreRegistrationStatus.ACTIVATED) {
            // Only the admin's APPROVED step allows member creation; we activate only after first payment.
            pre.setStatus(PreRegistrationStatus.ACTIVATED);
            preRegistrationRepository.save(pre);
            enqueueMemberActivatedSystemAlert(pre, member);
        }
    }

    private long getAnnualLimitDt(PackageType packageType) {
        if (packageType == null) return 1500;
        return switch (packageType) {
            case BASIC -> 1500;
            case CONFORT -> 3000;
            case PREMIUM -> 6000;
        };
    }

    /** System alert for admin dashboards when a pre-registration becomes an active member (deduped). */
    private void enqueueMemberActivatedSystemAlert(PreRegistration pre, Member member) {
        if (pre == null || pre.getId() == null) return;

        boolean exists = systemAlertRepository
                .findByAlertTypeAndSourceEntityTypeAndSourceEntityIdAndActive(
                        "MEMBER_ACTIVATED", "PRE_REGISTRATION", pre.getId(), true)
                .isPresent();
        if (exists) return;

        SystemAlert alert = new SystemAlert();
        alert.setAlertType("MEMBER_ACTIVATED");
        alert.setSeverity("low");
        alert.setRegion(null);
        alert.setTitle("Member activated");
        alert.setMessage(String.format(
                "Pre-registration id=%d (CIN=%s) has been activated as member id=%s.",
                pre.getId(),
                pre.getCinNumber() != null ? pre.getCinNumber() : "—",
                member != null && member.getId() != null ? member.getId().toString() : "—"
        ));
        alert.setActive(true);
        alert.setSourceEntityType("PRE_REGISTRATION");
        alert.setSourceEntityId(pre.getId());
        systemAlertRepository.save(alert);
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
            return paymentRepository.findByMember_IdAndGroup_IdOrderByCreatedAtDesc(memberId, groupId);
        }
        return paymentRepository.findByMember_IdOrderByCreatedAtDesc(memberId);
    }
}
