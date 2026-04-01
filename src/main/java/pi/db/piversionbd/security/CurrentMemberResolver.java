package pi.db.piversionbd.security;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import pi.db.piversionbd.entities.admin.AdminUser;
import pi.db.piversionbd.repository.admin.AdminUserRepository;

import java.util.Optional;

/**
 * Resolves the insurance {@link pi.db.piversionbd.entities.groups.Member} id for the logged-in portal user
 * (JWT subject = {@link AdminUser#getUsername()}, linked via {@link AdminUser#getMember()}).
 */
@Component
@RequiredArgsConstructor
public class CurrentMemberResolver {

    private final AdminUserRepository adminUserRepository;

    public boolean isAdmin(Authentication auth) {
        return hasAuthority(auth, "ROLE_ADMIN");
    }

    public boolean isMemberRole(Authentication auth) {
        return hasAuthority(auth, "ROLE_MEMBER");
    }

    private static boolean hasAuthority(Authentication auth, String role) {
        if (auth == null || auth.getAuthorities() == null) {
            return false;
        }
        for (GrantedAuthority a : auth.getAuthorities()) {
            if (role.equals(a.getAuthority())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Member id for the current portal account, if the user is linked to a {@link pi.db.piversionbd.entities.groups.Member}.
     */
    public Optional<Long> resolveMemberId(Authentication auth) {
        if (auth == null || auth.getPrincipal() == null) {
            return Optional.empty();
        }
        String username = auth.getPrincipal().toString();
        return adminUserRepository.findByUsername(username)
                .map(AdminUser::getMember)
                .filter(m -> m != null && m.getId() != null)
                .map(m -> m.getId());
    }

    public long requireMemberId(Authentication auth) {
        return resolveMemberId(auth).orElseThrow(() -> new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "No member profile linked to this account."));
    }

    /**
     * Members may only access their own memberId; admins may access any.
     */
    public void assertMemberOwnsMemberIdOrIsAdmin(Authentication auth, Long memberId) {
        if (memberId == null) {
            return;
        }
        if (isAdmin(auth)) {
            return;
        }
        if (isMemberRole(auth)) {
            long mine = requireMemberId(auth);
            if (memberId == null || mine != memberId) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not allowed to access another member's data.");
            }
        }
    }
}
