package pi.db.piversionbd.entities.hedera;

import jakarta.persistence.*;
import lombok.Data;
import pi.db.piversionbd.entities.groups.Member;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Records blockchain (Hedera) transaction hashes for audit trail.
 * Links to member, payment, or claim.
 */
@Entity
@Table(name = "BLOCKCHAIN_TRANSACTIONS")
@Data
public class BlockchainTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Hedera transaction ID. */
    @Column(name = "blockchain_hash", length = 256)
    private String blockchainHash;

    /** Type: CONTRACT_DEPLOY, PAYMENT, REIMBURSEMENT, DOCTOR_VISIT, PHARMACY, etc. */
    @Column(name = "transaction_type", nullable = false, length = 64)
    private String transactionType;

    /** Coins transferred (1 coin = 3 DT). */
    @Column(name = "coins_transferred", precision = 19, scale = 4)
    private BigDecimal coinsTransferred;

    @ManyToOne
    @JoinColumn(name = "member_id")
    private Member member;

    @Column(name = "payment_id")
    private Long paymentId;

    @Column(name = "claim_id")
    private Long claimId;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}
