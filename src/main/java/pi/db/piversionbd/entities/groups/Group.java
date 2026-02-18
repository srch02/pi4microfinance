package pi.db.piversionbd.entities.groups;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.Data;
import pi.db.piversionbd.entities.admin.PlatformKpiSnapshot;
import pi.db.piversionbd.entities.score.Claim;

import java.util.List;

@Entity
@Table(name = "groups")
@Data
@Schema(hidden = true) // only GroupDto is used in API; avoid schema generation issues
public class Group {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String type;
    private String region;

    /**
     * public: anyone can request membership by selecting the group.
     * private: membership requires an invite_code (QR code) shared by the creator.
     */
    @Column(name = "join_policy", length = 10)
    private String joinPolicy; // public | private

    /** Invite code for private groups only (nullable for public groups). */
    @Column(name = "invite_code", length = 64, unique = true)
    private String inviteCode;

    @Column(name = "min_members")
    private Integer minMembers;

    @Column(name = "max_members")
    private Integer maxMembers;

    @Column(name = "current_member_count")
    private Integer currentMemberCount;

    @ManyToOne
    @JoinColumn(name = "created_by_member_id")
    private Member creator;

    @OneToMany(mappedBy = "group")
    private List<Membership> memberships;

    @OneToMany(mappedBy = "group")
    private List<Payment> payments;

    @OneToMany(mappedBy = "group")
    private List<Claim> claims;

    @OneToMany(mappedBy = "group")
    private List<PlatformKpiSnapshot> kpiSnapshots;

    @OneToOne(mappedBy = "group")
    private GroupPool groupPool;
}

