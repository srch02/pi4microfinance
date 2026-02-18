package pi.db.piversionbd.repository.admin;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pi.db.piversionbd.entities.admin.SystemAlert;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SystemAlertRepository extends JpaRepository<SystemAlert, Long> {

    List<SystemAlert> findByActive(Boolean active);
    List<SystemAlert> findBySeverity(String severity);
    List<SystemAlert> findByRegion(String region);

    // Counters used by chatbot and dashboards
    long countBySeverity(String severity);
    long countByActive(Boolean active);
    long countByRegion(String region);

    /** For dedupe: at most one active LOW_POOL alert per group. */
    Optional<SystemAlert> findByAlertTypeAndSourceEntityTypeAndSourceEntityIdAndActive(
            String alertType, String sourceEntityType, Long sourceEntityId, Boolean active);

    // Stats
    @Query("select count(sa) from SystemAlert sa")
    long totalAlerts();

    @Query("select count(sa) from SystemAlert sa where sa.active = true")
    long activeAlerts();

    @Query("select count(sa) from SystemAlert sa where sa.active = false")
    long inactiveAlerts();

    @Query("select count(sa) from SystemAlert sa where lower(sa.severity) = 'critical'")
    long criticalAlerts();

    @Query("select sa.severity, count(sa) from SystemAlert sa group by sa.severity")
    List<Object[]> countGroupBySeverity();

    @Query("select sa.region, count(sa) from SystemAlert sa group by sa.region")
    List<Object[]> countGroupByRegion();

    // Recherche avancée (JPQL dynamique via paramètres facultatifs)
    @Query("select sa from SystemAlert sa where (:severity is null or sa.severity = :severity) and (:region is null or sa.region = :region) and (:active is null or sa.active = :active)")
    List<SystemAlert> search(@Param("severity") String severity, @Param("region") String region, @Param("active") Boolean active);

    // Expirées
    @Query("select sa from SystemAlert sa where sa.expirationDate is not null and sa.expirationDate < :now and sa.active = true")
    List<SystemAlert> findExpiredActive(@Param("now") LocalDateTime now);
}

