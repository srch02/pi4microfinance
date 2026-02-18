package pi.db.piversionbd.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pi.db.piversionbd.entities.admin.AdminReviewQueueItem;

@Repository
public interface AdminReviewQueueItemRepository extends JpaRepository<AdminReviewQueueItem, Long> {
    // Tu peux ajouter des méthodes personnalisées si besoin
}
