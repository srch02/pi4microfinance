package pi.db.piversionbd.controller.admin;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pi.db.piversionbd.entities.admin.AdminUser;
import pi.db.piversionbd.service.admin.AdminUserService;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService adminUserService;

    @GetMapping
    public ResponseEntity<List<AdminUser>> getAll() {
        return ResponseEntity.ok(adminUserService.getAllAdminUsers());
    }

    @GetMapping("/{id}")
    public ResponseEntity<AdminUser> getById(@PathVariable Long id) {
        return adminUserService.getAdminUserById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<AdminUser> create(@RequestBody CreateAdminUserRequest request) {
        if (request.getPassword() == null || request.getPassword().isBlank()) {
            throw new IllegalArgumentException("Mot de passe requis");
        }
        AdminUser created = adminUserService.register(request.getUsername(), request.getEmail(), request.getPassword());
        return ResponseEntity.ok(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<AdminUser> update(@PathVariable Long id, @RequestBody AdminUser details) {
        return adminUserService.updateAdminUser(id, details)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        boolean deleted = adminUserService.deleteAdminUser(id);
        return deleted ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    /**
     * Recherche par nom (contient, ignore case) et filtrage par rôle.
     * Ex: /api/admin/users/search?namePart=yas&role=ADMIN
     */
    @GetMapping("/search")
    public ResponseEntity<List<AdminUser>> search(
            @RequestParam(value = "namePart", required = false) String namePart,
            @RequestParam(value = "role", required = false) String role
    ) {
        List<AdminUser> users = adminUserService.searchByNameAndRole(namePart, role);
        return ResponseEntity.ok(users);
    }

    /**
     * Statistiques pour le tableau de bord: total, admins, members, bloqués, nouveaux aujourd'hui.
     */
    @GetMapping("/dashboard")
    public ResponseEntity<java.util.Map<String, Long>> dashboard() {
        return ResponseEntity.ok(adminUserService.dashboardStats());
    }

    /**
     * Déverrouille un compte bloqué (reset des compteurs et de lockedAt).
     */
    @PostMapping("/{id}/unlock")
    public ResponseEntity<Void> unlock(@PathVariable Long id) {
        adminUserService.unlockAccount(id);
        return ResponseEntity.noContent().build();
    }

    private ResponseEntity<?> badRequest(String message) {
        return ResponseEntity.badRequest().body(error(message));
    }

    private ApiError error(String message) {
        ApiError err = new ApiError();
        err.setTimestamp(LocalDateTime.now());
        err.setMessage(message);
        return err;
    }

    @Data
    public static class CreateAdminUserRequest {
        private String username;
        private String email;
        private String password;
    }


    @Data
    public static class AdminUserResponse {
        private Long id;
        private String username;
        private String email;
        private String role;
        private Boolean enabled;
        private String permissions;
        private LocalDateTime lastLogin;

        public static AdminUserResponse from(AdminUser user) {
            AdminUserResponse dto = new AdminUserResponse();
            dto.setId(user.getId());
            dto.setUsername(user.getUsername());
            dto.setEmail(user.getEmail());
            dto.setRole(user.getRole());
            dto.setEnabled(user.getEnabled());
            dto.setPermissions(user.getPermissions());
            dto.setLastLogin(user.getLastLogin());
            return dto;
        }
    }

    @Data
    public static class ApiError {
        private LocalDateTime timestamp;
        private String message;
    }
}
