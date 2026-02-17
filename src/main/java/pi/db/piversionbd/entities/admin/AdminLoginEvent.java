package pi.db.piversionbd.entities.admin;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "ADMIN_LOGIN_EVENTS")
@Data
public class AdminLoginEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_user_id")
    private AdminUser adminUser; // peut être null si utilisateur introuvable

    @Column(name = "username", nullable = false)
    private String username; // username tenté

    @Column(name = "success", nullable = false)
    private Boolean success;

    @Column(name = "reason")
    private String reason; // SUCCESS, USER_NOT_FOUND, DISABLED, LOCKED, INVALID_PASSWORD

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "user_agent", length = 512)
    private String userAgent;

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp = LocalDateTime.now();
}

