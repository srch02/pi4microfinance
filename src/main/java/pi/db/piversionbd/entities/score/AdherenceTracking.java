package pi.db.piversionbd.entities.score;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import pi.db.piversionbd.entities.groups.Member;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "ADHERENCE_TRACKING")
@Data
public class AdherenceTracking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "member_id")
    private Member member;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type")
    private AdherenceEventType eventType;

    @Column(name = "score_change")
    private BigDecimal scoreChange;

    @ManyToOne
    @JoinColumn(name = "related_claim_id")
    private Claim relatedClaim;

    @Column(name = "current_score")
    private BigDecimal currentScore;

    @Column(name = "note", length = 300)
    private String note;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
