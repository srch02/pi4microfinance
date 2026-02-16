package pi.db.piversionbd.entities.health;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
@Entity
@Table(name = "HEALTH_CHALLENGES")
@Data
public class HealthChallenge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    private String description;

    private Integer pointsReward;

    private LocalDateTime startDate;

    private LocalDateTime endDate;

    private Boolean active;

    @OneToMany(mappedBy = "challenge")
    private List<MemberChallengeParticipation> participations;
}

