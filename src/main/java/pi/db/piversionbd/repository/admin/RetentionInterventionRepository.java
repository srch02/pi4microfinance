package pi.db.piversionbd.repository.admin;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pi.db.piversionbd.entities.admin.RetentionIntervention;

import java.util.List;

@Repository
public interface RetentionInterventionRepository
        extends JpaRepository<RetentionIntervention, Long> {

    // 🔎 Trouver les interventions par Member ID
    List<RetentionIntervention> findByMemberId(Long memberId);

    // 🔎 Trouver les interventions par Prediction ID
    List<RetentionIntervention> findByPredictionId(Long predictionId);

    // 🔎 Trouver les interventions exécutées ou non
    List<RetentionIntervention> findByExecuted(Boolean executed);
}

