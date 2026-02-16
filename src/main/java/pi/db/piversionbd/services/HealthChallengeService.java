package pi.db.piversionbd.services;

import pi.db.piversionbd.entities.health.HealthChallenge;

import java.util.List;

public interface HealthChallengeService {
    HealthChallenge create(HealthChallenge challenge);

    List<HealthChallenge> getAll();

    void participate(Long challengeId, Long memberId);
}
