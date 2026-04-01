package pi.db.piversionbd.entities.groups;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "GROUP_CONVERSATIONS",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"group_id", "member_a_id", "member_b_id"}
        )
)
@Data
@Schema(hidden = true)
public class Conversation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "group_id", nullable = false)
    private Group group;

    @ManyToOne(optional = false)
    @JoinColumn(name = "member_a_id", nullable = false)
    private Member memberA;

    @ManyToOne(optional = false)
    @JoinColumn(name = "member_b_id", nullable = false)
    private Member memberB;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}

