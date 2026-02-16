package pi.db.piversionbd.services.score;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pi.db.piversionbd.dto.AdherenceCreateRequest;
import pi.db.piversionbd.dto.AdherenceResponse;
import pi.db.piversionbd.entities.groups.Member;
import pi.db.piversionbd.entities.score.AdherenceEventType;
import pi.db.piversionbd.entities.score.AdherenceTracking;
import pi.db.piversionbd.entities.score.Claim;
import pi.db.piversionbd.exceptions.NotFoundException;
import pi.db.piversionbd.repositories.score.AdherenceTrackingRepository;
import pi.db.piversionbd.repositories.score.ClaimRepository;
import pi.db.piversionbd.repositories.score.GroupRepository;
import pi.db.piversionbd.repositories.score.MemberRepository;

@Service
@RequiredArgsConstructor
@Transactional
public class AdherenceTrackingService {

    private final AdherenceTrackingRepository adherenceTrackingRepository;
    private final MemberRepository memberRepository;
    private final ClaimRepository claimRepository;

    public AdherenceTracking create(AdherenceTracking event) {
        if (event.getMember() == null) {
            throw new IllegalArgumentException("member est obligatoire.");
        }
        if (event.getEventType() == null) {
            throw new IllegalArgumentException("eventType est obligatoire.");
        }
        if (event.getScoreChange() == null || event.getCurrentScore() == null) {
            throw new IllegalArgumentException("scoreChange et currentScore sont obligatoires.");
        }
        return adherenceTrackingRepository.save(event);
    }

    @Transactional(readOnly = true)
    public AdherenceTracking getById(Long id) {
        return adherenceTrackingRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("AdherenceTracking introuvable: " + id));
    }

    @Transactional(readOnly = true)
    public Page<AdherenceTracking> getByMember(Long memberId, Pageable pageable) {
        return adherenceTrackingRepository.findByMemberIdOrderByCreatedAtDesc(memberId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<AdherenceTracking> getByClaim(Long claimId, Pageable pageable) {
        return adherenceTrackingRepository.findByRelatedClaimIdOrderByCreatedAtDesc(claimId, pageable);
    }

    public AdherenceTracking update(Long id, AdherenceTracking request) {
        AdherenceTracking existing = getById(id);

        existing.setEventType(request.getEventType());
        existing.setScoreChange(request.getScoreChange());
        existing.setCurrentScore(request.getCurrentScore());
        existing.setRelatedClaim(request.getRelatedClaim());
        existing.setNote(request.getNote());

        return adherenceTrackingRepository.save(existing);
    }

    public void delete(Long id) {
        AdherenceTracking existing = getById(id);
        adherenceTrackingRepository.delete(existing);
    }

    @Transactional
    public AdherenceTracking createFromIds(AdherenceCreateRequest req) {
        if (req.memberId == null) throw new IllegalArgumentException("memberId obligatoire");
        if (req.eventType == null) throw new IllegalArgumentException("eventType obligatoire");

        Member member = memberRepository.findById(req.memberId)
                .orElseThrow(() -> new NotFoundException("Member introuvable: " + req.memberId));

        Claim claim = null;
        if (req.claimId != null) {
            claim = claimRepository.findById(req.claimId)
                    .orElseThrow(() -> new NotFoundException("Claim introuvable: " + req.claimId));
        }

        AdherenceTracking e = new AdherenceTracking();
        e.setMember(member);
        e.setRelatedClaim(claim);
        e.setEventType(AdherenceEventType.valueOf(req.eventType));
        e.setScoreChange(req.scoreChange);
        e.setCurrentScore(req.currentScore);
        e.setNote(req.note);

        return adherenceTrackingRepository.save(e);
    }

    public AdherenceResponse toResponse(AdherenceTracking e) {
        return new AdherenceResponse(
                e.getId(),
                e.getMember() != null ? e.getMember().getId() : null,
                e.getRelatedClaim() != null ? e.getRelatedClaim().getId() : null,
                e.getEventType() != null ? e.getEventType().name() : null,
                e.getScoreChange(),
                e.getCurrentScore(),
                e.getNote(),
                e.getCreatedAt()
        );
    }

}
