package pi.db.piversionbd.repository.admin;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pi.db.piversionbd.entities.admin.MemberChurnForecast;

import java.util.List;

@Repository
public interface MemberChurnForecastRepository
        extends JpaRepository<MemberChurnForecast, Long> {

    // ✅ Trouver les prédictions par niveau de risque
    List<MemberChurnForecast> findByRiskLevel(String riskLevel);

    // ✅ Trouver les prédictions par member ID
    List<MemberChurnForecast> findByMemberId(Long memberId);

}

