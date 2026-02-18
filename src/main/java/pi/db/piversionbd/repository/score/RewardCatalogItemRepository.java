package pi.db.piversionbd.repositories.score;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import pi.db.piversionbd.entities.score.RewardCatalogItem;

import java.util.Optional;

public interface RewardCatalogItemRepository extends JpaRepository<RewardCatalogItem, Long> {

    Optional<RewardCatalogItem> findByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCase(String name);

    Page<RewardCatalogItem> findByActiveTrue(Pageable pageable);
}
