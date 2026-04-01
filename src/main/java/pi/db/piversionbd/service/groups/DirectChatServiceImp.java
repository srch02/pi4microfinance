package pi.db.piversionbd.service.groups;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pi.db.piversionbd.config.HederaProperties;
import pi.db.piversionbd.entities.groups.Conversation;
import pi.db.piversionbd.entities.groups.DirectMessage;
import pi.db.piversionbd.entities.groups.Group;
import pi.db.piversionbd.entities.groups.Member;
import pi.db.piversionbd.repository.groups.ConversationRepository;
import pi.db.piversionbd.repository.groups.DirectMessageRepository;
import pi.db.piversionbd.repository.groups.MembershipRepository;
import pi.db.piversionbd.service.hedera.HederaAuditService;
import pi.db.piversionbd.exception.ResourceNotFoundException;
import pi.db.piversionbd.service.notifications.TelegramNotificationService;
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
public class DirectChatServiceImp implements IDirectChatService {

    private static final Logger log = LoggerFactory.getLogger(DirectChatServiceImp.class);

    private final IGroupService groupService;
    private final IMemberService memberService;
    private final MembershipRepository membershipRepository;
    private final ConversationRepository conversationRepository;
    private final DirectMessageRepository directMessageRepository;
    private final HederaAuditService hederaAuditService;
    private final HederaProperties hederaProperties;
    private final AesEncryptionService aesEncryptionService;
    private final TelegramNotificationService telegramNotificationService;

    @Override
    public DirectMessage sendMessage(Long groupId, Long senderMemberId, Long receiverMemberId, String content) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("Message content must not be empty");
        }
        if (senderMemberId == null || receiverMemberId == null) {
            throw new IllegalArgumentException("senderMemberId and receiverMemberId are required");
        }
        if (senderMemberId.equals(receiverMemberId)) {
            throw new IllegalArgumentException("receiverMemberId must be different from senderMemberId");
        }

        Group group = groupService.getGroupById(groupId);
        Member sender = memberService.getMemberById(senderMemberId);
        Member receiver = memberService.getMemberById(receiverMemberId);

        ensureMemberInGroup(groupId, senderMemberId);
        ensureMemberInGroup(groupId, receiverMemberId);

        Conversation conversation = getOrCreateConversation(group, sender, receiver);

        // AES encrypt on server; participants decrypt on read endpoints.
        String encrypted = aesEncryptionService.encrypt(content);
        String hash = sha256Hex(encrypted);

        DirectMessage msg = new DirectMessage();
        msg.setConversation(conversation);
        msg.setSender(sender);
        msg.setReceiver(receiver);
        msg.setEncryptedContent(encrypted);
        msg.setMessageHash(hash);
        msg.setCreatedAt(LocalDateTime.now());
        msg.setFlagged(Boolean.FALSE);

        // Submit audit record to Hedera HCS.
        String hederaHash = submitAuditToHedera(group.getId(), conversation.getId(), sender.getId(), hash, msg.getCreatedAt());
        msg.setHederaTxHash(hederaHash);
        DirectMessage saved = directMessageRepository.save(msg);

        // Telegram notification (best-effort): receiver has unread message.
        if (receiver.getTelegramChatId() != null && !receiver.getTelegramChatId().isBlank()) {
            log.info("Attempting Telegram notify receiverId={} chatId={}", receiver.getId(), receiver.getTelegramChatId());
            telegramNotificationService.sendMessage(
                    receiver.getTelegramChatId(),
                    "New message from member #" + sender.getId() + " in group #" + group.getId() + "."
            );
        } else {
            log.warn("Telegram notify skipped: receiverId={} has no telegramChatId", receiver.getId());
        }

        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Conversation> getConversationsForMember(Long groupId, Long memberId) {
        ensureMemberInGroup(groupId, memberId);

        List<Conversation> fromA = conversationRepository.findByGroup_IdAndMemberA_Id(groupId, memberId);
        List<Conversation> fromB = conversationRepository.findByGroup_IdAndMemberB_Id(groupId, memberId);

        // Union (no duplicates because each conversation is stored with ordered members).
        fromA.addAll(fromB);
        return fromA;
    }

    @Override
    @Transactional(readOnly = true)
    public List<DirectMessage> getMessagesForConversation(Long groupId, Long conversationId, Long requesterMemberId) {
        ensureMemberInGroup(groupId, requesterMemberId);

        Conversation conversation = conversationRepository.findByIdAndGroup_Id(conversationId, groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));

        boolean participant = requesterMemberId.equals(conversation.getMemberA().getId())
                || requesterMemberId.equals(conversation.getMemberB().getId());
        if (!participant) {
            throw new SecurityException("Not a participant in this conversation");
        }

        List<DirectMessage> list = directMessageRepository.findByConversation_IdOrderByCreatedAtAsc(conversationId);

        // Mark messages as read when the receiver fetches them.
        boolean changed = false;
        LocalDateTime now = LocalDateTime.now();
        for (DirectMessage m : list) {
            if (m.getReceiver() != null
                    && requesterMemberId.equals(m.getReceiver().getId())
                    && m.getReadAt() == null) {
                m.setReadAt(now);
                changed = true;
            }
        }
        if (changed) {
            directMessageRepository.saveAll(list);
        }

        return list;
    }

    private Conversation getOrCreateConversation(Group group, Member sender, Member receiver) {
        long aId = sender.getId();
        long bId = receiver.getId();

        long memberAId = Math.min(aId, bId);
        long memberBId = Math.max(aId, bId);

        Member memberA = memberAId == aId ? sender : receiver;
        Member memberB = memberBId == bId ? receiver : sender;

        return conversationRepository
                .findByGroup_IdAndMemberA_IdAndMemberB_Id(group.getId(), memberAId, memberBId)
                .orElseGet(() -> {
                    Conversation c = new Conversation();
                    c.setGroup(group);
                    c.setMemberA(memberA);
                    c.setMemberB(memberB);
                    c.setCreatedAt(LocalDateTime.now());
                    return conversationRepository.save(c);
                });
    }

    private void ensureMemberInGroup(Long groupId, Long memberId) {
        // Ensure member is in the group.
        // Primary check: MEMBERSHIPS row (not ended).
        boolean hasActiveMembership = membershipRepository
                .findByMember_IdAndGroup_IdAndEndedAtIsNull(memberId, groupId)
                .isPresent();

        if (hasActiveMembership) {
            return;
        }

        // Fallback for seeded/local setups: members can be linked via Member.currentGroupId.
        Member m = memberService.getMemberById(memberId);
        if (m.getCurrentGroup() != null && groupId.equals(m.getCurrentGroup().getId())) {
            return;
        }

        throw new ResourceNotFoundException("Member is not in group");
    }

    private String submitAuditToHedera(Long groupId, Long conversationId, Long senderId, String messageHash, LocalDateTime createdAt) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("type", "DIRECT_MESSAGE");
            payload.put("groupId", groupId);
            payload.put("conversationId", conversationId);
            payload.put("senderId", senderId);
            payload.put("messageHash", messageHash);
            payload.put("timestamp", createdAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

            String json = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(payload);
            String topicId = hederaProperties.getTopicTransactions();
            return hederaAuditService.submitToTopic(topicId, json);
        } catch (Exception e) {
            log.error("Failed Hedera audit submission for DIRECT_MESSAGE", e);
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

