package pi.db.piversionbd.controller.admin;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pi.db.piversionbd.entities.admin.AdminUser;
import pi.db.piversionbd.service.admin.AdminUserService;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AdminAuthController {

    private final AdminUserService adminUserService;

    @PostMapping("/register")
    public ResponseEntity<AdminUserResponse> register(@RequestBody RegisterRequest request) {
        AdminUser user = adminUserService.register(request.getUsername(), request.getEmail(), request.getPassword());
        return ResponseEntity.ok(AdminUserResponse.from(user));
    }

    @PostMapping("/login")
    public ResponseEntity<AdminUserResponse> login(@RequestBody LoginRequest request) {
        AdminUser user = adminUserService.login(request.getUsername(), request.getPassword());
        return ResponseEntity.ok(AdminUserResponse.from(user));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        adminUserService.resetPassword(request.getEmail());
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @Data
    public static class RegisterRequest {
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

        public static AdminUserResponse from(AdminUser user) {
            AdminUserResponse dto = new AdminUserResponse();
            dto.setId(user.getId());
            dto.setUsername(user.getUsername());
            dto.setEmail(user.getEmail());
            dto.setRole(user.getRole());
            dto.setEnabled(user.getEnabled());
            dto.setPermissions(user.getPermissions());
            return dto;
        }
    }
}

