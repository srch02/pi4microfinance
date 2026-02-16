package pi.db.piversionbd.services.score;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pi.db.piversionbd.entities.score.RewardCatalogItem;
import pi.db.piversionbd.exceptions.NotFoundException;
import pi.db.piversionbd.repositories.score.RewardCatalogItemRepository;

@Service
@RequiredArgsConstructor
@Transactional
public class RewardCatalogItemService {

    private final RewardCatalogItemRepository rewardCatalogItemRepository;

    public RewardCatalogItem create(RewardCatalogItem item) {
        if (item.getName() == null || item.getName().isBlank()) {
            throw new IllegalArgumentException("name est obligatoire.");
        }
        if (item.getPointsRequired() == null || item.getPointsRequired() <= 0) {
            throw new IllegalArgumentException("pointsRequired doit être > 0.");
        }
        if (rewardCatalogItemRepository.existsByNameIgnoreCase(item.getName())) {
            throw new IllegalArgumentException("Un reward avec ce nom existe déjà.");
        }

        return rewardCatalogItemRepository.save(item);
    }

    @Transactional(readOnly = true)
    public RewardCatalogItem getById(Long id) {
        return rewardCatalogItemRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("RewardCatalogItem introuvable: " + id));
    }

    @Transactional(readOnly = true)
    public Page<RewardCatalogItem> getAll(Pageable pageable) {
        return rewardCatalogItemRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Page<RewardCatalogItem> getActive(Pageable pageable) {
        return rewardCatalogItemRepository.findByActiveTrue(pageable);
    }

    public RewardCatalogItem update(Long id, RewardCatalogItem request) {
        RewardCatalogItem existing = getById(id);

        if (request.getName() != null && !request.getName().equalsIgnoreCase(existing.getName())) {
            if (rewardCatalogItemRepository.existsByNameIgnoreCase(request.getName())) {
                throw new IllegalArgumentException("Un reward avec ce nom existe déjà.");
            }
            existing.setName(request.getName());
        }

        existing.setDescription(request.getDescription());
        if (request.getPointsRequired() != null) {
            existing.setPointsRequired(request.getPointsRequired());
        }
        existing.setActive(request.isActive());

        return rewardCatalogItemRepository.save(existing);
    }

    public void delete(Long id) {
        RewardCatalogItem existing = getById(id);
        rewardCatalogItemRepository.delete(existing);
    }
}
