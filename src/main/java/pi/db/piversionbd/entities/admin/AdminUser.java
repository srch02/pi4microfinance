package pi.db.piversionbd.entities.admin;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import pi.db.piversionbd.entities.groups.Member;

import java.time.LocalDateTime;
import jakarta.persistence.PrePersist;
import java.util.List;

@Entity
@Table(name = "ADMIN_USERS")
@Data
public class AdminUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(unique = true, nullable = false)
    private String email;

    private String role;

    @JsonIgnore
    @Column(nullable = false)
    private String password;

    private Boolean enabled = true;

    @Lob
    private String permissions;

    @Column(name = "last_login")
    private LocalDateTime lastLogin;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // Blocage après tentatives échouées
    @Column(name = "failed_login_attempts", nullable = false)
    private Integer failedLoginAttempts = 0;

    @Column(name = "locked_at")
    private LocalDateTime lockedAt;

    /** Optional link to insurance Member when this user is a member portal account. */
    @OneToOne
    @JoinColumn(name = "member_id", nullable = true)
    private Member member;

    @PrePersist
    public void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (failedLoginAttempts == null) {
            failedLoginAttempts = 0;
        }
    }

    @OneToMany(mappedBy = "assignedTo")
    @JsonIgnore
    private List<AdminReviewQueueItem> assignedTasks;
}
