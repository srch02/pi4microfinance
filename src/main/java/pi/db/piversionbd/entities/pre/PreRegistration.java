package pi.db.piversionbd.entities.pre;

import jakarta.persistence.*;
import lombok.Data;
import pi.db.piversionbd.entities.admin.AdminReviewQueueItem;
import pi.db.piversionbd.entities.groups.Member;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "PRE_REGISTRATIONS")
@Data
public class PreRegistration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cin_number", nullable = false)
    private String cinNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PreRegistrationStatus status;

    @Column(name = "fraud_score")
    private Float fraudScore;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "preRegistration")
    private List<MedicalHistory> medicalHistories;

    @OneToMany(mappedBy = "preRegistration")
    private List<RiskAssessment> riskAssessments;

    @OneToMany(mappedBy = "preRegistration")
    private List<DocumentUpload> documentUploads;

    @OneToMany(mappedBy = "preRegistration")
    private List<AdminReviewQueueItem> adminReviewQueueItems;

    @OneToOne(mappedBy = "preRegistration")
    private Member member;
}

