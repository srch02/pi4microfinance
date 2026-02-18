package pi.db.piversionbd.controller.groups;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pi.db.piversionbd.dto.groups.GroupsModuleDto;
import pi.db.piversionbd.entities.groups.GroupChangeRequest;
import pi.db.piversionbd.service.groups.IGroupChangeRequestService;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/group-change-requests")
@RequiredArgsConstructor
@Tag(name = "Group change requests (admin)", description = "Demands to change group; admin approves so member can then complete the switch.")
public class GroupChangeRequestController {

    private final IGroupChangeRequestService groupChangeRequestService;

    @GetMapping
    @Operation(summary = "List group-change requests by status",
            description = "Admin: list PENDING requests to approve or reject. Optional status filter (default: pending).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK")
    })
    public ResponseEntity<List<GroupsModuleDto.GroupChangeRequestDto>> list(
            @Parameter(description = "Filter by status. Placeholder: pending. Available: pending, approved, rejected, completed",
                    schema = @Schema(allowableValues = {"pending", "approved", "rejected", "completed"}, example = "pending"))
            @RequestParam(required = false, defaultValue = "pending") String status) {
        List<GroupChangeRequest> list = groupChangeRequestService.findByStatus(status);
        List<GroupsModuleDto.GroupChangeRequestDto> dtos = list.stream()
                .map(GroupsModuleDto.GroupChangeRequestDto::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/{requestId}")
    @Operation(summary = "Get group-change request by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK"),
            @ApiResponse(responseCode = "404", description = "Request not found", content = @Content(mediaType = "text/plain"))
    })
    public ResponseEntity<GroupsModuleDto.GroupChangeRequestDto> getById(
            @Parameter(description = "Request ID", required = true, schema = @Schema(example = "1")) @PathVariable Long requestId) {
        GroupChangeRequest r = groupChangeRequestService.getById(requestId);
        return ResponseEntity.ok(GroupsModuleDto.GroupChangeRequestDto.fromEntity(r));
    }

    @PatchMapping("/{requestId}/approve")
    @Operation(summary = "Approve group-change request (admin)",
            description = "After approval, the member can retry adding to the target group; their old membership will be ended and the new one created.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Request approved"),
            @ApiResponse(responseCode = "400", description = "Request is not pending", content = @Content(mediaType = "text/plain")),
            @ApiResponse(responseCode = "404", description = "Request not found", content = @Content(mediaType = "text/plain"))
    })
    public ResponseEntity<GroupsModuleDto.GroupChangeRequestDto> approve(
            @Parameter(description = "Request ID", required = true, schema = @Schema(example = "1")) @PathVariable Long requestId) {
        GroupChangeRequest r = groupChangeRequestService.approve(requestId);
        return ResponseEntity.ok(GroupsModuleDto.GroupChangeRequestDto.fromEntity(r));
    }

    @PatchMapping("/{requestId}/reject")
    @Operation(summary = "Reject group-change request (admin)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Request rejected"),
            @ApiResponse(responseCode = "400", description = "Request is not pending", content = @Content(mediaType = "text/plain")),
            @ApiResponse(responseCode = "404", description = "Request not found", content = @Content(mediaType = "text/plain"))
    })
    public ResponseEntity<GroupsModuleDto.GroupChangeRequestDto> reject(
            @Parameter(description = "Request ID", required = true, schema = @Schema(example = "1")) @PathVariable Long requestId) {
        GroupChangeRequest r = groupChangeRequestService.reject(requestId);
        return ResponseEntity.ok(GroupsModuleDto.GroupChangeRequestDto.fromEntity(r));
    }
}
