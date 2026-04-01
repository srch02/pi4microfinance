package pi.db.piversionbd.service.groups;

import pi.db.piversionbd.entities.groups.Conversation;
import pi.db.piversionbd.entities.groups.DirectMessage;

import java.util.List;

public interface IDirectChatService {

    DirectMessage sendMessage(Long groupId, Long senderMemberId, Long receiverMemberId, String content);

    List<Conversation> getConversationsForMember(Long groupId, Long memberId);

    List<DirectMessage> getMessagesForConversation(Long groupId, Long conversationId, Long requesterMemberId);
}

