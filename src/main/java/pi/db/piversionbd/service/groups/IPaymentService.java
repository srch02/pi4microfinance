package pi.db.piversionbd.service.groups;

import pi.db.piversionbd.entities.groups.Membership;
import pi.db.piversionbd.entities.groups.Payment;

import java.util.List;

/**
 * On successful payment: create payment record, update group pool, set membership to active.
 * Payment amount is always taken from the membership's monthly amount (based on the member's
 * chosen package: BASIC, CONFORT, PREMIUM and their subscription prices).
 */
public interface IPaymentService {

    /**
     * Record a successful first payment for a membership. Uses the membership's monthly amount
     * (from the chosen package) and applies 70% pool / 20% platform / 10% national fund.
     * Creates the payment record, updates the group pool, and sets the membership status to active.
     *
     * @param membershipId the membership this payment is for (must be pending)
     * @return the updated membership (status set to active)
     */
    Membership recordSuccessfulPayment(Long membershipId);

    /**
     * Process a monthly premium payment. Uses the membership's monthly amount (from the member's
     * chosen package: BASIC, CONFORT, PREMIUM). Splits: 70% pool, 20% platform, 10% national fund.
     * Creates payment record and updates group pool.
     *
     * @return the created payment (confirmation)
     */
    Payment processMonthlyPayment(Long memberId, Long groupId);

    /**
     * Get payment history for a member, optionally filtered by group.
     *
     * @param groupId optional; if null, returns all payments for the member
     */
    List<Payment> getPaymentHistory(Long memberId, Long groupId);
}
