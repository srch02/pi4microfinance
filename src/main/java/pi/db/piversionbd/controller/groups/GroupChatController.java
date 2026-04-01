package pi.db.piversionbd.controller.groups;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pi.db.piversionbd.dto.groups.directchat.DirectChatDto;
import pi.db.piversionbd.entities.groups.Conversation;
import pi.db.piversionbd.entities.groups.DirectMessage;
import pi.db.piversionbd.entities.groups.Member;
import pi.db.piversionbd.entities.admin.AdminUser;
import pi.db.piversionbd.repository.admin.AdminUserRepository;
import pi.db.piversionbd.service.groups.IDirectChatService;
import pi.db.piversionbd.service.security.AesEncryptionService;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/groups/{groupId}/chat")
@RequiredArgsConstructor
@Tag(name = "Module 1 – Direct Chat", description = "1-to-1 encrypted direct messaging inside a group with Hedera audit trail.")
@SecurityRequirement(name = "bearerAuth")
public class GroupChatController {

    private static final Logger log = LoggerFactory.getLogger(GroupChatController.class);

    private final IDirectChatService directChatService;
    private final AesEncryptionService aesEncryptionService;
    private final AdminUserRepository adminUserRepository;

    @PostMapping("/direct/{otherMemberId}/messages")
    @Operation(
            summary = "Send a direct (1-to-1) message",
            description = "Creates/opens a 1-to-1 conversation inside the same group and sends an encrypted message. Participants can read plaintext; admin cannot."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Message created"),
            @ApiResponse(responseCode = "403", description = "Not allowed (admin or unauthenticated)"),
            @ApiResponse(responseCode = "404", description = "Group, receiver, or membership not found")
    })
    public ResponseEntity<DirectChatDto.DirectMessageDto> sendDirectMessage(
            @Parameter(description = "Group ID. Placeholder: 3", required = true, schema = @Schema(example = "3"))
            @PathVariable Long groupId,
            @Parameter(description = "Other member ID (receiver). Placeholder: 15", required = true, schema = @Schema(example = "15"))
            @PathVariable Long otherMemberId,
            @Parameter(description = "Plaintext message content (will be encrypted server-side).", required = true, schema = @Schema(example = "Hey file a claim for your back pain."))
            @RequestParam String content
    ) {
        var current = getCurrentMemberOrNull();
        if (!current.authenticated) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        if (current.isAdmin) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
        }
        if (current.memberId == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        DirectMessage msg = directChatService.sendMessage(groupId, current.memberId, otherMemberId, content);
        String decrypted = aesSafeDecrypt(msg.getEncryptedContent());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(DirectChatDto.DirectMessageDto.fromEntity(msg, decrypted));
    }

    @GetMapping("/conversations")
    @Operation(summary = "List my direct conversations in a group")
    @ApiResponse(responseCode = "200", description = "OK")
    public ResponseEntity<List<DirectChatDto.ConversationDto>> listConversations(
            @PathVariable Long groupId
    ) {
        var current = getCurrentMemberOrNull();
        if (!current.authenticated) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        if (current.isAdmin) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        if (current.memberId == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        List<Conversation> convs = directChatService.getConversationsForMember(groupId, current.memberId);

        List<DirectChatDto.ConversationDto> dtos = convs.stream().map(c -> {
            boolean requesterIsA = c.getMemberA() != null && c.getMemberA().getId() != null && c.getMemberA().getId().equals(current.memberId);
            Member other = requesterIsA ? c.getMemberB() : c.getMemberA();
            return new DirectChatDto.ConversationDto(
                    c.getId(),
                    groupId,
                    other != null ? other.getId() : null,
                    other != null ? other.getCinNumber() : null,
                    c.getCreatedAt()
            );
        }).collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/conversations/{conversationId}/messages")
    @Operation(summary = "List messages in a direct conversation")
    @ApiResponse(responseCode = "200", description = "OK")
    public ResponseEntity<List<DirectChatDto.DirectMessageDto>> listConversationMessages(
            @PathVariable Long groupId,
            @PathVariable Long conversationId
    ) {
        var current = getCurrentMemberOrNull();
        if (!current.authenticated) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        if (current.isAdmin) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        if (current.memberId == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        List<DirectMessage> msgs = directChatService.getMessagesForConversation(groupId, conversationId, current.memberId);
        List<DirectChatDto.DirectMessageDto> dtos = msgs.stream()
                .map(m -> DirectChatDto.DirectMessageDto.fromEntity(m, aesSafeDecrypt(m.getEncryptedContent())))
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    private String aesSafeDecrypt(String encrypted) {
        try {
            return aesEncryptionService.decrypt(encrypted);
        } catch (Exception ex) {
            return encrypted;
        }
    }

    private CurrentUser getCurrentMemberOrNull() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null) {
            log.debug("DirectChat: no auth principal found (auth is null or principal is null)");
            return new CurrentUser(false, null, false);
        }
        String username = auth.getPrincipal().toString();
        boolean isAdmin = auth.getAuthorities() != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority() != null && a.getAuthority().equals("ROLE_ADMIN"));

        AdminUser au = adminUserRepository.findByUsername(username).orElse(null);
        Long memberId = au != null && au.getMember() != null ? au.getMember().getId() : null;

        log.debug(
                "DirectChat auth resolved: username={}, isAdmin={}, memberId={}, authorities={}",
                username,
                isAdmin,
                memberId,
                auth.getAuthorities() != null ? auth.getAuthorities().toString() : "[]"
        );
        return new CurrentUser(isAdmin, memberId, true);
    }

    private static class CurrentUser {
        private final boolean isAdmin;
        private final Long memberId;
        private final boolean authenticated;

        private CurrentUser(boolean isAdmin, Long memberId, boolean authenticated) {
            this.isAdmin = isAdmin;
            this.memberId = memberId;
            this.authenticated = authenticated;
        }
    }
}

