package pi.db.piversionbd.entities.groups;

import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;

/**
 * Request to change from one group to another. When a member already has a membership
 * in group A and tries to join group B, we create a PENDING request. Admin approves
 * → status APPROVED. Member then retries "add to group B" → we end membership in A,
 * create membership in B, set request to COMPLETED.
 */
@Entity
@Table(name = "group_change_requests")
@Data
public class GroupChangeRequest {

    public static final String STATUS_PENDING = "pending";
    public static final String STATUS_APPROVED = "approved";
    public static final String STATUS_REJECTED = "rejected";
    public static final String STATUS_COMPLETED = "completed";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    /** Group the member is currently in (will leave after approval + retry). */
    @ManyToOne
    @JoinColumn(name = "from_group_id", nullable = false)
    private Group fromGroup;

    /** Group the member wants to join. */
    @ManyToOne
    @JoinColumn(name = "to_group_id", nullable = false)
    private Group toGroup;

    /** pending → approved/rejected by admin; approved → completed when member retries add. */
    @Column(name = "status", length = 20, nullable = false)
    private String status = STATUS_PENDING;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    /** Optional: package type requested for the new membership (used when completing). */
    @Column(name = "requested_package_type", length = 20)
    private String requestedPackageType;
}
