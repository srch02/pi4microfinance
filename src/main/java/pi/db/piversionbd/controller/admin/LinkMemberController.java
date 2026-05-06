package pi.db.piversionbd.controller.admin;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pi.db.piversionbd.entities.admin.AdminUser;
import pi.db.piversionbd.entities.groups.Member;
import pi.db.piversionbd.repository.admin.AdminUserRepository;
import pi.db.piversionbd.repository.groups.MemberRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/admin/link-members")
@RequiredArgsConstructor
public class LinkMemberController {

    private final AdminUserRepository adminUserRepository;
    private final MemberRepository memberRepository;

    /**
     * POST /api/admin/link-members/auto
     * Links all AdminUsers (role=MEMBER) that have member_id=null
     * to a Member with the same email.
     */
    @PostMapping("/auto")
    public ResponseEntity<Map<String, Object>> autoLinkAll() {
        List<AdminUser> unlinked = adminUserRepository.findAll().stream()
                .filter(u -> u.getMember() == null && "MEMBER".equalsIgnoreCase(u.getRole()))
                .toList();

        List<String> linked = new ArrayList<>();
        List<String> notFound = new ArrayList<>();

        for (AdminUser user : unlinked) {
            Optional<Member> member = memberRepository.findByEmail(user.getEmail());
            if (member.isPresent()) {
                user.setMember(member.get());
                adminUserRepository.save(user);
                linked.add(user.getUsername() + " → Member#" + member.get().getId());
            } else {
                notFound.add(user.getUsername() + " (email: " + user.getEmail() + ")");
            }
        }

        return ResponseEntity.ok(Map.of(
                "linked", linked,
                "notFound", notFound,
                "totalLinked", linked.size(),
                "totalNotFound", notFound.size()
        ));
    }

    /**
     * POST /api/admin/link-members/manual?userId=3&memberId=5
     * Manually links a specific AdminUser to a specific Member.
     */
    @PostMapping("/manual")
    public ResponseEntity<Map<String, Object>> manualLink(
            @RequestParam Long userId,
            @RequestParam Long memberId
    ) {
        AdminUser user = adminUserRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("AdminUser not found: " + userId));
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("Member not found: " + memberId));

        user.setMember(member);
        adminUserRepository.save(user);

        return ResponseEntity.ok(Map.of(
                "message", "Linked successfully",
                "userId", userId,
                "memberId", memberId,
                "username", user.getUsername(),
                "memberEmail", member.getEmail() != null ? member.getEmail() : "N/A"
        ));
    }
}
