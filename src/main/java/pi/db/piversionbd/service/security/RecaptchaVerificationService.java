package pi.db.piversionbd.service.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

/**
 * Server-side verification of Google reCAPTCHA tokens (v2 checkbox / invisible or v3).
 * Docs: https://developers.google.com/recaptcha/docs/verify
 */
@Service
public class RecaptchaVerificationService {

    private static final Logger log = LoggerFactory.getLogger(RecaptchaVerificationService.class);
    private static final URI SITE_VERIFY = URI.create("https://www.google.com/recaptcha/api/siteverify");
    /** Local mapper — Spring Boot 4 may not expose {@code ObjectMapper} as a bean in all setups. */
    private static final ObjectMapper JSON = new ObjectMapper();

    private final boolean enabled;
    private final String secretKey;
    /** Used only when Google returns a {@code score} (reCAPTCHA v3). */
    private final double minScore;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    public RecaptchaVerificationService(
            @Value("${recaptcha.enabled:true}") boolean enabled,
            @Value("${recaptcha.secret-key:}") String secretKey,
            @Value("${recaptcha.min-score:0.5}") double minScore
    ) {
        this.enabled = enabled;
        this.secretKey = secretKey == null ? "" : secretKey.trim();
        this.minScore = minScore;
    }

    /**
     * @param recaptchaToken value from the frontend (g-recaptcha-response or v3 token)
     * @param remoteIp       client IP if available (optional but recommended)
     */
    public void verifyOrThrow(String recaptchaToken, String remoteIp) {
        if (!enabled) {
            return;
        }
        if (secretKey.isBlank()) {
            log.warn("recaptcha.enabled=true but recaptcha.secret-key is empty; skipping verification");
            return;
        }
        if (recaptchaToken == null || recaptchaToken.isBlank()) {
            throw new IllegalArgumentException("Captcha requis");
        }
        try {
            StringBuilder form = new StringBuilder();
            form.append("secret=").append(enc(secretKey))
                    .append("&response=").append(enc(recaptchaToken.trim()));
            if (remoteIp != null && !remoteIp.isBlank()) {
                form.append("&remoteip=").append(enc(remoteIp.trim()));
            }

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(SITE_VERIFY)
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(form.toString()))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) {
                log.warn("reCAPTCHA siteverify HTTP {} body={}", response.statusCode(), response.body());
                throw new IllegalStateException("Impossible de vérifier le captcha. Réessayez plus tard.");
            }

            JsonNode root = JSON.readTree(response.body());
            if (!root.path("success").asBoolean(false)) {
                log.debug("reCAPTCHA rejected: {}", response.body());
                throw new IllegalArgumentException("Captcha invalide ou expiré. Réessayez.");
            }
            // v3 returns a score; v2 does not — only enforce when present
            if (root.hasNonNull("score")) {
                double score = root.get("score").asDouble();
                if (score < minScore) {
                    log.debug("reCAPTCHA score too low: {}", score);
                    throw new IllegalArgumentException("Captcha invalide ou expiré. Réessayez.");
                }
            }
        } catch (IllegalArgumentException | IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            log.warn("reCAPTCHA verify error: {}", e.getMessage());
            throw new IllegalStateException("Impossible de vérifier le captcha. Réessayez plus tard.");
        }
    }

    private static String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }
}
