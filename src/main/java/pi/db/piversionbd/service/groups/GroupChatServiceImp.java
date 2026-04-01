package pi.db.piversionbd.service.groups;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pi.db.piversionbd.config.HederaProperties;
import pi.db.piversionbd.entities.groups.Group;
import pi.db.piversionbd.entities.groups.GroupMessage;
import pi.db.piversionbd.entities.groups.Member;
import pi.db.piversionbd.repository.groups.GroupMessageRepository;
import pi.db.piversionbd.service.hedera.HederaAuditService;
import pi.db.piversionbd.service.security.AesEncryptionService;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class GroupChatServiceImp implements IGroupChatService {

    private static final Logger log = LoggerFactory.getLogger(GroupChatServiceImp.class);

    private final IGroupService groupService;
    private final IMemberService memberService;
    private final GroupMessageRepository groupMessageRepository;
    private final HederaAuditService hederaAuditService;
    private final HederaProperties hederaProperties;
    private final AesEncryptionService aesEncryptionService;

    @Override
    public GroupMessage sendMessage(Long groupId, Long senderMemberId, String content) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("Message content must not be empty");
        }

        Group group = groupService.getGroupById(groupId);
        Member sender = memberService.getMemberById(senderMemberId);

        // Encrypt the content with AES-256-GCM
        String encrypted = aesEncryptionService.encrypt(content);
        String hash = sha256Hex(encrypted);

        GroupMessage message = new GroupMessage();
        message.setGroup(group);
        message.setSender(sender);
        message.setEncryptedContent(encrypted);
        message.setMessageHash(hash);
        message.setCreatedAt(LocalDateTime.now());
        message.setFlagged(Boolean.FALSE);

        // Submit audit record to Hedera HCS (transactions topic)
        String hederaHash = submitAuditToHedera(group.getId(), sender.getId(), hash, message.getCreatedAt());
        message.setHederaTxHash(hederaHash);

        return groupMessageRepository.save(message);
    }

    @Override
    @Transactional(readOnly = true)
    public List<GroupMessage> getMessagesForGroup(Long groupId) {
        // Ensure group exists (404-style behaviour via service)
        groupService.getGroupById(groupId);
        return groupMessageRepository.findByGroup_IdOrderByCreatedAtAsc(groupId);
    }

    private String submitAuditToHedera(Long groupId, Long senderId, String messageHash, LocalDateTime createdAt) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("type", "GROUP_MESSAGE");
            payload.put("groupId", groupId);
            payload.put("senderId", senderId);
            payload.put("messageHash", messageHash);
            payload.put("timestamp", createdAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

            // Use a local ObjectMapper instance; no Spring bean required.
            String json = new ObjectMapper().writeValueAsString(payload);
            String topicId = hederaProperties.getTopicTransactions();
            return hederaAuditService.submitToTopic(topicId, json);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize Hedera GROUP_MESSAGE payload", e);
            return null;
        }
    }

    private static String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}

