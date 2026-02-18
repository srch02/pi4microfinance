package pi.db.piversionbd.service.admin;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import pi.db.piversionbd.entities.admin.AdminUser;
import pi.db.piversionbd.repository.admin.AdminUserRepository;

import java.util.Collections;

@Service
@RequiredArgsConstructor
public class AdminUserDetailsService implements UserDetailsService {

    private final AdminUserRepository adminUserRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        AdminUser user = adminUserRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Utilisateur introuvable: " + username));
        if (Boolean.FALSE.equals(user.getEnabled())) {
            throw new UsernameNotFoundException("Compte désactivé: " + username);
        }
        GrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + (user.getRole() != null ? user.getRole() : "ADMIN"));
        return User.withUsername(user.getUsername())
                .password(user.getPassword())
                .authorities(Collections.singletonList(authority))
                .accountLocked(false)
                .disabled(false)
                .build();
    }
}

