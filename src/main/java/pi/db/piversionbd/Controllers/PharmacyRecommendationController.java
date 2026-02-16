package pi.db.piversionbd.Controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import pi.db.piversionbd.entities.health.PharmacyRecommendation;
import pi.db.piversionbd.services.PharmacyRecommendationService;

import java.util.List;

@RestController
@RequestMapping("/api/pharmacy-recommendations")
@RequiredArgsConstructor
public class PharmacyRecommendationController {

    private final PharmacyRecommendationService service;

    @PostMapping
    public PharmacyRecommendation create(@RequestBody PharmacyRecommendation recommendation) {
        return service.create(recommendation);
    }

    @GetMapping
    public List<PharmacyRecommendation> getAll() {
        return service.getAll();
    }

    @PutMapping("/{id}")
    public PharmacyRecommendation update(@PathVariable Long id,
                                         @RequestBody PharmacyRecommendation recommendation) {
        return service.update(id, recommendation);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}


