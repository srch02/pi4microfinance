package pi.db.piversionbd.entities.groups;

import jakarta.persistence.*;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonIgnore;
import pi.db.piversionbd.entities.admin.AdminReviewQueueItem;
import pi.db.piversionbd.entities.admin.MemberChurnForecast;
import pi.db.piversionbd.entities.admin.RetentionIntervention;
import pi.db.piversionbd.entities.health.Consultation;
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

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "personalized_monthly_price")
    private Float personalizedMonthlyPrice;

    @Column(name = "adherence_score")
    private Float adherenceScore;

    @ManyToOne
    @JoinColumn(name = "current_group_id")
    private Group currentGroup;

    @OneToOne
    @JoinColumn(name = "pre_registration_id")
    private PreRegistration preRegistration;

    @OneToMany(mappedBy = "member")
    @JsonIgnore
    private List<Membership> memberships;

    @OneToMany(mappedBy = "member")
    @JsonIgnore
    private List<Payment> payments;

    @OneToMany(mappedBy = "member")
    @JsonIgnore
    private List<MedicalHistory> medicalHistories;

    @OneToMany(mappedBy = "member")
    @JsonIgnore
    private List<DocumentUpload> documentUploads;

    @OneToMany(mappedBy = "member")
    @JsonIgnore
    private List<AdherenceTracking> adherenceTrackingEvents;

    @OneToMany(mappedBy = "member")
    @JsonIgnore
    private List<MemberReward> memberRewards;

    @OneToMany(mappedBy = "member")
    @JsonIgnore
    private List<MemberChurnForecast> memberChurnForecasts;

    @OneToMany(mappedBy = "member")
    @JsonIgnore
    private List<RetentionIntervention> retentionInterventions;

    @OneToMany(mappedBy = "member")
    @JsonIgnore
    private List<AdminReviewQueueItem> adminReviewQueueItems;

    @OneToMany(mappedBy = "member")
    @JsonIgnore
    private List<Claim> claims;

    @OneToMany(mappedBy = "creator")
    @JsonIgnore
    private List<Group> createdGroups;

    @OneToMany(mappedBy = "member", cascade = jakarta.persistence.CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<Consultation> consultations;
}
