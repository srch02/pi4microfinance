package pi.db.piversionbd.entities.groups;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import pi.db.piversionbd.entities.admin.PlatformKpiSnapshot;
import pi.db.piversionbd.entities.score.Claim;

import java.util.List;

@Entity
@Table(name = "GROUPS")
@Data
public class Group {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String type;
    private String region;

    @Column(name = "min_members")
    private Integer minMembers;

    @Column(name = "max_members")
    private Integer maxMembers;

    @Column(name = "current_member_count")
    private Integer currentMemberCount;

    @ManyToOne
    @JoinColumn(name = "created_by_member_id")
    private Member creator;
    @JsonIgnore
    @OneToMany(mappedBy = "group")
    private List<Membership> memberships;
    @JsonIgnore
    @OneToMany(mappedBy = "group")
    private List<Payment> payments;

    @JsonIgnore
    @OneToMany(mappedBy = "group")
    private List<Claim> claims;
    @JsonIgnore
    @OneToMany(mappedBy = "group")
    private List<PlatformKpiSnapshot> kpiSnapshots;

    @OneToOne(mappedBy = "group")
    private GroupPool groupPool;
}

