package pi.db.piversionbd.repository.groups;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pi.db.piversionbd.entities.groups.Payment;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    void deleteByMember_Id(Long memberId);

    List<Payment> findByMember_IdOrderByCreatedAtDesc(Long memberId);

    List<Payment> findByMember_IdAndGroup_IdOrderByCreatedAtDesc(Long memberId, Long groupId);
    long countByMember_IdAndGroup_Id(Long memberId, Long groupId);

    Optional<Payment> findTopByMember_IdAndGroup_IdOrderByCreatedAtDesc(Long memberId, Long groupId);
}
