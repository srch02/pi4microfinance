package pi.db.piversionbd.Controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import pi.db.piversionbd.entities.groups.Member;
import pi.db.piversionbd.entities.health.HealthTrackingEntry;
import pi.db.piversionbd.repositories.HealthTrackingEntryRepository;
import pi.db.piversionbd.repositories.MemberRepository;

import java.util.List;

@RestController
@RequestMapping("/api/health-tracking")
@RequiredArgsConstructor
public class HealthTrackingEntryController {

    private final HealthTrackingEntryRepository repository;
    private final MemberRepository memberRepository;

    @PostMapping("/{memberId}")
    public HealthTrackingEntry create(@PathVariable Long memberId,
                                      @RequestBody HealthTrackingEntry entry) {

        Member member = memberRepository.findById(memberId)
                .orElseThrow();

        entry.setMember(member);

        return repository.save(entry);
    }

    @GetMapping("/member/{memberId}")
    public List<HealthTrackingEntry> getByMember(@PathVariable Long memberId) {
        return repository.findByMemberId(memberId);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        repository.deleteById(id);
    }
}

