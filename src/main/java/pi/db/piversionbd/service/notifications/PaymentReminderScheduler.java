package pi.db.piversionbd.service.notifications;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import pi.db.piversionbd.entities.groups.Member;
import pi.db.piversionbd.entities.groups.Membership;
import pi.db.piversionbd.entities.groups.Payment;
import pi.db.piversionbd.repository.groups.MembershipRepository;
import pi.db.piversionbd.repository.groups.PaymentRepository;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

@Component
@RequiredArgsConstructor
public class PaymentReminderScheduler {

    private final MembershipRepository membershipRepository;
    private final PaymentRepository paymentRepository;
    private final TelegramNotificationService telegramNotificationService;

    /**
     * Runs every day at 09:00 server time.
     * Sends a reminder if last payment is older than 30 days.
     */
    @Scheduled(cron = "0 0 9 * * *")
    public void remindLatePayments() {
        List<Membership> active = membershipRepository.findByStatusIgnoreCaseAndEndedAtIsNull(Membership.STATUS_ACTIVE);
        Instant now = Instant.now();

        for (Membership m : active) {
            Member member = m.getMember();
            if (member == null) continue;
            String chatId = member.getTelegramChatId();
            if (chatId == null || chatId.isBlank()) continue;

            Payment last = paymentRepository.findTopByMember_IdAndGroup_IdOrderByCreatedAtDesc(
                    member.getId(),
                    m.getGroup() != null ? m.getGroup().getId() : null
            ).orElse(null);

            if (last == null || last.getCreatedAt() == null) continue;

            Instant lastPaymentAt = last.getCreatedAt().atZone(ZoneId.systemDefault()).toInstant();
            long days = Duration.between(lastPaymentAt, now).toDays();
            if (days < 30) continue;

            // Anti-spam: at most one reminder per 3 days.
            Instant lastReminder = m.getLastPaymentReminderAt();
            if (lastReminder != null && Duration.between(lastReminder, now).toDays() < 3) {
                continue;
            }

            String groupName = m.getGroup() != null ? m.getGroup().getName() : null;
            float monthly = m.getMonthlyAmount() != null ? m.getMonthlyAmount() : 0f;
            String msg = TelegramMemberMessages.buildPaymentReminderMessage(groupName, monthly);
            telegramNotificationService.sendMessage(chatId, msg);

            m.setLastPaymentReminderAt(now);
            membershipRepository.save(m);
        }
    }
}

