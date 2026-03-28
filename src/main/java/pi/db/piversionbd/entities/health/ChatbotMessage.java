package pi.db.piversionbd.entities.health;

import jakarta.persistence.*;
import pi.db.piversionbd.entities.groups.Member;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "CHATBOT_MESSAGES")
@Data
public class ChatbotMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "member_id", nullable = true)
    private Member member;

    @Column(name = "user_message", columnDefinition = "TEXT")
    private String userMessage;

    @Column(name = "bot_response", columnDefinition = "TEXT")
    private String botResponse;

    @Column(name = "recommended_doctor_id", nullable = true)
    private Long recommendedDoctorId;

    @Column(name = "recommended_product_id", nullable = true)
    private Long recommendedProductId;

    @Column(name = "detected_maladie")
    private String detectedMaladie;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public ChatbotMessage() {
        this.createdAt = LocalDateTime.now();
    }
}

