package pi.db.piversionbd.service.groups;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pi.db.piversionbd.entities.groups.Group;
import pi.db.piversionbd.entities.groups.Member;
import pi.db.piversionbd.entities.groups.PackageType;
import pi.db.piversionbd.entities.pre.MedicalHistory;
import pi.db.piversionbd.entities.pre.PreRegistration;
import pi.db.piversionbd.entities.pre.PreRegistrationStatus;
import pi.db.piversionbd.entities.pre.RiskAssessment;
import pi.db.piversionbd.exception.DuplicateCinException;
import pi.db.piversionbd.exception.ResourceNotFoundException;
import pi.db.piversionbd.repository.pre.RiskAssessmentRepository;
import pi.db.piversionbd.repository.groups.GroupRepository;
import pi.db.piversionbd.repository.groups.MemberRepository;
import pi.db.piversionbd.repository.groups.MembershipRepository;
import pi.db.piversionbd.repository.groups.PaymentRepository;
import pi.db.piversionbd.repository.pre.PreRegistrationRepository;
import pi.db.piversionbd.repository.admin.AdminUserRepository;
import pi.db.piversionbd.service.score.AdherenceTrackingService;
import pi.db.piversionbd.service.hedera.HederaContractService;

import pi.db.piversionbd.service.hedera.SolidariHealthContractService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Transactional
public class MemberServiceImp implements IMemberService {

    private static final Logger log = LoggerFactory.getLogger(MemberServiceImp.class);
    private static final float MAX_MONTHLY_PRICE = 70.0f;

    private final MemberRepository memberRepository;
    private final MembershipRepository membershipRepository;
    private final PaymentRepository paymentRepository;
    private final GroupRepository groupRepository;
    private final PreRegistrationRepository preRegistrationRepository;
    private final RiskAssessmentRepository riskAssessmentRepository;
    private final AdherenceTrackingService adherenceTrackingService;
    private final HederaContractService hederaContractService;
    private final SolidariHealthContractService solidariHealthContractService;
    private final AdminUserRepository adminUserRepository;

    @Override
    public List<Member> getAllMembers() {
        List<Member> members = memberRepository.findAll();
        members.forEach(this::resolveAdherenceScoreFromScore);
        return members;
    }

    @Override
    public List<Member> getMembersByGroupId(Long groupId) {
        List<Member> members = memberRepository.findByCurrentGroup_Id(groupId);
        members.forEach(this::resolveAdherenceScoreFromScore);
        return members;
    }

    @Override
    public Member getMemberById(Long id) {
        Member member = memberRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found with id " + id));
        resolveAdherenceScoreFromScore(member);
        return member;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Member> getMemberByEmail(String email) {
        if (email == null || email.isBlank()) {
            return Optional.empty();
        }
        return memberRepository.findByEmail(email.trim()).map(m -> {
            resolveAdherenceScoreFromScore(m);
            return m;
        });
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Member> getMemberByCinNumber(String cinNumber) {
        if (cinNumber == null || cinNumber.isBlank()) {
            return Optional.empty();
        }
        return memberRepository.findByCinNumber(cinNumber.trim()).map(m -> {
            resolveAdherenceScoreFromScore(m);
            return m;
        });
    }

    @Override
    public Member createMember(Member member) {
        member.setId(null);
        member.setCurrentGroup(null); // Group is set only when member is added via membership
        if (member.getCinNumber() == null || member.getCinNumber().isBlank()) {
            throw new IllegalArgumentException("cinNumber is required");
        }
        // CIN must exist in PreRegistration (module 5). For direct testing, auto-create if missing.
        PreRegistration pre = preRegistrationRepository.findByCinNumber(member.getCinNumber())
                .orElseGet(() -> {
                    PreRegistration newPre = new PreRegistration();
                    newPre.setCinNumber(member.getCinNumber());
                    newPre.setStatus(PreRegistrationStatus.ACCEPTED);
                    newPre.setCreatedAt(LocalDateTime.now());
                    return preRegistrationRepository.save(newPre);
                });

        // Pre-registration must be admin-approved (APPROVED/ACCEPTED) or already ACTIVATED after first payment.
        if (!PreRegistrationStatus.allowsMemberCreation(pre.getStatus())) {
            throw new IllegalArgumentException(
                    "Member creation requires pre-registration APPROVED, ACCEPTED, or ACTIVATED. Current status: "
                            + pre.getStatus());
        }

        // Safety: avoid creating a duplicate member for the same approved pre-registration.
        if (pre.getMember() != null) {
            throw new DuplicateCinException("This CIN is already linked to a member (pre-registration already consumed).");
        }

        member.setPreRegistration(pre);
        if (member.getCinNumber() != null && memberRepository.existsByCinNumber(member.getCinNumber())) {
            throw new DuplicateCinException("CIN number already in use: " + member.getCinNumber());
        }
        // Personalized monthly base from pré-inscription risk (not exposed as “pricing” on the pre API).
        // Legacy rows may still have calculatedPrice; prefer riskCoefficient when present.
        float basePrice = riskAssessmentRepository.findByPreRegistration_Id(pre.getId()).stream()
                .findFirst()
                .map(ra -> {
                    if (ra.getRiskCoefficient() != null) {
                        return Math.min(MAX_MONTHLY_PRICE,
                                Math.round(25.0f * ra.getRiskCoefficient() * 100f) / 100f);
                    }
                    if (ra.getCalculatedPrice() != null) {
                        return ra.getCalculatedPrice();
                    }
                    return 25.0f;
                })
                .orElse(25.0f);
        if (member.getPersonalizedMonthlyPrice() == null) {
            member.setPersonalizedMonthlyPrice(basePrice);
        }
        member.setPriceBasic(Math.min(MAX_MONTHLY_PRICE, basePrice));
        member.setPriceConfort(Math.min(MAX_MONTHLY_PRICE, basePrice * 1.3f));
        member.setPricePremium(Math.min(MAX_MONTHLY_PRICE, basePrice * 1.6f));
        float initialAdherence = computeInitialAdherenceScore(pre);
        member.setAdherenceScore(initialAdherence);
        if (member.getCreatedAt() == null) {
            member.setCreatedAt(LocalDateTime.now());
        }
        Member saved = memberRepository.save(member);
        createInitialAdherenceEvent(saved, initialAdherence);

        // Auto-link: if an AdminUser with the same email exists and is not yet linked, link it now
        if (saved.getEmail() != null && !saved.getEmail().isBlank()) {
            adminUserRepository.findByEmail(saved.getEmail()).ifPresent(adminUser -> {
                if (adminUser.getMember() == null) {
                    adminUser.setMember(saved);
                    adminUserRepository.save(adminUser);
                    log.info("Auto-linked AdminUser '{}' to new Member id={}", adminUser.getUsername(), saved.getId());
                }
            });
        }
        
        // Provision Hedera asynchronously to avoid blocking the HTTP request and causing timeouts
        CompletableFuture.runAsync(() -> {
            provisionHederaForNewMember(saved, pre);
        });
        
        resolveAdherenceScoreFromScore(saved);
        return saved;
    }

    /**
     * Wallet (EVM-style id), initial coins, topic audit + SolidariHealth policy at member creation.
     * First payment still idempotently skips if {@code blockchainContractHash} is already set.
     */
    private void provisionHederaForNewMember(Member member, PreRegistration pre) {
        if (member == null || member.getId() == null) {
            return;
        }
        try {
            if (member.getBlockchainContractHash() != null && !member.getBlockchainContractHash().isBlank()) {
                return;
            }
            PackageType packageType = PackageType.BASIC;
            float finalPrice = member.getPersonalizedMonthlyPrice() != null ? member.getPersonalizedMonthlyPrice() : 25.0f;

            String exclusionsNote = null;
            if (pre != null && pre.getId() != null) {
                exclusionsNote = riskAssessmentRepository.findByPreRegistration_Id(pre.getId()).stream()
                        .findFirst()
                        .map(RiskAssessment::getExclusions)
                        .orElse(null);
            }

            String evmAddress = SolidariHealthContractService.memberIdToEvmAddress(member.getId());
            member.setWalletAddress(evmAddress);
            float initialCoins = Math.round(finalPrice * 100f / 3f) / 100f;
            member.setCoinBalance(initialCoins);

            String topicHash = hederaContractService.deployContract(
                    member.getId(),
                    member.getCinNumber(),
                    packageType.name(),
                    member.getPriceBasic(),
                    member.getPriceConfort(),
                    member.getPricePremium(),
                    exclusionsNote);
            if (topicHash != null) {
                member.setBlockchainContractHash(topicHash);
            }

            long monthlyCents = Math.round(finalPrice * 100);
            long annualLimitDt = annualLimitDtForPackage(packageType);
            long annualCents = annualLimitDt * 100;
            String contractHash = solidariHealthContractService.createMemberPolicy(
                    member.getId(),
                    member.getCinNumber(),
                    packageType.name(),
                    monthlyCents,
                    annualCents);
            if (contractHash != null) {
                member.setBlockchainContractHash(contractHash);
            }

            memberRepository.save(member);
        } catch (Exception ex) {
            log.warn("Hedera provisioning failed for new member {}: {}", member.getId(), ex.getMessage());
        }
    }

    private static long annualLimitDtForPackage(PackageType packageType) {
        if (packageType == null) {
            return 1500;
        }
        return switch (packageType) {
            case BASIC -> 1500;
            case CONFORT -> 3000;
            case PREMIUM -> 6000;
        };
    }

    @Override
    public Member updateMember(Long id, Member updated) {
        Member existing = getMemberById(id);
        // CIN is not updatable – it comes from pre-registration, is verified and admin-approved
        if (updated.getAge() != null) existing.setAge(updated.getAge());
        if (updated.getProfession() != null) existing.setProfession(updated.getProfession());
        if (updated.getRegion() != null) existing.setRegion(updated.getRegion());
        if (updated.getEmail() != null) existing.setEmail(updated.getEmail());
        // personalizedMonthlyPrice and adherenceScore are read-only (from preinscription and score module)
        // enabled, failedLoginAttempts, lockedAt, lastLogin, createdAt are read-only (admin/system)
        existing.setCurrentGroup(updated.getCurrentGroup());
        return memberRepository.save(existing);
    }

    @Override
    public Member updateTelegramChatId(Long memberId, String telegramChatId) {
        Member existing = getMemberById(memberId);
        if (telegramChatId == null || telegramChatId.isBlank()) {
            throw new IllegalArgumentException("telegramChatId is required");
        }
        existing.setTelegramChatId(telegramChatId.trim());
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

        // Remove child rows first — lazy collections are often not loaded, so JPA cascade may not run.
        paymentRepository.deleteByMember_Id(id);
        membershipRepository.deleteByMember_Id(id);

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

    @Override
    @Transactional(readOnly = true)
    public Map<String, Long> dashboardStatsForMembers() {
        Map<String, Long> stats = new HashMap<>();
        stats.put("totalMembers", memberRepository.count());
        LocalDate today = LocalDate.now();
        long newToday = memberRepository.countByCreatedAtBetween(today.atStartOfDay(), today.atTime(LocalTime.MAX));
        stats.put("newMembersToday", newToday);
        return stats;
    }

    /** Fetches current adherence score from the score module and sets it on the member (automatic). */
    private void resolveAdherenceScoreFromScore(Member member) {
        if (member == null || member.getId() == null) return;
        Float score = adherenceTrackingService.getCurrentAdherenceScoreForMember(member.getId());
        if (score != null) {
            member.setAdherenceScore(score);
        }
    }

    /**
     * Initial dynamic adherence score from onboarding medical context:
     * - chronic indicators in medical declaration
     * - pre-registration fraud score
     * - risk coefficient
     */
    private float computeInitialAdherenceScore(PreRegistration pre) {
        float score = 80.0f; // neutral onboarding baseline

        String text = "";
        if (pre != null && pre.getMedicalHistories() != null && !pre.getMedicalHistories().isEmpty()) {
            MedicalHistory m = pre.getMedicalHistories().get(0);
            text = m != null && m.getExcludedConditionDetails() != null ? m.getExcludedConditionDetails() : "";
        }
        String normalized = text.toLowerCase(Locale.ROOT);
        int chronicHits = countKeywordHits(normalized,
                "chronic", "diabet", "hypertension", "asthma", "cancer", "renal", "cardia");
        if (chronicHits >= 1) score -= 12f;
        if (chronicHits >= 2) score -= 6f;
        if (chronicHits >= 3) score -= 5f;

        if (pre != null && pre.getFraudScore() != null) {
            float fraud = pre.getFraudScore();
            if (fraud >= 0.8f) score -= 20f;
            else if (fraud >= 0.6f) score -= 12f;
            else if (fraud >= 0.3f) score -= 6f;
        }

        float riskCoefficient = riskAssessmentRepository.findByPreRegistration_Id(pre != null ? pre.getId() : null)
                .stream()
                .findFirst()
                .map(pi.db.piversionbd.entities.pre.RiskAssessment::getRiskCoefficient)
                .orElse(1.0f);
        if (riskCoefficient > 1.2f) score -= 10f;
        else if (riskCoefficient > 1.0f) score -= 5f;
        else if (riskCoefficient < 0.95f) score += 3f;

        return Math.max(20f, Math.min(100f, score));
    }

    private static int countKeywordHits(String text, String... keywords) {
        if (text == null || text.isBlank()) return 0;
        int hits = 0;
        for (String k : keywords) {
            if (k != null && !k.isBlank() && text.contains(k)) {
                hits++;
            }
        }
        return hits;
    }

    private void createInitialAdherenceEvent(Member member, float initialAdherence) {
        if (member == null || member.getId() == null) return;
        try {
            adherenceTrackingService.createOnboardingBaseline(member.getId(), initialAdherence);
        } catch (Exception ex) {
            log.warn("Could not create onboarding adherence event for member {}: {}", member.getId(), ex.getMessage());
        }
    }
}
