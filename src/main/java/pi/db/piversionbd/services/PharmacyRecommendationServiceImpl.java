package pi.db.piversionbd.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pi.db.piversionbd.entities.groups.Membership;
import pi.db.piversionbd.entities.health.PharmacyRecommendation;
import pi.db.piversionbd.repositories.MembershipRepository;
import pi.db.piversionbd.repositories.PharmacyRecommendationRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PharmacyRecommendationServiceImpl
        implements PharmacyRecommendationService {

    private final PharmacyRecommendationRepository repository;
    private final MembershipRepository membershipRepository;

    @Override
    public PharmacyRecommendation create(PharmacyRecommendation recommendation) {
        return repository.save(recommendation);
    }

    @Override
    public List<PharmacyRecommendation> getAll() {
        return repository.findAll();
    }

    @Override
    public PharmacyRecommendation update(Long id, PharmacyRecommendation recommendation) {

        PharmacyRecommendation existing = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Recommendation not found"));

        existing.setDiscountPercentage(recommendation.getDiscountPercentage());

        return repository.save(existing);
    }

    @Override
    public void delete(Long id) {
        repository.deleteById(id);
    }
    @Override
    public double calculateDiscountedPrice(Long memberId, double originalPrice) {

        Membership membership = membershipRepository
                .findByMember_IdAndActiveTrue(memberId)
                .orElse(null);

        if (membership != null
                && membership.getPackageType() != null
                && membership.getPackageType().equalsIgnoreCase("PREMIUM")) {

            return originalPrice * 0.80; // 20% reduction
        }

        return originalPrice; // no reduction
    }

}

