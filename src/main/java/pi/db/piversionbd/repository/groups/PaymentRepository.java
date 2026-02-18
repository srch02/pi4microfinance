package pi.db.piversionbd.repository.groups;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pi.db.piversionbd.entities.groups.Payment;

import java.util.List;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByMember_IdOrderByIdDesc(Long memberId);

    List<Payment> findByMember_IdAndGroup_IdOrderByIdDesc(Long memberId, Long groupId);
}
