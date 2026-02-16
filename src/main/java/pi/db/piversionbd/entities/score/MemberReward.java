package pi.db.piversionbd.entities.score;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import pi.db.piversionbd.entities.groups.Member;

import java.time.LocalDateTime;

@Entity
@Table(name = "MEMBER_REWARDS")
@Data
public class MemberReward {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "member_id")
    private Member member;

    @ManyToOne
    @JoinColumn(name = "reward_id")
    private RewardCatalogItem reward;

    @Column(name = "points_spent", nullable = false)
    private Integer pointsSpent;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private RewardRedemptionStatus status = RewardRedemptionStatus.PENDING;

    @Column(name = "valid_until")
    private LocalDateTime validUntil;

    @Column(name = "redeemed_at")
    private LocalDateTime redeemedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
