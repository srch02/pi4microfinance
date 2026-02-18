package pi.db.piversionbd.entities.groups;

import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;

@Entity
@Table(name = "MEMBERSHIPS")
@Data
public class Membership {

    // --- Package coverage baselines (before personalization) ---
    // These reflect the product sheet:
    // BASIC   : 10 DT/month  -> ~5 problems/year, 1500 DT/year (≈300 DT per problem)
    // CONFORT : 20 DT/month  -> ~5–6 problems/year, 3000 DT/year
    // PREMIUM : 50 DT/month  -> ~8–10 problems/year, 6000 DT/year
    private static final float BASIC_BASE_MONTHLY = 10f;
    private static final float BASIC_BASE_ANNUAL_LIMIT = 1500f;
    private static final int BASIC_BASE_CONSULTATIONS = 5;

    private static final float CONFORT_BASE_MONTHLY = 20f;
    private static final float CONFORT_BASE_ANNUAL_LIMIT = 3000f;
    private static final int CONFORT_BASE_CONSULTATIONS = 6;

    private static final float PREMIUM_BASE_MONTHLY = 50f;
    private static final float PREMIUM_BASE_ANNUAL_LIMIT = 6000f;
    private static final int PREMIUM_BASE_CONSULTATIONS = 9;

    /** Minimum / maximum scaling we allow vs the baseline coverage, based on personalized monthly amount. */
    private static final float COVERAGE_SCALE_MIN = 0.75f;
    private static final float COVERAGE_SCALE_MAX = 1.5f;

    /** Threshold (e.g. 80%) to start warning a member that they are close to their annual limit or consultations limit. */
    private static final float ALERT_THRESHOLD_RATIO = 0.8f;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "member_id")
    private Member member;

    @ManyToOne
    @JoinColumn(name = "group_id")
    private Group group;

    @Column(name = "package_type")
    private String packageType;

    @Column(name = "monthly_amount")
    private Float monthlyAmount;

    @Column(name = "consultations_limit")
    private Integer consultationsLimit;

    @Column(name = "annual_limit")
    private Float annualLimit;

    /** pending, active, suspended, cancelled. New memberships start as pending until first payment. */
    @Column(name = "status", length = 20)
    private String status;

    /** When set, membership is ended (soft delete). Used with status=cancelled. */
    @Column(name = "ended_at")
    private Instant endedAt;

    /** Set when membership becomes active (first successful payment). */
    @Column(name = "activated_at")
    private Instant activatedAt;

    public static final String STATUS_PENDING = "pending";
    public static final String STATUS_ACTIVE = "active";
    public static final String STATUS_SUSPENDED = "suspended";
    public static final String STATUS_CANCELLED = "cancelled";

    /**
     * Apply personalized coverage (consultations_limit and annual_limit) based on the package type
     * and the member's personalized monthly amount.
     *
     * Idea:
     * - Each package has a baseline monthly price and baseline coverage (consultations + annual limit)
     * - The real monthly_amount for the member is personalized (risk, age, etc.)
     * - We scale the coverage slightly with the personalized monthly amount (so coverage feels fair),
     *   but keep it within a reasonable band [75% .. 150%] of the baseline.
     */
    public void applyPersonalizedCoverage() {
        if (packageType == null || monthlyAmount == null || monthlyAmount <= 0f) {
            // If we don't know the package or amount, leave coverage unset.
            return;
        }
        String pkg = packageType.toUpperCase();

        float baseMonthly;
        float baseAnnual;
        int baseConsultations;
        switch (pkg) {
            case "CONFORT" -> {
                baseMonthly = CONFORT_BASE_MONTHLY;
                baseAnnual = CONFORT_BASE_ANNUAL_LIMIT;
                baseConsultations = CONFORT_BASE_CONSULTATIONS;
            }
            case "PREMIUM" -> {
                baseMonthly = PREMIUM_BASE_MONTHLY;
                baseAnnual = PREMIUM_BASE_ANNUAL_LIMIT;
                baseConsultations = PREMIUM_BASE_CONSULTATIONS;
            }
            default -> {
                baseMonthly = BASIC_BASE_MONTHLY;
                baseAnnual = BASIC_BASE_ANNUAL_LIMIT;
                baseConsultations = BASIC_BASE_CONSULTATIONS;
            }
        }

        // How far we are from the baseline monthly price.
        float scale = monthlyAmount / baseMonthly;
        if (scale < COVERAGE_SCALE_MIN) scale = COVERAGE_SCALE_MIN;
        if (scale > COVERAGE_SCALE_MAX) scale = COVERAGE_SCALE_MAX;

        // Annual limit scales linearly; consultations a bit more conservatively.
        float personalizedAnnualLimit = baseAnnual * scale;
        int personalizedConsultations = Math.round(baseConsultations * scale);

        this.annualLimit = personalizedAnnualLimit;
        this.consultationsLimit = personalizedConsultations;
    }

    /**
     * @param usedAmountThisYear total reimbursed / covered amount for this membership in the current policy year
     * @return true if the member is close to their annual limit (e.g. ≥ 80% of the limit)
     */
    public boolean isCloseToAnnualLimit(float usedAmountThisYear) {
        if (annualLimit == null || annualLimit <= 0f) {
            return false;
        }
        return usedAmountThisYear >= annualLimit * ALERT_THRESHOLD_RATIO;
    }

    /**
     * @param usedConsultations number of consultations already used in the current policy year
     * @return true if the member is close to their consultations limit (e.g. ≥ 80% of the limit)
     */
    public boolean isCloseToConsultationsLimit(int usedConsultations) {
        if (consultationsLimit == null || consultationsLimit <= 0) {
            return false;
        }
        return usedConsultations >= consultationsLimit * ALERT_THRESHOLD_RATIO;
    }

}

