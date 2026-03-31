package pi.db.piversionbd.entities.groups;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cin_number", nullable = false)
    private String cinNumber;

    @Column(name = "personalized_monthly_price")
    private Float personalizedMonthlyPrice;

    @Column(name = "adherence_score")
    private Float adherenceScore;

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "current_group_id")
    private Group currentGroup;

    @OneToOne
    @JoinColumn(name = "pre_registration_id")
    private PreRegistration preRegistration;
    @JsonIgnore
    @OneToMany(mappedBy = "member")
    private List<Membership> memberships;
    @JsonIgnore
    @OneToMany(mappedBy = "member")
    private List<Payment> payments;
    @JsonIgnore
    @OneToMany(mappedBy = "member")
    private List<MedicalHistory> medicalHistories;
    @JsonIgnore
    @OneToMany(mappedBy = "member")
    private List<DocumentUpload> documentUploads;
    @JsonIgnore
    @OneToMany(mappedBy = "member")
    private List<AdherenceTracking> adherenceTrackingEvents;
    @JsonIgnore
    @OneToMany(mappedBy = "member")
    private List<MemberReward> memberRewards;
    @JsonIgnore
    @OneToMany(mappedBy = "member")
    private List<Consultation> consultations;
    @JsonIgnore
    @OneToMany(mappedBy = "member")
    private List<PharmacyRecommendation> pharmacyRecommendations;
    @JsonIgnore
    @OneToMany(mappedBy = "member")
    private List<HealthTrackingEntry> healthTrackingEntries;
    @JsonIgnore
    @OneToMany(mappedBy = "member")
    private List<MemberChurnForecast> memberChurnForecasts;
    @JsonIgnore
    @OneToMany(mappedBy = "member")
    private List<RetentionIntervention> retentionInterventions;
    @JsonIgnore
    @OneToMany(mappedBy = "member")
    private List<AdminReviewQueueItem> adminReviewQueueItems;
    @JsonIgnore
    @OneToMany(mappedBy = "member")
    private List<Claim> claims;
    @JsonIgnore
    @OneToMany(mappedBy = "creator")
    private List<Group> createdGroups;
}

