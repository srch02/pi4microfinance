package pi.db.piversionbd.entities.score;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import pi.db.piversionbd.entities.admin.AdminReviewQueueItem;
import pi.db.piversionbd.entities.groups.Group;
import pi.db.piversionbd.entities.groups.Member;
import pi.db.piversionbd.entities.pre.DocumentUpload;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "CLAIMS")
@Data
public class Claim {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id")
    private Group group;

    @Column(name = "claim_number", nullable = false)
    private String claimNumber;

    @Column(name = "amount_requested")
    private BigDecimal amountRequested;

    @Column(name = "amount_approved")
    private BigDecimal amountApproved;

    @Column(name = "scoring_result")
    private BigDecimal finalScoreSnapshot;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private ClaimStatus status = ClaimStatus.SUBMITTED;

    @Enumerated(EnumType.STRING)
    @Column(name = "decision_reason", length = 50)
    private ClaimDecisionReason decisionReason;

    @Column(name = "excluded_condition_detected", nullable = false)
    private boolean excludedConditionDetected = false;

    @Lob
    @Column(name = "decision_comment")
    private String decisionComment;

    @Column(name = "decision_at")
    private LocalDateTime decisionAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToOne(mappedBy = "claim")
    private ClaimScoring claimScoring;

    @OneToMany(mappedBy = "relatedClaim", fetch = FetchType.LAZY)
    @JsonIgnore
    private List<AdherenceTracking> adherenceTrackingEvents;

    @OneToMany(mappedBy = "claim", fetch = FetchType.LAZY)
    @JsonIgnore
    private List<DocumentUpload> documentUploads;

    @OneToMany(mappedBy = "claim", fetch = FetchType.LAZY)
    @JsonIgnore
    private List<AdminReviewQueueItem> adminReviewQueueItems;
}
