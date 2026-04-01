package pi.db.piversionbd.service.notifications;

import pi.db.piversionbd.entities.score.Claim;

import java.math.BigDecimal;

public final class TelegramClaimMessages {

    private TelegramClaimMessages() {}

    public static String buildAutoApproved(Claim claim) {
        return """
                Claim update — SolidariHealth

                Your claim was AUTO-APPROVED.

                Claim ID: %s
                Claim Number: %s
                Amount: %s DT
                Status: %s

                Next step: reimbursement processing will continue automatically.
                """.formatted(
                safeId(claim),
                safe(claim != null ? claim.getClaimNumber() : null),
                formatDt(bestAmount(claim)),
                safe(claim != null && claim.getStatus() != null ? claim.getStatus().name() : null)
        ).trim();
    }

    public static String buildManual24h(Claim claim) {
        return """
                Claim update — SolidariHealth

                Your claim is under review and will be handled within 24 hours.

                Claim ID: %s
                Claim Number: %s
                Amount requested: %s DT
                Status: %s
                """.formatted(
                safeId(claim),
                safe(claim != null ? claim.getClaimNumber() : null),
                formatDt(claim != null ? claim.getAmountRequested() : null),
                safe(claim != null && claim.getStatus() != null ? claim.getStatus().name() : null)
        ).trim();
    }

    public static String buildAdminApprovalLowScore(Claim claim) {
        return """
                Claim update — SolidariHealth

                Your claim requires ADMIN approval due to a low score.
                It will be handled within 24 hours.

                Claim ID: %s
                Claim Number: %s
                Amount requested: %s DT
                Status: %s
                """.formatted(
                safeId(claim),
                safe(claim != null ? claim.getClaimNumber() : null),
                formatDt(claim != null ? claim.getAmountRequested() : null),
                safe(claim != null && claim.getStatus() != null ? claim.getStatus().name() : null)
        ).trim();
    }

    private static String safeId(Claim claim) {
        return claim != null && claim.getId() != null ? String.valueOf(claim.getId()) : "—";
    }

    private static String safe(String v) {
        return v == null || v.isBlank() ? "—" : v;
    }

    private static BigDecimal bestAmount(Claim claim) {
        if (claim == null) return null;
        return claim.getAmountApproved() != null ? claim.getAmountApproved() : claim.getAmountRequested();
    }

    private static String formatDt(BigDecimal v) {
        if (v == null) return "—";
        try {
            return String.format("%.2f", v);
        } catch (Exception e) {
            return v.toString();
        }
    }
}

