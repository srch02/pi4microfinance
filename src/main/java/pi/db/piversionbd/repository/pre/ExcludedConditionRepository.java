package pi.db.piversionbd.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pi.db.piversionbd.entities.pre.ExcludedCondition;

import java.util.List;

public interface ExcludedConditionRepository extends JpaRepository<ExcludedCondition, Long> {
    List<ExcludedCondition> findByAutoRejectTrue();
}