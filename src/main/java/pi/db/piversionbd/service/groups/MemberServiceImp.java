package pi.db.piversionbd.service.groups;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pi.db.piversionbd.entities.groups.Group;
import pi.db.piversionbd.entities.groups.Member;
import pi.db.piversionbd.entities.pre.PreRegistration;
import pi.db.piversionbd.exception.DuplicateCinException;
import pi.db.piversionbd.exception.ResourceNotFoundException;
import pi.db.piversionbd.repository.pre.RiskAssessmentRepository;
import pi.db.piversionbd.repository.groups.GroupRepository;
import pi.db.piversionbd.repository.groups.MemberRepository;
import pi.db.piversionbd.repository.pre.PreRegistrationRepository;
import pi.db.piversionbd.service.score.AdherenceTrackingService;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class MemberServiceImp implements IMemberService {

    private static final Logger log = LoggerFactory.getLogger(MemberServiceImp.class);
    private static final float MAX_MONTHLY_PRICE = 70.0f;
    private static final int MAX_FAILED_ATTEMPTS = 3;

    private final MemberRepository memberRepository;
    private final GroupRepository groupRepository;
    private final PreRegistrationRepository preRegistrationRepository;
    private final RiskAssessmentRepository riskAssessmentRepository;
    private final AdherenceTrackingService adherenceTrackingService;
    private final PasswordEncoder passwordEncoder;
    private final JavaMailSender mailSender;

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
    public Member createMember(Member member) {
        member.setId(null);
        member.setCurrentGroup(null); // Group is set only when member is added via membership
        if (member.getCinNumber() == null || member.getCinNumber().isBlank()) {
            throw new IllegalArgumentException("cinNumber is required");
        }
        // CIN must exist in PreRegistration (module 5). If not found, deny creation.
        PreRegistration pre = preRegistrationRepository.findByCinNumber(member.getCinNumber())
                .orElseThrow(() -> new ResourceNotFoundException("CIN number doesn't exist in PreRegistration: " + member.getCinNumber()));
        member.setPreRegistration(pre);
        if (member.getCinNumber() != null && memberRepository.existsByCinNumber(member.getCinNumber())) {
            throw new DuplicateCinException("CIN number already in use: " + member.getCinNumber());
        }
        // Set prices from preinscription (RiskAssessment) so member can choose BASIC/CONFORT/PREMIUM when creating membership
        float basePrice = riskAssessmentRepository.findByPreRegistration_Id(pre.getId()).stream()
                .findFirst()
                .map(pi.db.piversionbd.entities.pre.RiskAssessment::getCalculatedPrice)
                .orElse(25.0f);
        if (member.getPersonalizedMonthlyPrice() == null) {
            member.setPersonalizedMonthlyPrice(basePrice);
        }
        member.setPriceBasic(Math.min(MAX_MONTHLY_PRICE, basePrice));
        member.setPriceConfort(Math.min(MAX_MONTHLY_PRICE, basePrice * 1.3f));
        member.setPricePremium(Math.min(MAX_MONTHLY_PRICE, basePrice * 1.6f));
        Member saved = memberRepository.save(member);
        resolveAdherenceScoreFromScore(saved);
        return saved;
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

        // Deletes only the MEMBERS row (and cascades to memberships, payments, etc.).
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
    @Transactional
    public Member register(String email, String rawPassword, String cinNumber) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email requis");
        }
        if (rawPassword == null || rawPassword.isBlank()) {
            throw new IllegalArgumentException("Mot de passe requis");
        }
        if (memberRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email déjà utilisé");
        }
        Member m = new Member();
        m.setEmail(email);
        m.setPassword(passwordEncoder.encode(rawPassword));
        m.setEnabled(true);
        m.setCreatedAt(LocalDateTime.now());
        String cin = (cinNumber != null && !cinNumber.isBlank()) ? cinNumber : generateCinNumber();
        m.setCinNumber(cin);
        return memberRepository.save(m);
    }

    @Override
    @Transactional
    public Member login(String email, String rawPassword) {
        Member m = memberRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable"));
        if (Boolean.FALSE.equals(m.getEnabled())) {
            throw new IllegalStateException("Compte désactivé");
        }
        if (m.getLockedAt() != null) {
            throw new IllegalStateException("Compte bloqué après tentatives échouées");
        }
        if (!passwordEncoder.matches(rawPassword, m.getPassword())) {
            int attempts = m.getFailedLoginAttempts() == null ? 0 : m.getFailedLoginAttempts();
            attempts++;
            m.setFailedLoginAttempts(attempts);
            if (attempts >= MAX_FAILED_ATTEMPTS) {
                m.setLockedAt(LocalDateTime.now());
            }
            memberRepository.save(m);
            throw new IllegalArgumentException("Mot de passe invalide");
        }
        m.setFailedLoginAttempts(0);
        m.setLastLogin(LocalDateTime.now());
        return memberRepository.save(m);
    }

    @Override
    @Transactional
    public void resetPassword(String email) {
        Member m = memberRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Email introuvable"));
        String newPassword = generateSecurePassword();
        m.setPassword(passwordEncoder.encode(newPassword));
        memberRepository.save(m);
        sendResetPasswordEmail(m, newPassword);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Long> dashboardStatsForMembers() {
        Map<String, Long> stats = new HashMap<>();
        stats.put("totalMembers", memberRepository.count());
        stats.put("blockedMembers", memberRepository.countByEnabledFalse());
        stats.put("lockedMembers", memberRepository.countByLockedAtNotNull());
        LocalDate today = LocalDate.now();
        long newToday = memberRepository.countByCreatedAtBetween(today.atStartOfDay(), today.atTime(LocalTime.MAX));
        stats.put("newMembersToday", newToday);
        return stats;
    }

    private static String generateSecurePassword() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[16];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String generateCinNumber() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[8];
        random.nextBytes(bytes);
        return "CIN-" + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private void sendResetPasswordEmail(Member m, String plaintextPassword) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(m.getEmail());
            message.setSubject("Réinitialisation de mot de passe - Compte Membre");
            message.setText("Bonjour,\n\nVotre mot de passe a été réinitialisé. Nouveau mot de passe:\n\n" + plaintextPassword + "\n\nChangez-le dès votre prochaine connexion.\n");
            mailSender.send(message);
            log.info("Email reset password envoyé à {}", m.getEmail());
        } catch (Exception e) {
            log.error("Échec d'envoi de l'email reset password à {}: {}", m.getEmail(), e.getMessage(), e);
        }
    }

    /** Fetches current adherence score from the score module and sets it on the member (automatic). */
    private void resolveAdherenceScoreFromScore(Member member) {
        if (member == null || member.getId() == null) return;
        Float score = adherenceTrackingService.getCurrentAdherenceScoreForMember(member.getId());
        if (score != null) {
            member.setAdherenceScore(score);
        }
    }
}
