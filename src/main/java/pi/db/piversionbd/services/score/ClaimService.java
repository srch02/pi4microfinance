package pi.db.piversionbd.services.score;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pi.db.piversionbd.dto.ClaimCreateRequest;
import pi.db.piversionbd.dto.ClaimResponse;
import pi.db.piversionbd.entities.groups.Group;
import pi.db.piversionbd.entities.groups.Member;
import pi.db.piversionbd.entities.score.*;
import pi.db.piversionbd.exceptions.NotFoundException;
import pi.db.piversionbd.repositories.score.ClaimRepository;
import pi.db.piversionbd.repositories.score.GroupRepository;
import pi.db.piversionbd.repositories.score.MemberRepository;
import pi.db.piversionbd.repositories.score.MemberRewardRepository;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class ClaimService {

    private final ClaimRepository claimRepository;
    private final MemberRepository memberRepository;
    private final GroupRepository groupRepository;




    public Claim create(Claim claim) {
        if (claim.getClaimNumber() == null || claim.getClaimNumber().isBlank()) {
            throw new IllegalArgumentException("claimNumber est obligatoire.");
        }
        if (claimRepository.existsByClaimNumber(claim.getClaimNumber())) {
            throw new IllegalArgumentException("claimNumber existe déjà.");
        }
        if (claim.getMember() == null) {
            throw new IllegalArgumentException("member est obligatoire.");
        }
        if (claim.getGroup() == null) {
            throw new IllegalArgumentException("group est obligatoire.");
        }

        if (claim.getStatus() == null) {
            claim.setStatus(ClaimStatus.SUBMITTED);
        }

        return claimRepository.save(claim);
    }

    @Transactional(readOnly = true)
    public Claim getById(Long id) {
        return claimRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Claim introuvable: " + id));
    }

    @Transactional(readOnly = true)
    public Claim getDetailsById(Long id) {
        return claimRepository.findDetailsById(id)
                .orElseThrow(() -> new NotFoundException("Claim introuvable: " + id));
    }

    @Transactional(readOnly = true)
    public Page<Claim> getAll(Pageable pageable) {
        return claimRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Page<Claim> getByStatus(ClaimStatus status, Pageable pageable) {
        return claimRepository.findByStatusOrderByCreatedAtDesc(status, pageable);
    }

    @Transactional(readOnly = true)
    public Claim getByClaimNumber(String claimNumber) {
        return claimRepository.findByClaimNumber(claimNumber)
                .orElseThrow(() -> new NotFoundException("Claim introuvable pour claimNumber: " + claimNumber));
    }

    public Claim update(Long id, Claim request) {
        Claim existing = getById(id);

        // Champs modifiables en CRUD simple
        existing.setAmountRequested(request.getAmountRequested());
        existing.setAmountApproved(request.getAmountApproved());
        existing.setFinalScoreSnapshot(request.getFinalScoreSnapshot());
        existing.setExcludedConditionDetected(request.isExcludedConditionDetected());
        existing.setDecisionComment(request.getDecisionComment());
        existing.setDecisionReason(request.getDecisionReason());
        existing.setDecisionAt(request.getDecisionAt());

        if (request.getStatus() != null) {
            existing.setStatus(request.getStatus());
        }

        return claimRepository.save(existing);
    }

    public Claim updateStatus(Long id, ClaimStatus status, ClaimDecisionReason reason, String comment) {
        Claim claim = getById(id);
        claim.setStatus(status);
        claim.setDecisionReason(reason);
        claim.setDecisionComment(comment);
        claim.setDecisionAt(LocalDateTime.now());
        return claimRepository.save(claim);
    }

    public void delete(Long id) {
        Claim claim = getById(id);
        claimRepository.delete(claim);
    }
    @Transactional
    public Claim createFromIds(ClaimCreateRequest req) {
        if (req.memberId == null) throw new IllegalArgumentException("memberId obligatoire");
        if (req.groupId == null) throw new IllegalArgumentException("groupId obligatoire");
        if (req.claimNumber == null || req.claimNumber.isBlank())
            throw new IllegalArgumentException("claimNumber obligatoire");

        Member member = memberRepository.findById(req.memberId)
                .orElseThrow(() -> new NotFoundException("Member introuvable: " + req.memberId));

        Group group = groupRepository.findById(req.groupId)
                .orElseThrow(() -> new NotFoundException("Group introuvable: " + req.groupId));

        Claim c = new Claim();
        c.setMember(member);
        c.setGroup(group);
        c.setClaimNumber(req.claimNumber);
        c.setAmountRequested(req.amountRequested);
        c.setStatus(ClaimStatus.SUBMITTED);

        return claimRepository.save(c);
    }

    public ClaimResponse toResponse(Claim c) {
        return new ClaimResponse(
                c.getId(),
                c.getClaimNumber(),
                c.getAmountRequested(),
                c.getAmountApproved(),
                c.getFinalScoreSnapshot(),
                c.getStatus() != null ? c.getStatus().name() : null,
                c.getDecisionReason() != null ? c.getDecisionReason().name() : null,
                c.isExcludedConditionDetected(),
                c.getDecisionComment(),
                c.getDecisionAt(),
                c.getCreatedAt(),
                c.getUpdatedAt(),
                c.getMember() != null ? c.getMember().getId() : null,
                c.getGroup() != null ? c.getGroup().getId() : null
        );
    }

}
