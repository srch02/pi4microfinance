package pi.db.piversionbd.entities.admin;

import jakarta.persistence.*;
import lombok.Data;
import pi.db.piversionbd.entities.groups.Member;
import pi.db.piversionbd.entities.pre.PreRegistration;
import pi.db.piversionbd.entities.score.Claim;

@Entity
@Table(name = "ADMIN_REVIEW_QUEUE")
@Data
public class AdminReviewQueueItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "task_type")
    private String taskType;

    @Column(name = "priority_score")
    private Float priorityScore;

    @ManyToOne
    @JoinColumn(name = "pre_registration_id")
    private PreRegistration preRegistration;

    @ManyToOne
    @JoinColumn(name = "claim_id")
    private Claim claim;

    @ManyToOne
    @JoinColumn(name = "member_id")
    private Member member;

    @ManyToOne
    @JoinColumn(name = "assigned_to_admin_id")
    private AdminUser assignedTo;
}
