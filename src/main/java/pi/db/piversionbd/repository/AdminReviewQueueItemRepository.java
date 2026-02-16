package pi.db.piversionbd.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pi.db.piversionbd.entities.admin.AdminReviewQueueItem;
import pi.db.piversionbd.entities.pre.PreRegistration;

import java.util.List;

public interface AdminReviewQueueItemRepository extends JpaRepository<AdminReviewQueueItem, Long> {
    List<AdminReviewQueueItem> findByPreRegistration(PreRegistration preRegistration);
}
