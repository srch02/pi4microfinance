package pi.db.piversionbd.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pi.db.piversionbd.entities.health.ChatbotMessage;
import pi.db.piversionbd.entities.groups.Member;

import java.util.List;

@Repository
public interface ChatbotMessageRepository extends JpaRepository<ChatbotMessage, Long> {
    List<ChatbotMessage> findByMemberId(Long memberId);
    List<ChatbotMessage> findByMemberIdOrderByCreatedAtDesc(Long memberId);
    List<ChatbotMessage> findByDetectedMaladie(String maladie);
}

