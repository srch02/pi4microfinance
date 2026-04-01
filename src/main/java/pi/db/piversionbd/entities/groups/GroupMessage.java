package pi.db.piversionbd.entities.groups;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "GROUP_MESSAGES")
@Data
@Schema(hidden = true)
public class GroupMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "group_id", nullable = false)
    private Group group;

    @ManyToOne(optional = false)
    @JoinColumn(name = "sender_member_id", nullable = false)
    private Member sender;

    /** Encrypted message content (AES-256). */
    @Lob
    @Column(name = "encrypted_content", nullable = false)
    private String encryptedContent;

    /** SHA-256 hash of the encrypted content (hex). */
    @Column(name = "message_hash", length = 64, nullable = false)
    private String messageHash;

    /** Hedera transaction hash / ID for the audit trail. */
    @Column(name = "hedera_tx_hash", length = 256)
    private String hederaTxHash;

    /** Optional fraud score (0.0–1.0) when ML is enabled. */
    @Column(name = "fraud_score")
    private Float fraudScore;

    /** True when the message is flagged as suspicious. */
    @Column(name = "flagged")
    private Boolean flagged;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}

