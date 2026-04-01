package pi.db.piversionbd.controller.admin;

import jakarta.servlet.http.HttpServletRequest;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pi.db.piversionbd.entities.admin.AdminUser;
import pi.db.piversionbd.service.admin.AdminUserService;
import pi.db.piversionbd.config.JwtTokenProvider;
import pi.db.piversionbd.service.security.RecaptchaVerificationService;

@RestController
@RequestMapping("/api/members/auth")
@RequiredArgsConstructor
public class MemberAuthController {

    private final AdminUserService adminUserService;
    private final JwtTokenProvider jwtTokenProvider;
    private final RecaptchaVerificationService recaptchaVerificationService;

    @PostMapping("/register")
    public ResponseEntity<MemberResponse> register(@RequestBody RegisterRequest request) {
        AdminUser u = request.getMemberId() != null
                ? adminUserService.registerMemberPortalAccount(request.getUsername(), request.getEmail(), request.getPassword(), request.getMemberId())
                : adminUserService.registerWithRole(request.getUsername(), request.getEmail(), request.getPassword(), "MEMBER");
        return ResponseEntity.ok(MemberResponse.from(u));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        try {
            if (request.getUsername() == null || request.getUsername().isBlank()) {
                return ResponseEntity.badRequest().body(error("Username requis"));
            }
            if (request.getPassword() == null || request.getPassword().isBlank()) {
                return ResponseEntity.badRequest().body(error("Mot de passe requis"));
            }
            try {
                recaptchaVerificationService.verifyOrThrow(request.getRecaptchaToken(), ClientIp.from(httpRequest));
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body(error(e.getMessage()));
            } catch (IllegalStateException e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error(e.getMessage()));
            }
            AdminUser u = adminUserService.login(request.getUsername(), request.getPassword());
            String token = jwtTokenProvider.createToken(u.getUsername(), u.getRole());
            return ResponseEntity.ok(AuthResponse.from(u, token));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error("Erreur interne"));
        }
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        try {
            if (request.getEmail() == null || request.getEmail().isBlank()) {
                return ResponseEntity.badRequest().body(error("Email requis"));
            }
            adminUserService.resetPassword(request.getEmail());
            return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error("Erreur interne"));
        }
    }

    private ApiError error(String message) {
        ApiError err = new ApiError();
        err.setMessage(message);
        return err;
    }

    @Data
    public static class RegisterRequest {
        private String username;
        private String email;
        private String password;
        /** When set, links this portal account to the given Member (Option B). */
        private Long memberId;
    }

    @Data
    public static class LoginRequest {
        private String username;
        private String password;
        private String recaptchaToken;
    }

    @Data
    public static class ForgotPasswordRequest {
        private String email;
    }

    @Data
    public static class MemberResponse {
        private Long id;
        private String username;
        private String email;
        private Boolean enabled;
        private String role;
        /** Present when this portal account is linked to a Member (Option B). */
        private Long memberId;

        public static MemberResponse from(AdminUser u) {
            MemberResponse dto = new MemberResponse();
            dto.setId(u.getId());
            dto.setUsername(u.getUsername());
            dto.setEmail(u.getEmail());
            dto.setEnabled(u.getEnabled());
            dto.setRole(u.getRole());
            dto.setMemberId(u.getMember() != null ? u.getMember().getId() : null);
            return dto;
        }
    }

    @Data
    public static class AuthResponse {
        private Long id;
        private String username;
        private String email;
        private Boolean enabled;
        private String role;
        private Long memberId;
        private String token;

        public static AuthResponse from(AdminUser u, String token) {
            AuthResponse dto = new AuthResponse();
            dto.setId(u.getId());
            dto.setUsername(u.getUsername());
            dto.setEmail(u.getEmail());
            dto.setEnabled(u.getEnabled());
            dto.setRole(u.getRole());
            dto.setMemberId(u.getMember() != null ? u.getMember().getId() : null);
            dto.setToken(token);
            return dto;
        }
    }

    @Data
    public static class ApiError {
        private String message;
    }
}
