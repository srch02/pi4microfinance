package pi.db.piversionbd.controller.admin;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pi.db.piversionbd.dto.score.AdherenceResponse;
import pi.db.piversionbd.entities.groups.Member;
import pi.db.piversionbd.repository.groups.MemberRepository;
import pi.db.piversionbd.service.score.AdherenceTrackingService;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/adherence-tracking")
@RequiredArgsConstructor
public class AdherenceAdminController {

    private final MemberRepository memberRepository;
    private final AdherenceTrackingService adherenceTrackingService;

    /**
     * GET /api/admin/adherence-tracking/members
     * Returns all members with their current adherence score and latest event.
     */
    @GetMapping("/members")
    public ResponseEntity<Page<MemberAdherenceSummary>> getAllMembersAdherence(
            @PageableDefault(size = 20, sort = "id") Pageable pageable
    ) {
        Page<Member> members = memberRepository.findAll(pageable);

        List<MemberAdherenceSummary> summaries = members.getContent().stream()
                .map(m -> {
                    Float score = adherenceTrackingService.getCurrentAdherenceScoreForMember(m.getId());
                    return new MemberAdherenceSummary(
                            m.getId(),
                            m.getEmail(),
                            m.getCinNumber(),
                            m.getProfession(),
                            m.getRegion(),
                            m.getAge(),
                            score != null ? score : (m.getAdherenceScore() != null ? m.getAdherenceScore() : 0f),
                            m.getCreatedAt() != null ? m.getCreatedAt().toString() : null,
                            m.getCurrentGroup() != null ? m.getCurrentGroup().getId() : null,
                            m.getCurrentGroup() != null ? m.getCurrentGroup().getName() : null
                    );
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(new PageImpl<>(summaries, pageable, members.getTotalElements()));
    }

    /**
     * GET /api/admin/adherence-tracking/members/{memberId}/events
     * Returns all adherence events for a specific member.
     */
    @GetMapping("/members/{memberId}/events")
    public ResponseEntity<Page<AdherenceResponse>> getMemberEvents(
            @PathVariable Long memberId,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable
    ) {
        return ResponseEntity.ok(
                adherenceTrackingService.getByMember(memberId, pageable)
                        .map(adherenceTrackingService::toResponse)
        );
    }

    public record MemberAdherenceSummary(
            Long id,
            String email,
            String cinNumber,
            String profession,
            String region,
            Integer age,
            Float adherenceScore,
            String createdAt,
            Long groupId,
            String groupName
    ) {}
}
