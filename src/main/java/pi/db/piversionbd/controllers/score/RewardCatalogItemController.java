package pi.db.piversionbd.controllers.score;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import pi.db.piversionbd.entities.score.RewardCatalogItem;
import pi.db.piversionbd.services.score.RewardCatalogItemService;

import java.net.URI;

@RestController
@RequestMapping("/api/rewards/catalog")
@RequiredArgsConstructor
public class RewardCatalogItemController {

    private final RewardCatalogItemService rewardCatalogItemService;

    @PostMapping
    public ResponseEntity<RewardCatalogItem> create(@RequestBody RewardCatalogItem item) {
        RewardCatalogItem created = rewardCatalogItemService.create(item);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.getId())
                .toUri();

        return ResponseEntity.created(location).body(created);
    }

    @GetMapping("/{id}")
    public ResponseEntity<RewardCatalogItem> getById(@PathVariable Long id) {
        return ResponseEntity.ok(rewardCatalogItemService.getById(id));
    }

    @GetMapping
    public ResponseEntity<Page<RewardCatalogItem>> getAll(
            @RequestParam(defaultValue = "false") boolean activeOnly,
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable
    ) {
        if (activeOnly) {
            return ResponseEntity.ok(rewardCatalogItemService.getActive(pageable));
        }
        return ResponseEntity.ok(rewardCatalogItemService.getAll(pageable));
    }

    @PutMapping("/{id}")
    public ResponseEntity<RewardCatalogItem> update(
            @PathVariable Long id,
            @RequestBody RewardCatalogItem request
    ) {
        return ResponseEntity.ok(rewardCatalogItemService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        rewardCatalogItemService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
