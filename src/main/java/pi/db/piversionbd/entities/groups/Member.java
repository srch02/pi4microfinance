package pi.db.piversionbd.entities.groups;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.Data;
import pi.db.piversionbd.entities.admin.AdminReviewQueueItem;
import pi.db.piversionbd.entities.admin.MemberChurnForecast;
import pi.db.piversionbd.entities.admin.RetentionIntervention;
import pi.db.piversionbd.entities.health.Consultation;
import pi.db.piversionbd.entities.health.HealthTrackingEntry;
import pi.db.piversionbd.entities.health.PharmacyRecommendation;
import pi.db.piversionbd.entities.pre.DocumentUpload;
import pi.db.piversionbd.entities.pre.MedicalHistory;
import pi.db.piversionbd.entities.pre.PreRegistration;
import pi.db.piversionbd.entities.score.AdherenceTracking;
import pi.db.piversionbd.entities.score.Claim;
import pi.db.piversionbd.entities.score.MemberReward;

import java.util.List;

@Entity
@Table(name = "MEMBERS")
@Data
@Schema(hidden = true)
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cin_number", nullable = false, unique = true)
    private String cinNumber;

    @Column(name = "personalized_monthly_price")
    private Float personalizedMonthlyPrice;

    @Column(name = "price_basic")
    private Float priceBasic;

    @Column(name = "price_confort")
    private Float priceConfort;

    @Column(name = "price_premium")
    private Float pricePremium;

    @Column(name = "adherence_score")
    private Float adherenceScore;

    @Column(name = "age")
    private Integer age;

    @Column(name = "profession", length = 100)
    private String profession;

    @Column(name = "region", length = 100)
    private String region;

    @ManyToOne
    @JoinColumn(name = "current_group_id")
    private Group currentGroup;

    @OneToOne
    @JoinColumn(name = "pre_registration_id")
    private PreRegistration preRegistration;

    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Membership> memberships;

    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Payment> payments;

    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MedicalHistory> medicalHistories;

    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DocumentUpload> documentUploads;

    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AdherenceTracking> adherenceTrackingEvents;

    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MemberReward> memberRewards;

    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Consultation> consultations;

    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PharmacyRecommendation> pharmacyRecommendations;

    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<HealthTrackingEntry> healthTrackingEntries;

    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MemberChurnForecast> memberChurnForecasts;

    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RetentionIntervention> retentionInterventions;

    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AdminReviewQueueItem> adminReviewQueueItems;

    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Claim> claims;

    @OneToMany(mappedBy = "creator")
    private List<Group> createdGroups;
}

