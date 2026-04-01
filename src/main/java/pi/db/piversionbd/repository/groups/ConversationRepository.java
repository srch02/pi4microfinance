package pi.db.piversionbd.repository.groups;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pi.db.piversionbd.entities.groups.Conversation;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    Optional<Conversation> findByGroup_IdAndMemberA_IdAndMemberB_Id(Long groupId, Long memberAId, Long memberBId);

    List<Conversation> findByGroup_IdAndMemberA_Id(Long groupId, Long memberId);

    List<Conversation> findByGroup_IdAndMemberB_Id(Long groupId, Long memberId);

    Optional<Conversation> findByIdAndGroup_Id(Long conversationId, Long groupId);
}

