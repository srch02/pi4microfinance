package pi.db.piversionbd.Controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import pi.db.piversionbd.entities.health.HealthChallenge;
import pi.db.piversionbd.services.HealthChallengeService;

import java.util.List;

@RestController
@RequestMapping("/api/challenges")
@RequiredArgsConstructor
@CrossOrigin("*")
public class ChallengeController {

    private final HealthChallengeService challengeService;

    @PostMapping
    public HealthChallenge create(@RequestBody HealthChallenge challenge) {
        return challengeService.create(challenge);
    }

    @GetMapping
    public List<HealthChallenge> getAll() {
        return challengeService.getAll();
    }

    @PostMapping("/{challengeId}/participate/{memberId}")
    public void participate(@PathVariable Long challengeId,
                            @PathVariable Long memberId) {

        challengeService.participate(challengeId, memberId);
    }
}
