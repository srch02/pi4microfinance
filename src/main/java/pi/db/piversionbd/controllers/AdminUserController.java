package pi.db.piversionbd.controllers;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pi.db.piversionbd.entities.admin.AdminUser;
import pi.db.piversionbd.services.AdminUserService;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService adminUserService;

    // ✅ Get all admin users
    @GetMapping
    public ResponseEntity<List<AdminUser>> getAll() {
        return ResponseEntity.ok(adminUserService.getAllAdminUsers());
    }

    // ✅ Get admin user by ID
    @GetMapping("/{id}")
    public ResponseEntity<AdminUser> getById(@PathVariable Long id) {
        return adminUserService.getAdminUserById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // ✅ Create a new admin user
    @PostMapping
    public ResponseEntity<AdminUser> create(@RequestBody CreateAdminUserRequest request) {
        if (request.getPassword() == null || request.getPassword().isBlank()) {
            throw new IllegalArgumentException("Mot de passe requis");
        }
        AdminUser created = adminUserService.register(request.getUsername(), request.getEmail(), request.getPassword());
        return ResponseEntity.ok(created);
    }

    // ✅ Update an existing admin user
    @PutMapping("/{id}")
    public ResponseEntity<AdminUser> update(@PathVariable Long id, @RequestBody AdminUser details) {
        return adminUserService.updateAdminUser(id, details)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // ✅ Delete an admin user
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        boolean deleted = adminUserService.deleteAdminUser(id);
        return deleted ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    /**
     * Authentifie un admin par username + password.
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            if (request.getUsername() == null || request.getUsername().isBlank()) {
                return badRequest("Username requis");
            }
            if (request.getPassword() == null || request.getPassword().isBlank()) {
                return badRequest("Mot de passe requis");
            }
            AdminUser user = adminUserService.login(request.getUsername(), request.getPassword());
            return ResponseEntity.ok(AdminUserResponse.from(user));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error("Erreur interne"));
        }
    }

    /**
     * Réinitialise le mot de passe d'un admin via son email.
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        try {
            if (request.getEmail() == null || request.getEmail().isBlank()) {
                return badRequest("Email requis");
            }
            adminUserService.resetPassword(request.getEmail());
            return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error("Erreur interne"));
        }
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
    public static class LoginRequest {
        private String username;
        private String password;
    }

    @Data
    public static class ForgotPasswordRequest {
        private String email;
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
