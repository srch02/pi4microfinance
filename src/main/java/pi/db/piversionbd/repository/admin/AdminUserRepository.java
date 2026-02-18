package pi.db.piversionbd.repository.admin;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pi.db.piversionbd.entities.admin.AdminUser;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AdminUserRepository extends JpaRepository<AdminUser, Long> {
    Optional<AdminUser> findByUsername(String username);
    Optional<AdminUser> findByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);

    // Recherche et filtrage
    List<AdminUser> findByUsernameContainingIgnoreCase(String username);
    List<AdminUser> findByRoleIgnoreCase(String role);
    List<AdminUser> findByEnabled(Boolean enabled);

    // Méthodes de comptage pour le tableau de bord
    long count();
    long countByRoleIgnoreCase(String role);
    long countByEnabledFalse();
    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
    long countByLockedAtNotNull();
}

