package pi.db.piversionbd.service.admin;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pi.db.piversionbd.entities.admin.AdminReviewQueueItem;
import pi.db.piversionbd.repository.admin.AdminReviewQueueItemRepository;

import java.util.List;
import java.util.Optional;

@Service
public class AdminReviewQueueItemService {

    @Autowired
    private AdminReviewQueueItemRepository repository;

    // Récupérer tous les items
    public List<AdminReviewQueueItem> getAllItems() {
        return repository.findAll();
    }

    // Récupérer un item par ID
    public Optional<AdminReviewQueueItem> getItemById(Long id) {
        return repository.findById(id);
    }

    // Créer un nouvel item
    public AdminReviewQueueItem createItem(AdminReviewQueueItem item) {
        return repository.save(item);
    }

    // Mettre à jour un item existant
    public Optional<AdminReviewQueueItem> updateItem(Long id, AdminReviewQueueItem details) {
        return repository.findById(id).map(item -> {
            item.setTaskType(details.getTaskType());
            item.setPriorityScore(details.getPriorityScore());
            item.setPreRegistration(details.getPreRegistration());
            item.setClaim(details.getClaim());
            item.setMember(details.getMember());
            item.setAssignedTo(details.getAssignedTo());
            return repository.save(item);
        });
    }

    // Supprimer un item
    public boolean deleteItem(Long id) {
        if (repository.existsById(id)) {
            repository.deleteById(id);
            return true;
        }
        return false;
    }
}

