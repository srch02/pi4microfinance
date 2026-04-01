package pi.db.piversionbd.entities.groups;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

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

    /** Hedera transaction ID for this payment (blockchain audit). */
    @Column(name = "blockchain_hash", length = 256)
    private String blockchainHash;

    /** Amount in coins (1 coin = 3 DT). */
    @Column(name = "coin_amount")
    private Float coinAmount;

    /** Amount in DT (Dinars Tunisiens). */
    @Column(name = "dt_amount")
    private Float dtAmount;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}

