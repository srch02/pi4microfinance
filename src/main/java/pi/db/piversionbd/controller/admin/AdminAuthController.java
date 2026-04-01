package pi.db.piversionbd.controller.admin;

import jakarta.servlet.http.HttpServletRequest;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pi.db.piversionbd.entities.admin.AdminUser;
import pi.db.piversionbd.service.admin.AdminUserService;
import pi.db.piversionbd.config.JwtTokenProvider;
import pi.db.piversionbd.service.security.RecaptchaVerificationService;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AdminAuthController {

    private final AdminUserService adminUserService;
    private final JwtTokenProvider jwtTokenProvider;
    private final RecaptchaVerificationService recaptchaVerificationService;

    @Value("${recaptcha.enabled:true}")
    private boolean recaptchaEnabled;
    @Value("${recaptcha.site-key:}")
    private String recaptchaSiteKey;

    /** Public config so the admin SPA can load the widget without hardcoding the site key. */
    @GetMapping("/recaptcha-config")
    public ResponseEntity<Map<String, Object>> recaptchaConfig() {
        return ResponseEntity.ok(Map.of(
                "enabled", recaptchaEnabled,
                "siteKey", recaptchaSiteKey != null ? recaptchaSiteKey : ""
        ));
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        try {
            AdminUser user = adminUserService.register(request.getUsername(), request.getEmail(), request.getPassword());
            return ResponseEntity.ok(AdminUserResponse.from(user));
        } catch (IllegalArgumentException e) {
            ApiError err = new ApiError();
            err.setMessage(e.getMessage());
            return ResponseEntity.badRequest().body(err);
        } catch (Exception e) {
            ApiError err = new ApiError();
            err.setMessage("Erreur interne");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(err);
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        try {
            recaptchaVerificationService.verifyOrThrow(request.getRecaptchaToken(), ClientIp.from(httpRequest));
        } catch (IllegalArgumentException e) {
            ApiError err = new ApiError();
            err.setMessage(e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(err);
        } catch (IllegalStateException e) {
            ApiError err = new ApiError();
            err.setMessage(e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(err);
        }
        try {
            AdminUser user = adminUserService.login(request.getUsername(), request.getPassword());
            String token = jwtTokenProvider.createToken(user.getUsername(), user.getRole());
            return ResponseEntity.ok(AuthResponse.from(user, token));
        } catch (IllegalArgumentException | IllegalStateException e) {
            ApiError err = new ApiError();
            err.setMessage(e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(err);
        } catch (Exception e) {
            ApiError err = new ApiError();
            err.setMessage("Erreur interne");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(err);
        }
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
        /** Token from Google reCAPTCHA (g-recaptcha-response or v3 token). */
        private String recaptchaToken;
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

    @Data
    public static class AuthResponse {
        private Long id;
        private String username;
        private String email;
        private String role;
        private Boolean enabled;
        private String token;

        public static AuthResponse from(AdminUser user, String token) {
            AuthResponse dto = new AuthResponse();
            dto.setId(user.getId());
            dto.setUsername(user.getUsername());
            dto.setEmail(user.getEmail());
            dto.setRole(user.getRole());
            dto.setEnabled(user.getEnabled());
            dto.setToken(token);
            return dto;
        }
    }

    @Data
    public static class ApiError {
        private String message;
    }
}

