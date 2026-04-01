package pi.db.piversionbd.service.admin;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pi.db.piversionbd.entities.admin.AdminUser;
import pi.db.piversionbd.entities.groups.Member;
import pi.db.piversionbd.repository.admin.AdminUserRepository;
import pi.db.piversionbd.repository.groups.MemberRepository;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@RequiredArgsConstructor
public class AdminUserService {

    private static final Logger log = LoggerFactory.getLogger(AdminUserService.class);
    private static final int MAX_FAILED_ATTEMPTS = 6;

    private final AdminUserRepository adminUserRepository;
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final JavaMailSender mailSender;

    @Value("${spring.mail.from:}")
    private String mailFrom;

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );

    private static void validateEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email requis");
        }
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new IllegalArgumentException("Format d'email invalide");
        }
    }

    private static void validatePassword(String rawPassword) {
        if (rawPassword == null || rawPassword.isBlank()) {
            throw new IllegalArgumentException("Mot de passe requis");
        }
        if (rawPassword.length() < 8) {
            throw new IllegalArgumentException("Mot de passe trop court (min 8)");
        }
        boolean hasLetter = rawPassword.chars().anyMatch(Character::isLetter);
        boolean hasDigit = rawPassword.chars().anyMatch(Character::isDigit);
        if (!(hasLetter && hasDigit)) {
            throw new IllegalArgumentException("Mot de passe faible: inclure au moins une lettre et un chiffre");
        }
    }

    // 🔐 REGISTER avec rôle
    @Transactional
    public AdminUser registerWithRole(String username, String email, String rawPassword, String role) {
        username = normalizeUsername(username);
        email = normalizeEmail(email);
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username requis");
        }
        validateEmail(email);
        validatePassword(rawPassword);
        if (adminUserRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Username déjà utilisé");
        }
        if (adminUserRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email déjà utilisé");
        }
        AdminUser user = new AdminUser();
        user.setUsername(username);
        user.setEmail(email);
        user.setEnabled(true);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setRole(role);
        AdminUser saved = adminUserRepository.save(user);
        sendWelcomeEmail(saved);
        return saved;
    }

    /** Register a member portal account and link it to an existing Member (Option B). */
    @Transactional
    public AdminUser registerMemberPortalAccount(String username, String email, String rawPassword, Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("Member introuvable: " + memberId));
        AdminUser user = registerWithRole(username, email, rawPassword, "MEMBER");
        user.setMember(member);
        return adminUserRepository.save(user);
    }

    // 🔐 REGISTER (ADMIN par défaut)
    @Transactional
    public AdminUser register(String username, String email, String rawPassword) {
        return registerWithRole(username, email, rawPassword, "ADMIN");
    }

    // 🔐 LOGIN
    public AdminUser login(String username, String rawPassword) {
        AdminUser user = adminUserRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable"));

        if (Boolean.FALSE.equals(user.getEnabled())) {
            throw new IllegalStateException("Compte désactivé");
        }
        if (user.getLockedAt() != null) {
            throw new IllegalStateException("Compte bloqué après tentatives échouées");
        }

        if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
            int attempts = user.getFailedLoginAttempts() == null ? 0 : user.getFailedLoginAttempts();
            attempts++;
            user.setFailedLoginAttempts(attempts);
            if (attempts >= MAX_FAILED_ATTEMPTS) {
                user.setLockedAt(LocalDateTime.now());
            }
            adminUserRepository.save(user);
            throw new IllegalArgumentException("Mot de passe invalide");
        }

        // Succès: reset les compteurs
        user.setFailedLoginAttempts(0);
        user.setLastLogin(LocalDateTime.now());
        adminUserRepository.save(user);
        return user;
    }

    @Transactional
    public void unlockAccount(Long userId) {
        adminUserRepository.findById(userId).ifPresent(user -> {
            user.setFailedLoginAttempts(0);
            user.setLockedAt(null);
            adminUserRepository.save(user);
        });
    }

    // CRUD
    public List<AdminUser> getAllAdminUsers() {
        return adminUserRepository.findAll();
    }

    public AdminUser createAdminUser(AdminUser adminUser) {
        // Simplifie la création via payload JSON (username, email, password)
        if (adminUser.getPassword() == null || adminUser.getPassword().isEmpty()) {
            throw new IllegalArgumentException("Mot de passe requis");
        }
        return register(adminUser.getUsername(), adminUser.getEmail(), adminUser.getPassword());
    }

    public Optional<AdminUser> getAdminUserById(Long id) {
        return adminUserRepository.findById(id);
    }

    public Optional<AdminUser> updateAdminUser(Long id, AdminUser details) {
        return adminUserRepository.findById(id).map(user -> {
            if (details.getUsername() != null && !details.getUsername().isBlank()) {
                user.setUsername(normalizeUsername(details.getUsername()));
            }
            if (details.getEmail() != null && !details.getEmail().isBlank()) {
                String normalizedEmail = normalizeEmail(details.getEmail());
                validateEmail(normalizedEmail);
                user.setEmail(normalizedEmail);
            }
            if (details.getRole() != null && !details.getRole().isBlank()) {
                user.setRole(details.getRole());
            }
            user.setPermissions(details.getPermissions());

            if (details.getPassword() != null && !details.getPassword().isEmpty()) {
                validatePassword(details.getPassword());
                user.setPassword(passwordEncoder.encode(details.getPassword()));
            }

            return adminUserRepository.save(user);
        });
    }

    public boolean deleteAdminUser(Long id) {
        return adminUserRepository.findById(id).map(user -> {
            adminUserRepository.delete(user);
            return true;
        }).orElse(false);
    }

    public Optional<AdminUser> getAdminUserByUsername(String username) {
        return adminUserRepository.findByUsername(username);
    }

    public Optional<AdminUser> getAdminUserByEmail(String email) {
        return adminUserRepository.findByEmail(email);
    }

    private void sendWelcomeEmail(AdminUser user) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            if (mailFrom != null && !mailFrom.isBlank()) {
                message.setFrom(mailFrom);
            }
            message.setTo(user.getEmail());
            message.setSubject("Bienvenue - Compte Admin créé");
            message.setText("Bonjour " + user.getUsername() + ",\n\nVotre compte administrateur a été créé avec succès.\n\nCordialement,");
            mailSender.send(message);
            log.info("Email de bienvenue envoyé à {}", user.getEmail());
        } catch (Exception e) {
            log.error("Échec d'envoi de l'email de bienvenue à {}: {}", user.getEmail(), e.getMessage(), e);
        }
    }

    private void sendResetPasswordEmail(AdminUser user, String plaintextPassword) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            if (mailFrom != null && !mailFrom.isBlank()) {
                message.setFrom(mailFrom);
            }
            message.setTo(user.getEmail());
            message.setSubject("Réinitialisation de mot de passe - Compte Admin");
            message.setText("Bonjour " + user.getUsername() + ",\n\n" +
                    "Votre mot de passe a été réinitialisé. Voici votre nouveau mot de passe : \n\n" +
                    plaintextPassword + "\n\n" +
                    "Par mesure de sécurité, connectez-vous et changez-le immédiatement depuis votre profil.\n\n" +
                    "Cordialement,");
            mailSender.send(message);
            log.info("Email de réinitialisation envoyé à {}", user.getEmail());
        } catch (Exception e) {
            log.error("Échec d'envoi de l'email de réinitialisation à {}: {}", user.getEmail(), e.getMessage(), e);
        }
    }

    /**
     * Réinitialise le mot de passe pour l'email donné, génère un nouveau mot de passe,
     * l'encode et notifie l'utilisateur par email.
     */
    @Transactional
    public void resetPassword(String email) {
        AdminUser user = adminUserRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Email introuvable"));

        String newPassword = generateSecurePassword();
        user.setPassword(passwordEncoder.encode(newPassword));
        adminUserRepository.save(user);
        sendResetPasswordEmail(user, newPassword);
    }

    private String generateSecurePassword() {
        // 16 bytes aléatoires, encodés en Base64 URL-safe -> ~22 caractères
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[16];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public List<AdminUser> searchByNameAndRole(String namePart, String role) {
        boolean hasName = namePart != null && !namePart.isBlank();
        boolean hasRole = role != null && !role.isBlank();
        if (!hasName && !hasRole) {
            return adminUserRepository.findAll();
        }
        if (hasName && !hasRole) {
            return adminUserRepository.findByUsernameContainingIgnoreCase(namePart);
        }
        if (!hasName && hasRole) {
            return adminUserRepository.findByRoleIgnoreCase(role);
        }
        // les deux présents: intersection
        List<AdminUser> byName = adminUserRepository.findByUsernameContainingIgnoreCase(namePart);
        List<AdminUser> byRole = adminUserRepository.findByRoleIgnoreCase(role);
        byName.retainAll(byRole);
        return byName;
    }

    public Map<String, Long> dashboardStats() {
        Map<String, Long> stats = new HashMap<>();
        long total = adminUserRepository.count();
        long totalAdmins = adminUserRepository.countByRoleIgnoreCase("ADMIN");
        long totalMembers = adminUserRepository.countByRoleIgnoreCase("MEMBER");
        long blocked = adminUserRepository.countByEnabledFalse();
        long locked = adminUserRepository.countByLockedAtNotNull();
        LocalDate today = LocalDate.now();
        long newToday = adminUserRepository.countByCreatedAtBetween(today.atStartOfDay(), today.atTime(LocalTime.MAX));
        long insuredMembers = memberRepository.count();
        long linkedAccounts = adminUserRepository.countByMemberIsNotNull();

        stats.put("totalUsers", total);
        stats.put("totalAdmins", totalAdmins);
        stats.put("totalMembers", totalMembers);
        stats.put("blockedUsers", blocked);
        stats.put("lockedUsers", locked);
        stats.put("newUsersToday", newToday);
        stats.put("insuredMembers", insuredMembers);
        stats.put("portalAccounts", totalMembers);
        stats.put("linkedAccounts", linkedAccounts);
        return stats;
    }

    private static String normalizeUsername(String username) {
        return username == null ? null : username.trim();
    }
    private static String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }
}

