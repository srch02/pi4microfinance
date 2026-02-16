package pi.db.piversionbd.entities.health;

import jakarta.persistence.*;
import lombok.Data;
import pi.db.piversionbd.entities.groups.Member;

import java.time.LocalDateTime;

@Entity
@Table(name = "MEMBER_CHALLENGE_PARTICIPATION")
@Data
public class MemberChallengeParticipation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "member_id")
    private Member member;

    @ManyToOne
    @JoinColumn(name = "challenge_id")
    private HealthChallenge challenge;

    private LocalDateTime joinedAt;

    private Boolean completed;

    private Integer progressPercentage;

    private Integer pointsEarned;
}

