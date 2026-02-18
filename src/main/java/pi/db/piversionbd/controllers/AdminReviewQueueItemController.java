package pi.db.piversionbd.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pi.db.piversionbd.entities.admin.AdminReviewQueueItem;
import pi.db.piversionbd.services.AdminReviewQueueItemService;

import java.util.List;

@RestController
@RequestMapping("/api/admin/review-queue")
public class AdminReviewQueueItemController {

    @Autowired
    private AdminReviewQueueItemService reviewQueueService;

    // GET tous les items
    @GetMapping
    public List<AdminReviewQueueItem> getAllItems() {
        return reviewQueueService.getAllItems();
    }

    // GET item par ID
    @GetMapping("/{id}")
    public ResponseEntity<AdminReviewQueueItem> getItemById(@PathVariable Long id) {
        return reviewQueueService.getItemById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // POST créer un nouvel item
    @PostMapping
    public AdminReviewQueueItem createItem(@RequestBody AdminReviewQueueItem item) {
        return reviewQueueService.createItem(item);
    }

    // PUT mettre à jour un item
    @PutMapping("/{id}")
    public ResponseEntity<AdminReviewQueueItem> updateItem(
            @PathVariable Long id,
            @RequestBody AdminReviewQueueItem itemDetails) {

        return reviewQueueService.updateItem(id, itemDetails)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // DELETE un item
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteItem(@PathVariable Long id) {
        if (reviewQueueService.deleteItem(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}
