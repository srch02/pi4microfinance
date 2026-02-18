package pi.db.piversionbd.entities.score;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "CLAIM_SCORING")
@Data
public class ClaimScoring {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "claim_id", unique = true)
    private Claim claim;

    @Column(name = "reliability_score")
    private BigDecimal reliabilityScore;

    @Column(name = "document_score")
    private BigDecimal documentScore;

    @Column(name = "medical_score")
    private BigDecimal medicalScore;

    @Column(name = "compliance_score")
    private BigDecimal complianceScore;

    @Column(name = "total_score")
    private BigDecimal totalScore;

    @Column(name = "excluded_condition_detected", nullable = false)
    private boolean excludedConditionDetected = false;

    @Lob
    @Column(name = "fraud_indicators")
    private String fraudIndicators;

    @Column(name = "scored_at", nullable = false)
    private LocalDateTime scoredAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
