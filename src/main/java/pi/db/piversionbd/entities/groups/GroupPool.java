package pi.db.piversionbd.entities.groups;

import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;

@Entity
@Table(name = "GROUP_POOLS")
@Data
public class GroupPool {

    /** When pool_balance falls below this ratio of total_contributions, we consider it \"low\" and can alert admins. */
    private static final float LOW_BALANCE_RATIO = 0.2f; // e.g. 20% of all contributions

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "group_id", unique = true)
    private Group group;

    @Column(name = "pool_balance")
    private Float poolBalance;

    @Column(name = "total_contributions")
    private Float totalContributions;

    @Column(name = "total_paid_out")
    private Float totalPaidOut;

    /** Last time the pool was updated (e.g. when a payment was applied). */
    @Column(name = "updated_at")
    private Instant updatedAt;

    /**
     * @return true if the pool balance is considered low compared to what members have contributed overall.
     *         This can be used to trigger admin alerts when the solidarity fund is close to exhaustion.
     */
    public boolean isLowBalance() {
        float contributions = totalContributions != null ? totalContributions : 0f;
        float balance = poolBalance != null ? poolBalance : 0f;
        if (contributions <= 0f) {
            // No contributions recorded yet; nothing to alert on.
            return false;
        }
        return balance <= contributions * LOW_BALANCE_RATIO;
    }
}

