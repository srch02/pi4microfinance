package pi.db.piversionbd.dto.groups.directchat;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import pi.db.piversionbd.entities.groups.DirectMessage;
import pi.db.piversionbd.entities.groups.Group;

import java.time.LocalDateTime;

public final class DirectChatDto {

    private DirectChatDto() {}

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "1-to-1 conversation metadata inside a group.")
    public static class ConversationDto {
        @Schema(description = "Conversation ID.", example = "10")
        private Long conversationId;

        @Schema(description = "Group ID.", example = "3")
        private Long groupId;

        @Schema(description = "Other member ID (not the requester).", example = "15")
        private Long otherMemberId;

        @Schema(description = "Other member CIN (if you choose to expose it).", example = "12345678")
        private String otherCinNumber;

        @Schema(description = "When the conversation was created.", example = "2026-02-25T00:49:18")
        private LocalDateTime createdAt;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Direct message in a 1-to-1 conversation.")
    public static class DirectMessageDto {
        @Schema(description = "Message ID.", example = "100")
        private Long messageId;

        @Schema(description = "Conversation ID.", example = "10")
        private Long conversationId;

        @Schema(description = "Group ID.", example = "3")
        private Long groupId;

        @Schema(description = "Sender member ID.", example = "15")
        private Long senderId;

        @Schema(description = "Decrypted plaintext content (only visible to conversation participants).", example = "Hey file a claim for your back pain.")
        private String content;

        @Schema(description = "When the message was created.", example = "2026-02-25T00:49:18")
        private LocalDateTime createdAt;

        @Schema(description = "Hedera audit tx hash/id (immutable proof).", example = "0.0.7989863@1234567890")
        private String hederaTxHash;

        @Schema(description = "Fraud score between 0.0 and 1.0 (optional).", example = "0.87")
        private Float fraudScore;

        @Schema(description = "Flagged by fraud heuristics/ML (optional).", example = "false")
        private Boolean flagged;

        public static DirectMessageDto fromEntity(DirectMessage m, String decryptedContent) {
            if (m == null) return null;
            Group g = m.getConversation() != null ? m.getConversation().getGroup() : null;
            return new DirectMessageDto(
                    m.getId(),
                    m.getConversation() != null ? m.getConversation().getId() : null,
                    g != null ? g.getId() : null,
                    m.getSender() != null ? m.getSender().getId() : null,
                    decryptedContent,
                    m.getCreatedAt(),
                    m.getHederaTxHash(),
                    m.getFraudScore(),
                    m.getFlagged()
            );
        }
    }
}

