package pi.db.piversionbd.entities.score;

public enum ClaimStatus {
    SUBMITTED,
    SCORED,
    APPROVED_AUTO,
    MANUAL_REVIEW,
    APPROVED_MANUAL,
    REJECTED_LOW_SCORE,
    REJECTED_EXCLUSION,
    REJECTED_FRAUD,
    PAID,
    CANCELLED
}
