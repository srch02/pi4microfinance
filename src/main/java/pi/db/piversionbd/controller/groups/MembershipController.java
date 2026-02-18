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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pi.db.piversionbd.dto.groups.GroupsModuleDto;
import pi.db.piversionbd.service.groups.IMembershipService.AddMembershipResult;
import pi.db.piversionbd.service.groups.IMembershipService;

@RestController
@RequestMapping("/api/memberships")
@RequiredArgsConstructor
@Tag(name = "Memberships", description = "Membership operations (join private groups by invite code / QR).")
public class MembershipController {

    private final IMembershipService membershipService;

    @PostMapping("/join-by-invite")
    @Operation(
            summary = "Join private group by invite code (QR)",
            description = "Use the inviteCode from a private group. Creates a pending membership, or a group-change request if the member is already in another group (admin approves, then member retries).")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Membership created (pending)"),
            @ApiResponse(responseCode = "202", description = "Member already in another group: group-change request created; wait for admin approval, then retry"),
            @ApiResponse(responseCode = "400", description = "Invalid input or pending request already exists", content = @Content(mediaType = "text/plain")),
            @ApiResponse(responseCode = "404", description = "Invite code or member not found", content = @Content(mediaType = "text/plain"))
    })
    public ResponseEntity<?> joinByInvite(
            @Parameter(
                    description = "Invite code from the private group (QR content). Placeholder: a1b2c3d4e5f6",
                    required = true,
                    schema = @Schema(example = "a1b2c3d4e5f6"))
            @RequestParam String inviteCode,
            @Parameter(
                    description = "Member ID that wants to join the private group. Placeholder: 1",
                    required = true,
                    schema = @Schema(example = "1"))
            @RequestParam Long memberId,
            @Parameter(
                    description = "Package type for the membership. Placeholder: BASIC. Available values: BASIC, CONFORT, PREMIUM. "
                            + "If omitted, defaults to BASIC or member's personalized price.",
                    schema = @Schema(allowableValues = {"BASIC", "CONFORT", "PREMIUM"}, example = "BASIC"))
            @RequestParam(required = false) String packageType) {

        AddMembershipResult result = membershipService.addMemberToGroupByInviteCode(inviteCode, memberId, packageType);
        if (result.getMembership() != null) {
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(GroupsModuleDto.MembershipDto.fromEntity(result.getMembership()));
        }
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(GroupsModuleDto.GroupChangeRequestDto.fromEntity(result.getGroupChangeRequest()));
    }
}

