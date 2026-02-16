package pi.db.piversionbd.services;

import pi.db.piversionbd.entities.health.PharmacyRecommendation;

import java.util.List;

public interface PharmacyRecommendationService {

    PharmacyRecommendation create(PharmacyRecommendation recommendation);

    List<PharmacyRecommendation> getAll();

    PharmacyRecommendation update(Long id, PharmacyRecommendation recommendation);

    void delete(Long id);
}

