package pi.db.piversionbd.entities.groups;

import jakarta.persistence.*;
import lombok.Data;
import pi.db.piversionbd.entities.health.Consultation;

@Entity
@Table(name = "PAYMENTS")
@Data
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "member_id")
    private Member member;

    @ManyToOne
    @JoinColumn(name = "group_id")
    private Group group;

    private Float amount;

    @Column(name = "pool_allocation")
    private Float poolAllocation;

    @Column(name = "platform_fee")
    private Float platformFee;

    @Column(name = "national_fund")
    private Float nationalFund;

    @OneToOne
    private Consultation consultation;
}

