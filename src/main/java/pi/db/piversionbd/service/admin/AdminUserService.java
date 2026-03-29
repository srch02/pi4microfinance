package pi.db.piversionbd.service.admin;

import org.springframework.stereotype.Service;
import pi.db.piversionbd.entities.admin.AdminUser;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class AdminUserService {

    private final Map<Long, AdminUser> store = new ConcurrentHashMap<>();
    private final AtomicLong seq = new AtomicLong(1);

    public List<AdminUser> getAllAdminUsers() {
        return new ArrayList<>(store.values());
    }

    public Optional<AdminUser> getAdminUserById(Long id) {
        return Optional.ofNullable(store.get(id));
    }

    public AdminUser register(String username, String email, String password) {
        AdminUser user = new AdminUser();
        user.setId(seq.getAndIncrement());
        user.setUsername(username);
        user.setEmail(email);
        user.setRole("ADMIN");
        user.setPermissions("ALL");
        store.put(user.getId(), user);
        return user;
    }

    public Optional<AdminUser> updateAdminUser(Long id, AdminUser details) {
        AdminUser existing = store.get(id);
        if (existing == null) {
            return Optional.empty();
        }
        if (details.getUsername() != null) existing.setUsername(details.getUsername());
        if (details.getEmail() != null) existing.setEmail(details.getEmail());
        if (details.getRole() != null) existing.setRole(details.getRole());
        if (details.getPermissions() != null) existing.setPermissions(details.getPermissions());
        return Optional.of(existing);
    }

    public boolean deleteAdminUser(Long id) {
        return store.remove(id) != null;
    }

    public List<AdminUser> searchByNameAndRole(String namePart, String role) {
        String np = namePart == null ? "" : namePart.toLowerCase();
        String r = role == null ? "" : role.toLowerCase();
        return store.values().stream()
            .filter(u -> np.isBlank() || (u.getUsername() != null && u.getUsername().toLowerCase().contains(np)))
            .filter(u -> r.isBlank() || (u.getRole() != null && u.getRole().toLowerCase().equals(r)))
            .toList();
    }

    public Map<String, Long> dashboardStats() {
        long total = store.size();
        long admins = store.values().stream().filter(u -> "ADMIN".equalsIgnoreCase(u.getRole())).count();
        long members = store.values().stream().filter(u -> "MEMBER".equalsIgnoreCase(u.getRole())).count();
        return Map.of(
            "total", total,
            "admins", admins,
            "members", members,
            "blocked", 0L,
            "newToday", 0L
        );
    }

    public void unlockAccount(Long id) {
        // no-op for minimal compatibility service
    }
}
