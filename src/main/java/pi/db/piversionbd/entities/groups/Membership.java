package pi.db.piversionbd.entities.groups;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "MEMBERSHIPS")
@Data
public class Membership {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "member_id")
    private Member member;

    @ManyToOne
    @JoinColumn(name = "group_id")
    private Group group;

    @Column(name = "packageType")
    private String  packageType;

    @Column(name = "monthly_amount")
    private Float monthlyAmount;

    @Column(name = "consultations_limit")
    private Integer consultationsLimit;

    @Column(name = "annual_limit")
    private Float annualLimit;

    @Column(nullable = false)
    private Boolean active = true;

}

