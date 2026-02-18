package pi.db.piversionbd.entities.pre;

import jakarta.persistence.*;
import lombok.Data;
import pi.db.piversionbd.entities.groups.Member;

import java.time.LocalDateTime;

@Entity
@Table(name = "MEDICAL_HISTORY")
@Data
public class MedicalHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "pre_registration_id")
    private PreRegistration preRegistration;

    @ManyToOne
    @JoinColumn(name = "member_id")
    private Member member;

    @Lob
    @Column(name = "excluded_condition_details")
    private String excludedConditionDetails;

    /**
     * Q&A payload (JSON) captured from the medical history form (future ML input).
     */
    @Lob
    @Column(name = "qa_payload")
    private String qaPayload;

    @Column(name = "quality_score")
    private Float qualityScore;

    @Column(name = "is_crude")
    private Boolean crude;

    @Column(name = "ml_fraud_score")
    private Float mlFraudScore;

    @Column(name = "ml_fraud_reason", length = 512)
    private String mlFraudReason;

    @Column(name = "assessed_at")
    private LocalDateTime assessedAt;
}

