package pi.db.piversionbd.controller.admin;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pi.db.piversionbd.entities.admin.AdminReviewQueueItem;
import pi.db.piversionbd.service.admin.AdminReviewQueueItemService;

import java.util.List;

@RestController
@RequestMapping("/api/admin/review-queue")
public class AdminReviewQueueItemController {

    @Autowired
    private AdminReviewQueueItemService reviewQueueService;

    @GetMapping
    public List<AdminReviewQueueItem> getAllItems() {
        return reviewQueueService.getAllItems();
    }

    @GetMapping("/{id}")
    public ResponseEntity<AdminReviewQueueItem> getItemById(@PathVariable Long id) {
        return reviewQueueService.getItemById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public AdminReviewQueueItem createItem(@RequestBody AdminReviewQueueItem item) {
        return reviewQueueService.createItem(item);
    }

    @PutMapping("/{id}")
    public ResponseEntity<AdminReviewQueueItem> updateItem(
            @PathVariable Long id,
            @RequestBody AdminReviewQueueItem itemDetails) {

        return reviewQueueService.updateItem(id, itemDetails)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteItem(@PathVariable Long id) {
        if (reviewQueueService.deleteItem(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}

