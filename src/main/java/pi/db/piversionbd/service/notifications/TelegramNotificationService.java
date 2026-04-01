package pi.db.piversionbd.service.notifications;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import pi.db.piversionbd.entities.groups.Member;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

@Service
public class TelegramNotificationService {
    private static final Logger log = LoggerFactory.getLogger(TelegramNotificationService.class);

    private final boolean enabled;
    private final String botToken;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public TelegramNotificationService(
            @Value("${telegram.bot.enabled:false}") boolean enabled,
            @Value("${telegram.bot.token:}") String botToken
    ) {
        this.enabled = enabled;
        this.botToken = botToken == null ? "" : botToken.trim();
    }

    /** Welcome text with package prices and rules; skipped if Telegram not linked. */
    public void sendWelcomeForNewMember(Member member) {
        if (member == null || member.getTelegramChatId() == null || member.getTelegramChatId().isBlank()) {
            return;
        }
        sendMessage(member.getTelegramChatId(), TelegramMemberMessages.buildWelcomeMessage(member));
    }

    public void sendMessage(String chatId, String text) {
        if (!enabled) {
            log.debug("Telegram disabled; skipping notification");
            return;
        }
        if (botToken.isBlank()) {
            log.warn("Telegram token is empty; skipping notification");
            return;
        }
        if (chatId == null || chatId.isBlank()) {
            log.debug("Telegram chatId missing; skipping notification");
            return;
        }
        if (text == null || text.isBlank()) {
            log.debug("Telegram message text empty; skipping notification");
            return;
        }

        try {
            String body = "chat_id=" + url(chatId.trim())
                    + "&text=" + url(text.trim());

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.telegram.org/bot" + botToken + "/sendMessage"))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 == 2) {
                log.info("Telegram notification sent to chatId={}", chatId);
            } else {
                log.warn("Telegram send failed status={} body={}", response.statusCode(), response.body());
            }
        } catch (Exception e) {
            // Best-effort notifications: do not fail business flows.
            log.warn("Telegram send exception: {}", e.getMessage(), e);
        }
    }

    private static String url(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }
}

