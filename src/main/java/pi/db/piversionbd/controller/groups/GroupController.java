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
import pi.db.piversionbd.repository.groups.GroupPoolRepository;
import pi.db.piversionbd.service.groups.IMembershipService.AddMembershipResult;
import pi.db.piversionbd.service.groups.IGroupService;
import pi.db.piversionbd.service.groups.IMembershipService;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/groups")
@RequiredArgsConstructor
@Tag(name = "Module 1 – Groups & Memberships", description = "Groups CRUD; membership operations (add/end member in group)")
public class GroupController {

    private final IGroupService groupService;
    private final IMembershipService membershipService;
    private final GroupPoolRepository groupPoolRepository;

    @GetMapping
    @Operation(summary = "List all groups", description = "Optional filter by type (query param with dropdown).")
    @ApiResponse(responseCode = "200", description = "OK")
    public ResponseEntity<List<GroupsModuleDto.GroupDto>> getAll(
            @Parameter(description = "Filter by group type. Placeholder: FAMILY. Available values: FAMILY, STUDENTS, WORKERS, MIXED", schema = @Schema(allowableValues = { "FAMILY", "STUDENTS", "WORKERS", "MIXED" }, example = "FAMILY"))
            @RequestParam(required = false) String type) {
        List<Group> groups = type != null && !type.isBlank() ? groupService.getAllGroups(type) : groupService.getAllGroups();
        List<GroupsModuleDto.GroupDto> list = groups.stream()
                .map(GroupsModuleDto.GroupDto::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(list);
    }

    @GetMapping("/{groupId}")
    @Operation(summary = "Get group by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK"),
            @ApiResponse(responseCode = "404", description = "Group not found", content = @Content(mediaType = "text/plain", schema = @Schema(example = "Group not found with id 99")))
    })
    public ResponseEntity<GroupsModuleDto.GroupDto> getById(
            @Parameter(description = "Group ID. Placeholder: 1", required = true, schema = @Schema(example = "1")) @PathVariable Long groupId) {
        Group group = groupService.getGroupById(groupId);
        return ResponseEntity.ok(GroupsModuleDto.GroupDto.fromEntity(group));
    }

    @GetMapping("/{groupId}/pool")
    @Operation(summary = "Get group pool", description = "Returns the group's solidarity pool: exact balance, total contributions, total paid out, and when it was last updated (e.g. when a payment was applied).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK – pool data (amounts and updatedAt)"),
            @ApiResponse(responseCode = "404", description = "Group not found", content = @Content(mediaType = "text/plain"))
    })
    public ResponseEntity<GroupsModuleDto.GroupPoolDto> getPool(
            @Parameter(description = "Group ID. Placeholder: 1", required = true, schema = @Schema(example = "1")) @PathVariable Long groupId) {
        groupService.getGroupById(groupId); // 404 if group missing
        GroupsModuleDto.GroupPoolDto dto = groupPoolRepository.findByGroup_Id(groupId)
                .map(GroupsModuleDto.GroupPoolDto::fromEntity)
                .orElse(GroupsModuleDto.GroupPoolDto.empty(groupId));
        return ResponseEntity.ok(dto);
    }

    @PostMapping
    @Operation(
            summary = "Create a new group",
            description = "Creates a public or private group. Private groups get an invite_code that the creator can share (QR). currentMemberCount is managed automatically.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Group created"),
            @ApiResponse(responseCode = "400", description = "name required")
    })
    public ResponseEntity<GroupsModuleDto.GroupDto> create(
            @Parameter(
                    description = "Group name. Placeholder: Family Solidarity 42",
                    required = true,
                    schema = @Schema(example = "Family Solidarity 42"))
            @RequestParam String name,
            @Parameter(
                    description = "Group type. Placeholder: FAMILY. Available values: FAMILY, STUDENTS, WORKERS, MIXED",
                    schema = @Schema(allowableValues = {"FAMILY", "STUDENTS", "WORKERS", "MIXED"}, example = "FAMILY"))
            @RequestParam(required = false) String type,
            @Parameter(
                    description = "Region / city. Placeholder: Tunis",
                    schema = @Schema(example = "Tunis"))
            @RequestParam(required = false) String region,
            @Parameter(
                    description = "Minimum members. Placeholder: 5",
                    schema = @Schema(example = "5"))
            @RequestParam(required = false) Integer minMembers,
            @Parameter(
                    description = "Maximum members. Placeholder: 15",
                    schema = @Schema(example = "15"))
            @RequestParam(required = false) Integer maxMembers,
            @Parameter(
                    description = "Join policy. Placeholder: public. Available values: public, private",
                    schema = @Schema(allowableValues = {"public", "private"}, example = "public"))
            @RequestParam(required = false) String joinPolicy) {

        Group group = new Group();
        group.setName(name);
        group.setType(type);
        group.setRegion(region);
        group.setMinMembers(minMembers);
        group.setMaxMembers(maxMembers);
        group.setJoinPolicy(joinPolicy);

        Group created = groupService.createGroup(group);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(GroupsModuleDto.GroupDto.fromEntity(created));
    }

    @PutMapping("/{groupId}")
    @Operation(summary = "Update group by ID", description = "Send the fields to update; omitted params keep existing values. Use placeholders as a guide.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK – updated group"),
            @ApiResponse(responseCode = "400", description = "No fields to update (send at least one param)"),
            @ApiResponse(responseCode = "404", description = "Group not found", content = @Content(mediaType = "text/plain", schema = @Schema(example = "Group not found with id 99")))
    })
    public ResponseEntity<GroupsModuleDto.GroupDto> update(
            @Parameter(description = "Group ID. Placeholder: 1", required = true, schema = @Schema(example = "1"))
            @PathVariable Long groupId,
            @Parameter(description = "Group name. Placeholder: Family Solidarity 42", schema = @Schema(example = "Family Solidarity 42"))
            @RequestParam(required = false) String name,
            @Parameter(description = "Group type. Placeholder: FAMILY. Available values: FAMILY, STUDENTS, WORKERS, MIXED",
                    schema = @Schema(allowableValues = {"FAMILY", "STUDENTS", "WORKERS", "MIXED"}, example = "FAMILY"))
            @RequestParam(required = false) String type,
            @Parameter(description = "Region / city. Placeholder: Tunis", schema = @Schema(example = "Tunis"))
            @RequestParam(required = false) String region,
            @Parameter(description = "Minimum members. Placeholder: 5", schema = @Schema(example = "5"))
            @RequestParam(required = false) Integer minMembers,
            @Parameter(description = "Maximum members. Placeholder: 15", schema = @Schema(example = "15"))
            @RequestParam(required = false) Integer maxMembers,
            @Parameter(description = "Join policy. Placeholder: public. Available values: public, private",
                    schema = @Schema(allowableValues = {"public", "private"}, example = "public"))
            @RequestParam(required = false) String joinPolicy) {

        boolean noParamsSent = name == null && type == null && region == null
                && minMembers == null && maxMembers == null && joinPolicy == null;
        if (noParamsSent) {
            return ResponseEntity.badRequest().build();
        }

        Group updated = new Group();
        updated.setName(name);
        updated.setType(type);
        updated.setRegion(region);
        updated.setMinMembers(minMembers);
        updated.setMaxMembers(maxMembers);
        updated.setJoinPolicy(joinPolicy);

        Group group = groupService.updateGroup(groupId, updated);
        return ResponseEntity.ok(GroupsModuleDto.GroupDto.fromEntity(group));
    }

    @DeleteMapping("/{groupId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete group by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "No content"),
            @ApiResponse(responseCode = "404", description = "Group not found", content = @Content(mediaType = "text/plain", schema = @Schema(example = "Group not found with id 99")))
    })
    public void delete(
            @Parameter(description = "Group ID. Placeholder: 1", required = true, schema = @Schema(example = "1")) @PathVariable Long groupId) {
        groupService.deleteGroup(groupId);
    }

    // ----- Membership operations (groups & payments after member is approved) -----

    @PostMapping("/{groupId}/members")
    @Operation(summary = "Create membership – add member to group",
            description = "Creates a membership, or a group-change request if the member is already in another group (admin must approve; then member retries). "
                    + "Use groupId (path), memberId and packageType (query).")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Membership created"),
            @ApiResponse(responseCode = "202", description = "Member already in another group: group-change request created; wait for admin approval, then retry"),
            @ApiResponse(responseCode = "400", description = "memberId required, member already in group, or pending request already exists"),
            @ApiResponse(responseCode = "404", description = "Group or member not found")
    })
    public ResponseEntity<?> createMembership(
            @Parameter(description = "Group ID. Placeholder: 1", required = true, schema = @Schema(example = "1")) @PathVariable Long groupId,
            @Parameter(description = "Member ID (current member). Placeholder: 3", required = true, schema = @Schema(example = "3")) @RequestParam Long memberId,
            @Parameter(description = "Package type. Placeholder: BASIC. Available values: BASIC, CONFORT, PREMIUM", schema = @Schema(allowableValues = { "BASIC", "CONFORT", "PREMIUM" }, example = "BASIC")) @RequestParam(required = false) String packageType) {
        AddMembershipResult result = membershipService.addMemberToGroup(groupId, memberId, packageType);
        if (result.getMembership() != null) {
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(GroupsModuleDto.MembershipDto.fromEntity(result.getMembership()));
        }
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(GroupsModuleDto.GroupChangeRequestDto.fromEntity(result.getGroupChangeRequest()));
    }

    @DeleteMapping("/{groupId}/members/{memberId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "End membership – remove member from group", description = "Ends the membership (soft delete). Admin or self.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Membership ended"),
            @ApiResponse(responseCode = "404", description = "Group, member, or active membership not found")
    })
    public void endMembership(
            @Parameter(description = "Group ID. Placeholder: 1", required = true, schema = @Schema(example = "1")) @PathVariable Long groupId,
            @Parameter(description = "Member ID. Placeholder: 3", required = true, schema = @Schema(example = "3")) @PathVariable Long memberId) {
        membershipService.removeMemberFromGroup(groupId, memberId);
    }

    // No toEntity helper needed; create/update build Group directly from request params.
}
