package pi.db.piversionbd.repository.groups;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pi.db.piversionbd.entities.groups.DirectMessage;

import java.util.List;

@Repository
public interface DirectMessageRepository extends JpaRepository<DirectMessage, Long> {

    List<DirectMessage> findByConversation_IdOrderByCreatedAtAsc(Long conversationId);

    long countByReceiver_IdAndReadAtIsNull(Long receiverMemberId);
}

