package pi.db.piversionbd.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pi.db.piversionbd.entities.groups.Member;
import pi.db.piversionbd.entities.health.HealthTrackingEntry;
import pi.db.piversionbd.repositories.HealthTrackingEntryRepository;
import pi.db.piversionbd.repositories.MemberRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class HealthTrackingEntryServiceImpl implements HealthTrackingEntryService {

    private final HealthTrackingEntryRepository repository;
    private final MemberRepository memberRepository;

    @Override
    public HealthTrackingEntry create(Long memberId, HealthTrackingEntry entry) {

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("Member not found"));

        entry.setMember(member);

        return repository.save(entry);
    }

    @Override
    public List<HealthTrackingEntry> getByMember(Long memberId) {
        return repository.findByMemberId(memberId);
    }

    @Override
    public void delete(Long id) {
        repository.deleteById(id);
    }
}

