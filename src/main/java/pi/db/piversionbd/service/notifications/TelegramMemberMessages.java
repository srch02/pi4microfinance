package pi.db.piversionbd.service.notifications;

import pi.db.piversionbd.entities.groups.Member;

/**
 * Long-form Telegram texts for member onboarding and reminders (Telegram limit ~4096 chars).
 */
public final class TelegramMemberMessages {

    private TelegramMemberMessages() {}

    public static String buildWelcomeMessage(Member member) {
        String basic = formatDt(member.getPriceBasic());
        String confort = formatDt(member.getPriceConfort());
        String premium = formatDt(member.getPricePremium());
        String cin = member.getCinNumber() != null ? member.getCinNumber() : "—";

        return """
                Welcome to SolidariHealth!

                Member ID: %d | CIN: %s

                Your personalized monthly amounts by package:
                • BASIC: %s DT/month
                • CONFORT: %s DT/month
                • PREMIUM: %s DT/month

                General rules:
                1) Respect all members: no insults, harassment, or harmful speech in the group.
                2) Claims: you cannot file a claim until after 5 consecutive months of payment.
                3) Fraud: any fraud attempt may result in a ban from the app, from 3 months up to permanent exclusion.
                4) Warnings: you receive up to 3 warnings for serious breaches; after the third, you may be excluded — without refund of contributions or claims.

                Thank you for being part of the solidarity group.
                """.formatted(
                member.getId() != null ? member.getId() : 0L,
                cin,
                basic,
                confort,
                premium
        ).trim();
    }

    public static String buildPaymentReminderMessage(String groupName, float monthlyAmountDt) {
        String g = groupName != null && !groupName.isBlank() ? groupName : "your group";
        return """
                Payment reminder — SolidariHealth

                Your monthly contribution for "%s" is due: %.2f DT.

                Please pay on time to keep your membership active and your coverage in force.

                Reminder: claims are only possible after 5 months of consecutive payments (see welcome rules).
                """.formatted(g, monthlyAmountDt).trim();
    }

    private static String formatDt(Float v) {
        if (v == null) return "—";
        return String.format("%.2f", v);
    }
}
