package pi.db.piversionbd.service.score;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import pi.db.piversionbd.dto.score.AdherenceCreateRequest;
import pi.db.piversionbd.dto.score.AdherenceResponse;
import pi.db.piversionbd.entities.groups.Member;
import pi.db.piversionbd.entities.score.AdherenceEventType;
import pi.db.piversionbd.entities.score.AdherenceTracking;
import pi.db.piversionbd.entities.score.Claim;
import pi.db.piversionbd.exception.ResourceNotFoundException;
import pi.db.piversionbd.repository.score.AdherenceTrackingRepository;
import pi.db.piversionbd.repository.score.ClaimRepository;
import pi.db.piversionbd.repository.groups.MemberRepository;

import java.math.BigDecimal;

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
        AdherenceTracking saved = adherenceTrackingRepository.save(event);
        syncMemberAdherenceScore(event.getMember().getId(), event.getCurrentScore());
        return saved;
    }

    /**
     * Creates an onboarding adherence event in its own transaction.
     * This prevents the whole member-creation transaction from becoming rollback-only
     * if something goes wrong during adherence persistence.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createOnboardingBaseline(Long memberId, float initialScore) {
        if (memberId == null) return;
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Member introuvable: " + memberId));

        AdherenceTracking event = new AdherenceTracking();
        event.setMember(member);
        event.setEventType(AdherenceEventType.ONBOARDING_MEDICAL_BASELINE);
        event.setScoreChange(BigDecimal.ZERO);
        event.setCurrentScore(BigDecimal.valueOf(initialScore));
        event.setNote("Automatic baseline score from onboarding medical history.");
        adherenceTrackingRepository.save(event);
        syncMemberAdherenceScore(memberId, event.getCurrentScore());
    }

    @Transactional(readOnly = true)
    public AdherenceTracking getById(Long id) {
        return adherenceTrackingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("AdherenceTracking introuvable: " + id));
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

        AdherenceTracking saved = adherenceTrackingRepository.save(existing);
        if (saved.getMember() != null && saved.getCurrentScore() != null) {
            syncMemberAdherenceScore(saved.getMember().getId(), saved.getCurrentScore());
        }
        return saved;
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
                .orElseThrow(() -> new ResourceNotFoundException("Member introuvable: " + req.memberId));

        Claim claim = null;
        if (req.claimId != null) {
            claim = claimRepository.findById(req.claimId)
                    .orElseThrow(() -> new ResourceNotFoundException("Claim introuvable: " + req.claimId));
        }

        AdherenceTracking e = new AdherenceTracking();
        e.setMember(member);
        e.setRelatedClaim(claim);
        e.setEventType(AdherenceEventType.valueOf(req.eventType));
        e.setScoreChange(req.scoreChange);
        e.setCurrentScore(req.currentScore);
        e.setNote(req.note);

        AdherenceTracking saved = adherenceTrackingRepository.save(e);
        syncMemberAdherenceScore(member.getId(), req.currentScore);
        return saved;
    }

    /**
     * Returns the current adherence score for a member from the score module (latest event's currentScore).
     * Use this when displaying member so adherence is always from score.
     */
    @Transactional(readOnly = true)
    public Float getCurrentAdherenceScoreForMember(Long memberId) {
        return adherenceTrackingRepository.findTop1ByMember_IdOrderByCreatedAtDesc(memberId)
                .map(AdherenceTracking::getCurrentScore)
                .map(BigDecimal::floatValue)
                .orElse(null);
    }

    /** Updates Member.adherenceScore so it stays in sync with the score module. */
    private void syncMemberAdherenceScore(Long memberId, java.math.BigDecimal currentScore) {
        if (memberId == null || currentScore == null) return;
        memberRepository.findById(memberId).ifPresent(m -> {
            m.setAdherenceScore(currentScore.floatValue());
            memberRepository.save(m);
        });
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
