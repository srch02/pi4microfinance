package pi.db.piversionbd.controller.groups;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pi.db.piversionbd.dto.groups.GroupsModuleDto;
import pi.db.piversionbd.entities.groups.Group;
import pi.db.piversionbd.entities.groups.Member;
import pi.db.piversionbd.entities.groups.Membership;
import pi.db.piversionbd.service.groups.IGroupService;
import pi.db.piversionbd.service.groups.IMemberService;
import pi.db.piversionbd.service.groups.IMembershipService;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
@Tag(name = "Module 1 – Members & Memberships", description = "Members CRUD, filter by group; membership operations (groups/payments after approved)")
public class MemberController {

    private final IMemberService memberService;
    private final IMembershipService membershipService;
    private final IGroupService groupService;

    @GetMapping
    @Operation(summary = "List all members or filter by group", description = "Use optional query param groupId to get only members of that group.")
    @ApiResponse(responseCode = "200", description = "OK")
    public ResponseEntity<List<GroupsModuleDto.MemberDto>> getAll(
            @Parameter(description = "Filter by group ID (optional). Placeholder: 1", schema = @Schema(type = "integer", format = "int64", example = "1"))
            @RequestParam(required = false) Long groupId) {
        List<Member> members = groupId != null
                ? memberService.getMembersByGroupId(groupId)
                : memberService.getAllMembers();
        List<GroupsModuleDto.MemberDto> list = members.stream()
                .map(GroupsModuleDto.MemberDto::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(list);
    }

    @GetMapping("/{memberId}")
    @Operation(summary = "Get member by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK"),
            @ApiResponse(responseCode = "404", description = "Member not found", content = @Content(mediaType = "text/plain", schema = @Schema(example = "Member not found with id 99")))
    })
    public ResponseEntity<GroupsModuleDto.MemberDto> getById(
            @Parameter(description = "Member ID. Placeholder: 1", required = true, schema = @Schema(example = "1")) @PathVariable Long memberId) {
        Member member = memberService.getMemberById(memberId);
        return ResponseEntity.ok(GroupsModuleDto.MemberDto.fromEntity(member));
    }

    @GetMapping("/{memberId}/groups")
    @Operation(summary = "Get groups for member (active memberships)", description = "Groups the member currently belongs to (active memberships only; usually one).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK"),
            @ApiResponse(responseCode = "404", description = "Member not found")
    })
    public ResponseEntity<List<GroupsModuleDto.GroupDto>> getGroupsForMember(
            @Parameter(description = "Member ID. Placeholder: 1", required = true, schema = @Schema(example = "1")) @PathVariable Long memberId) {
        List<Group> groups = membershipService.getGroupsForMember(memberId);
        List<GroupsModuleDto.GroupDto> list = groups.stream()
                .map(GroupsModuleDto.GroupDto::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(list);
    }

    @GetMapping("/{memberId}/memberships")
    @Operation(summary = "Get memberships for member", description = "All memberships (active and ended) for the member. Use for groups and payments after approved.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK"),
            @ApiResponse(responseCode = "404", description = "Member not found")
    })
    public ResponseEntity<List<GroupsModuleDto.MembershipDto>> getMembershipsForMember(
            @Parameter(description = "Member ID. Placeholder: 1", required = true, schema = @Schema(example = "1")) @PathVariable Long memberId) {
        List<Membership> memberships = membershipService.getMembershipsForMember(memberId);
        List<GroupsModuleDto.MembershipDto> list = memberships.stream()
                .map(GroupsModuleDto.MembershipDto::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(list);
    }

    @PostMapping
    @Operation(summary = "Create a new member", description = "Member form after acceptance: CIN from pre-registration; member fills age, profession, region. Group is not set here — add the member to a group later via membership (POST /api/groups/{groupId}/members).")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Member created"),
            @ApiResponse(responseCode = "400", description = "cinNumber required"),
            @ApiResponse(responseCode = "409", description = "CIN number already in use", content = @Content(mediaType = "text/plain", schema = @Schema(example = "CIN number already in use: 12345678")))
    })
    public ResponseEntity<GroupsModuleDto.MemberDto> create(
            @Parameter(description = "CIN number (from pre-registration). Placeholder: 12345678", required = true, schema = @Schema(example = "12345678")) @RequestParam String cinNumber,
            @Parameter(description = "Age (for group suggestions). Placeholder: 28", schema = @Schema(example = "28")) @RequestParam(required = false) Integer age,
            @Parameter(description = "Profession (e.g. student, worker). Placeholder: student", schema = @Schema(example = "student")) @RequestParam(required = false) String profession,
            @Parameter(description = "Region. Placeholder: Tunis", schema = @Schema(example = "Tunis")) @RequestParam(required = false) String region,
            @Parameter(description = "Personalized monthly premium (DT). Placeholder: 17.5", schema = @Schema(example = "17.5")) @RequestParam(required = false) Float personalizedMonthlyPrice,
            @Parameter(description = "Adherence score (0-100). Placeholder: 85", schema = @Schema(example = "85")) @RequestParam(required = false) Float adherenceScore) {
        GroupsModuleDto.MemberDto dto = new GroupsModuleDto.MemberDto(null, cinNumber, age, profession, region, personalizedMonthlyPrice, adherenceScore, null);
        Member member = toEntity(dto);
        Member created = memberService.createMember(member);
        return ResponseEntity.status(HttpStatus.CREATED).body(GroupsModuleDto.MemberDto.fromEntity(created));
    }

    @PutMapping("/{memberId}")
    @Operation(summary = "Update member by ID", description = "Send the fields to update; omitted params keep existing values. CIN is not updatable (set from pre-registration, verified and admin-approved). Use placeholders as a guide.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK – updated member"),
            @ApiResponse(responseCode = "400", description = "No fields to update (send at least one param)"),
            @ApiResponse(responseCode = "404", description = "Member or group not found", content = @Content(mediaType = "text/plain", schema = @Schema(example = "Member not found with id 99")))
    })
    public ResponseEntity<GroupsModuleDto.MemberDto> update(
            @Parameter(description = "Member ID. Placeholder: 1", required = true, schema = @Schema(example = "1")) @PathVariable Long memberId,
            @Parameter(description = "Age. Placeholder: 28", schema = @Schema(example = "28")) @RequestParam(required = false) Integer age,
            @Parameter(description = "Profession. Placeholder: student", schema = @Schema(example = "student")) @RequestParam(required = false) String profession,
            @Parameter(description = "Region. Placeholder: Tunis", schema = @Schema(example = "Tunis")) @RequestParam(required = false) String region,
            @Parameter(description = "Personalized monthly premium (DT). Placeholder: 17.5", schema = @Schema(example = "17.5")) @RequestParam(required = false) Float personalizedMonthlyPrice,
            @Parameter(description = "Adherence score (0-100). Placeholder: 85", schema = @Schema(example = "85")) @RequestParam(required = false) Float adherenceScore,
            @Parameter(description = "Current group ID. Placeholder: 1", schema = @Schema(example = "1")) @RequestParam(required = false) Long currentGroupId) {
        Member existing = memberService.getMemberById(memberId);
        boolean noParamsSent = age == null && profession == null && region == null && personalizedMonthlyPrice == null && adherenceScore == null && currentGroupId == null;
        if (noParamsSent) {
            return ResponseEntity.badRequest().build();
        }
        Integer newAge = age != null ? age : existing.getAge();
        String newProfession = profession != null ? profession : existing.getProfession();
        String newRegion = region != null ? region : existing.getRegion();
        Float newPrice = personalizedMonthlyPrice != null ? personalizedMonthlyPrice : existing.getPersonalizedMonthlyPrice();
        Float newScore = adherenceScore != null ? adherenceScore : existing.getAdherenceScore();
        Long newGroupId = currentGroupId != null ? currentGroupId : (existing.getCurrentGroup() != null ? existing.getCurrentGroup().getId() : null);
        GroupsModuleDto.MemberDto dto = new GroupsModuleDto.MemberDto(memberId, existing.getCinNumber(), newAge, newProfession, newRegion, newPrice, newScore, newGroupId);
        Member member = toEntity(dto);
        memberService.resolveCurrentGroup(member, newGroupId);
        Member updated = memberService.updateMember(memberId, member);
        return ResponseEntity.ok(GroupsModuleDto.MemberDto.fromEntity(updated));
    }

    @DeleteMapping("/{memberId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete member by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "No content"),
            @ApiResponse(responseCode = "404", description = "Member not found", content = @Content(mediaType = "text/plain", schema = @Schema(example = "Member not found with id 99")))
    })
    public void delete(
            @Parameter(description = "Member ID. Placeholder: 1", required = true, schema = @Schema(example = "1")) @PathVariable Long memberId) {
        memberService.deleteMember(memberId);
    }

    @GetMapping("/{memberId}/group-suggestions")
    @Operation(
            summary = "Get group suggestions for this member",
            description = "Uses the member's stored age, profession, and region to return matching groups (same region, compatible type, not full). After member fills the form, call this so they can choose a group, then choose package (BASIC/CONFORT/PREMIUM) and pay.")
    @ApiResponse(responseCode = "200", description = "OK – list of suggested groups")
    public ResponseEntity<List<GroupsModuleDto.GroupDto>> getGroupSuggestionsForMember(
            @Parameter(description = "Member ID", required = true) @PathVariable Long memberId) {
        Member member = memberService.getMemberById(memberId);
        List<Group> groups = groupService.getGroupSuggestions(member.getAge(), member.getProfession(), member.getRegion(), null);
        List<GroupsModuleDto.GroupDto> list = groups.stream()
                .map(GroupsModuleDto.GroupDto::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(list);
    }

    private static Member toEntity(GroupsModuleDto.MemberDto dto) {
        Member m = new Member();
        m.setId(dto.getMemberId());
        m.setCinNumber(dto.getCinNumber());
        m.setAge(dto.getAge());
        m.setProfession(dto.getProfession());
        m.setRegion(dto.getRegion());
        m.setPersonalizedMonthlyPrice(dto.getPersonalizedMonthlyPrice());
        m.setAdherenceScore(dto.getAdherenceScore());
        return m;
    }
}
