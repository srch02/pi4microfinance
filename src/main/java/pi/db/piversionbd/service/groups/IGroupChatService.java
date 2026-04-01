package pi.db.piversionbd.service.groups;

import pi.db.piversionbd.entities.groups.GroupMessage;

import java.util.List;

public interface IGroupChatService {

    /**
     * Sends a new message in the given group from the given member.
     * Content is plaintext; encryption and Hedera audit are handled server-side.
     */
    GroupMessage sendMessage(Long groupId, Long senderMemberId, String content);

    /**
     * Returns all messages for a group, ordered by creation time (oldest first).
     */
    List<GroupMessage> getMessagesForGroup(Long groupId);
}

