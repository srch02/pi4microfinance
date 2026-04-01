package pi.db.piversionbd.entities.groups;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "DIRECT_MESSAGES")
@Data
@Schema(hidden = true)
public class DirectMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    @ManyToOne(optional = false)
    @JoinColumn(name = "sender_member_id", nullable = false)
    private Member sender;

    @ManyToOne(optional = false)
    @JoinColumn(name = "receiver_member_id", nullable = false)
    private Member receiver;

    /**
     * Encrypted message content (Base64 for AES-GCM: IV || ciphertext).
     */
    @Lob
    @Column(name = "encrypted_content", nullable = false)
    private String encryptedContent;

    /** SHA-256 hex of encrypted_content. */
    @Column(name = "message_hash", length = 64, nullable = false)
    private String messageHash;

    /** Hedera transaction hash / ID for immutable audit trail. */
    @Column(name = "hedera_tx_hash", length = 256)
    private String hederaTxHash;

    /** Optional ML / fraud score (0.0 - 1.0). */
    @Column(name = "fraud_score")
    private Float fraudScore;

    /** True if flagged by fraud heuristics/ML. */
    @Column(name = "flagged")
    private Boolean flagged = false;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /** When the receiver read the message (null = unread). */
    @Column(name = "read_at")
    private LocalDateTime readAt;
}

