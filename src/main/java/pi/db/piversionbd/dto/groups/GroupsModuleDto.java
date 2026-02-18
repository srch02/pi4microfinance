package pi.db.piversionbd.dto.groups;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import pi.db.piversionbd.entities.groups.Group;
import pi.db.piversionbd.entities.groups.Member;
import pi.db.piversionbd.entities.groups.Membership;
import pi.db.piversionbd.entities.groups.Payment;

/**
 * Single file for all Module 1 (Groups) DTOs: groups, members, etc.
 */
public final class GroupsModuleDto {

    private GroupsModuleDto() {}

    // ----- Group -----

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(
            description = "Solidarity group. Fill each field; use placeholders below as guide.",
            example = "{\"groupId\": 1, \"name\": \"Family Solidarity 42\", \"type\": \"FAMILY\", \"region\": \"Tunis\", \"minMembers\": 5, \"maxMembers\": 15, \"currentMemberCount\": 0}"
    )
    public static class GroupDto {

        @Schema(description = "Group ID. Placeholder: 1 (read-only, omit for create)", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
        private Long groupId;

        @Schema(description = "Group name. Placeholder: Family Solidarity 42", example = "Family Solidarity 42", requiredMode = Schema.RequiredMode.REQUIRED)
        private String name;

        @Schema(description = "Group type. Placeholder: FAMILY. Available values: FAMILY, STUDENTS, WORKERS, MIXED", example = "FAMILY", allowableValues = { "FAMILY", "STUDENTS", "WORKERS", "MIXED" })
        private String type;

        @Schema(description = "Region / city. Placeholder: Tunis", example = "Tunis")
        private String region;

        @Schema(description = "Join policy: public or private. Private groups require an invite code (QR).", example = "public", allowableValues = { "public", "private" })
        private String joinPolicy;

        @Schema(description = "Invite code for private groups (returned on creation so creator can share). Null for public groups.", example = "a1b2c3d4e5f6")
        private String inviteCode;

        @Schema(description = "Creator member ID (if known).", example = "1")
        private Long createdByMemberId;

        @Schema(description = "Minimum members. Placeholder: 5", example = "5", minimum = "5", maximum = "30")
        private Integer minMembers;

        @Schema(description = "Maximum members. Placeholder: 15", example = "15", minimum = "5", maximum = "30")
        private Integer maxMembers;

        @Schema(description = "Current member count. Placeholder: 0", example = "0")
        private Integer currentMemberCount;

        public static GroupDto fromEntity(Group g) {
            if (g == null) return null;
            return new GroupDto(
                    g.getId(), // exposed as groupId
                    g.getName(),
                    g.getType(),
                    g.getRegion(),
                    g.getJoinPolicy(),
                    g.getInviteCode(),
                    g.getCreator() != null ? g.getCreator().getId() : null,
                    g.getMinMembers(),
                    g.getMaxMembers(),
                    g.getCurrentMemberCount()
            );
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Create group request (public or private). For private groups, inviteCode is generated and returned.")
    public static class CreateGroupRequest {

        @Schema(description = "Group name", example = "My Family Group", requiredMode = Schema.RequiredMode.REQUIRED)
        private String name;

        @Schema(description = "Group type", example = "FAMILY", allowableValues = { "FAMILY", "STUDENTS", "WORKERS", "MIXED" })
        private String type;

        @Schema(description = "Region", example = "Tunis")
        private String region;

        @Schema(description = "Minimum members", example = "2")
        private Integer minMembers;

        @Schema(description = "Maximum members", example = "10")
        private Integer maxMembers;

        @Schema(description = "Join policy: public or private", example = "private", allowableValues = { "public", "private" })
        private String joinPolicy;

        @Schema(description = "Creator member ID (until auth is added).", example = "1")
        private Long creatorMemberId;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Update group request. currentMemberCount is not updatable.")
    public static class UpdateGroupRequest {

        @Schema(description = "Group name", example = "My Family Group")
        private String name;

        @Schema(description = "Group type", example = "FAMILY", allowableValues = { "FAMILY", "STUDENTS", "WORKERS", "MIXED" })
        private String type;

        @Schema(description = "Region", example = "Tunis")
        private String region;

        @Schema(description = "Minimum members", example = "2")
        private Integer minMembers;

        @Schema(description = "Maximum members", example = "10")
        private Integer maxMembers;

        @Schema(description = "Join policy: public or private", example = "private", allowableValues = { "public", "private" })
        private String joinPolicy;
    }

    // ----- Member -----

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(
            description = "Member form: after acceptance, app shows this form with pre-registration info. Member fills age, profession, region; chooses package (BASIC/CONFORT/PREMIUM) then pays.",
            example = "{\"memberId\": 1, \"cinNumber\": \"12345678\", \"age\": 28, \"profession\": \"student\", \"region\": \"Tunis\", \"personalizedMonthlyPrice\": 17.5, \"adherenceScore\": 85, \"currentGroupId\": 1}"
    )
    public static class MemberDto {

        @Schema(description = "Member ID (read-only on response; omit for create)", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
        private Long memberId;

        @Schema(description = "CIN number (from pre-registration, not editable on update)", example = "12345678", requiredMode = Schema.RequiredMode.REQUIRED)
        private String cinNumber;

        @Schema(description = "Age (for group suggestions / future age-range matching)", example = "28")
        private Integer age;

        @Schema(description = "Profession (e.g. student, worker) – for group type matching: students→STUDENTS, workers→WORKERS, family→FAMILY", example = "student")
        private String profession;

        @Schema(description = "Region – for group suggestions (same region)", example = "Tunis")
        private String region;

        @Schema(description = "Personalized monthly premium (DT). Placeholder: 17.5", example = "17.5")
        private Float personalizedMonthlyPrice;

        @Schema(description = "Adherence score (0-100). Placeholder: 85", example = "85", minimum = "0", maximum = "100")
        private Float adherenceScore;

        @Schema(description = "Current group ID (optional)", example = "1")
        private Long currentGroupId;

        public static MemberDto fromEntity(Member m) {
            if (m == null) return null;
            return new MemberDto(
                    m.getId(),
                    m.getCinNumber(),
                    m.getAge(),
                    m.getProfession(),
                    m.getRegion(),
                    m.getPersonalizedMonthlyPrice(),
                    m.getAdherenceScore(),
                    m.getCurrentGroup() != null ? m.getCurrentGroup().getId() : null
            );
        }
    }

    // ----- Membership -----

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Membership: links a member to a group with package and monthly amount (post-approval groups/payments)")
    public static class MembershipDto {

        @Schema(description = "Membership ID. Placeholder: 1", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
        private Long membershipId;

        @Schema(description = "Member ID. Placeholder: 1", example = "1")
        private Long memberId;

        @Schema(description = "Group ID. Placeholder: 1", example = "1")
        private Long groupId;

        @Schema(description = "Package type. Placeholder: BASIC. Available values: BASIC, CONFORT, PREMIUM", example = "BASIC", allowableValues = { "BASIC", "CONFORT", "PREMIUM" })
        private String packageType;

        @Schema(description = "Monthly amount (DT). Placeholder: 17.5", example = "17.5")
        private Float monthlyAmount;

        @Schema(description = "Consultations limit. Placeholder: 4", example = "4")
        private Integer consultationsLimit;

        @Schema(description = "Annual limit. Placeholder: 500.0", example = "500.0")
        private Float annualLimit;

        @Schema(description = "Status: pending, active, suspended, cancelled. Pending until first payment.", example = "pending", allowableValues = { "pending", "active", "suspended", "cancelled" })
        private String status;

        public static MembershipDto fromEntity(Membership m) {
            if (m == null) return null;
            String status = m.getStatus() != null ? m.getStatus() : Membership.STATUS_PENDING;
            return new MembershipDto(
                    m.getId(),
                    m.getMember() != null ? m.getMember().getId() : null,
                    m.getGroup() != null ? m.getGroup().getId() : null,
                    m.getPackageType(),
                    m.getMonthlyAmount(),
                    m.getConsultationsLimit(),
                    m.getAnnualLimit(),
                    status
            );
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Request to create a membership. Body is package type only; memberId is provided via query param or auth.")
    public static class AddMemberToGroupRequest {

        @Schema(description = "Package type. Placeholder: BASIC. Available values: BASIC, CONFORT, PREMIUM", example = "BASIC", allowableValues = { "BASIC", "CONFORT", "PREMIUM" })
        private String packageType;
    }

    // ----- Payment (successful payment flow) -----

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Deprecated: first payment now uses membership's monthly amount (package BASIC/CONFORT/PREMIUM). POST /api/payments/memberships/{id} requires no body.")
    public static class RecordPaymentRequest {

        @Schema(description = "No longer used; amount is taken from membership.", deprecated = true)
        private Float amount;

        @Schema(description = "No longer used; 70/20/10 split is applied server-side.", deprecated = true)
        private Float poolAllocation;

        @Schema(description = "No longer used.", deprecated = true)
        private Float platformFee;

        @Schema(description = "No longer used.", deprecated = true)
        private Float nationalFund;
    }

    // ----- Monthly payment (POST /api/payments) -----

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Process a monthly premium payment. Amount is taken from the membership (member's chosen package: BASIC, CONFORT, PREMIUM).")
    public static class MonthlyPaymentRequest {

        @Schema(description = "Member ID. Placeholder: 1", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        private Long memberId;

        @Schema(description = "Group ID. Placeholder: 1", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        private Long groupId;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Payment record / confirmation (id, splits, group pool updated).")
    public static class PaymentDto {

        @Schema(description = "Payment ID.", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
        private Long paymentId;

        @Schema(description = "Member ID.", example = "1")
        private Long memberId;

        @Schema(description = "Group ID.", example = "1")
        private Long groupId;

        @Schema(description = "Total amount (DT).", example = "17.5")
        private Float amount;

        @Schema(description = "Pool allocation (70%).", example = "12.25")
        private Float poolAllocation;

        @Schema(description = "Platform fee (20%).", example = "3.5")
        private Float platformFee;

        @Schema(description = "National fund (10%).", example = "1.75")
        private Float nationalFund;

        public static PaymentDto fromEntity(Payment p) {
            if (p == null) return null;
            return new PaymentDto(
                    p.getId(),
                    p.getMember() != null ? p.getMember().getId() : null,
                    p.getGroup() != null ? p.getGroup().getId() : null,
                    p.getAmount(),
                    p.getPoolAllocation(),
                    p.getPlatformFee(),
                    p.getNationalFund()
            );
        }
    }

    // ----- Alerts (coverage & pool) -----

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Coverage alerts for a membership: flags indicating if the member is close to their annual or consultations limit.")
    public static class CoverageAlertsDto {

        @Schema(description = "Membership ID.", example = "1")
        private Long membershipId;

        @Schema(description = "True if used amount this year is close to annual_limit (e.g. ≥ 80%).", example = "false")
        private Boolean closeToAnnualLimit;

        @Schema(description = "True if used consultations are close to consultations_limit (e.g. ≥ 80%).", example = "false")
        private Boolean closeToConsultationsLimit;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Alert status for a group pool (used by admin dashboards).")
    public static class GroupPoolAlertDto {

        @Schema(description = "Group ID.", example = "1")
        private Long groupId;

        @Schema(description = "True if pool balance is low compared to total contributions (e.g. ≤ 20%).", example = "false")
        private Boolean lowBalance;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Group pool: exact balance, total contributions, and when it was last updated (e.g. when a payment was applied).")
    public static class GroupPoolDto {

        @Schema(description = "Group ID.", example = "1")
        private Long groupId;

        @Schema(description = "Current pool balance (DT). Amount available in the solidarity fund.", example = "122.50")
        private Float poolBalance;

        @Schema(description = "Total contributions so far (DT). Sum of all pool allocations from payments.", example = "500.00")
        private Float totalContributions;

        @Schema(description = "Total paid out (DT). Amount withdrawn from pool for claims.", example = "0")
        private Float totalPaidOut;

        @Schema(description = "Last time the pool was updated (e.g. when a payment was recorded). Null if no payment yet.", example = "2025-02-15T14:30:00Z")
        private java.time.Instant updatedAt;

        @Schema(description = "True if pool balance is low (≤ 20% of total contributions).", example = "false")
        private Boolean lowBalance;

        public static GroupPoolDto fromEntity(pi.db.piversionbd.entities.groups.GroupPool p) {
            if (p == null) return null;
            Long gid = p.getGroup() != null ? p.getGroup().getId() : null;
            float balance = p.getPoolBalance() != null ? p.getPoolBalance() : 0f;
            float contributions = p.getTotalContributions() != null ? p.getTotalContributions() : 0f;
            float paidOut = p.getTotalPaidOut() != null ? p.getTotalPaidOut() : 0f;
            return new GroupPoolDto(gid, balance, contributions, paidOut, p.getUpdatedAt(), p.isLowBalance());
        }

        /** For a group that has no pool record yet (no payments). */
        public static GroupPoolDto empty(Long groupId) {
            return new GroupPoolDto(groupId, 0f, 0f, 0f, null, false);
        }
    }

    // ----- Group change request (admin approval flow) -----

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Request to change group. When member already in another group, a PENDING request is created; admin approves then member can complete the switch.")
    public static class GroupChangeRequestDto {

        @Schema(description = "Request ID.", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
        private Long requestId;

        @Schema(description = "Member ID.", example = "1")
        private Long memberId;

        @Schema(description = "Current group ID (member will leave).", example = "1")
        private Long fromGroupId;

        @Schema(description = "Target group ID (member wants to join).", example = "2")
        private Long toGroupId;

        @Schema(description = "Status: pending, approved, rejected, completed.", example = "pending", allowableValues = {"pending", "approved", "rejected", "completed"})
        private String status;

        @Schema(description = "When the request was created.")
        private java.time.Instant createdAt;

        @Schema(description = "When admin reviewed (approved/rejected).")
        private java.time.Instant reviewedAt;

        @Schema(description = "Requested package type for the new membership.", example = "BASIC")
        private String requestedPackageType;

        public static GroupChangeRequestDto fromEntity(pi.db.piversionbd.entities.groups.GroupChangeRequest r) {
            if (r == null) return null;
            return new GroupChangeRequestDto(
                    r.getId(),
                    r.getMember() != null ? r.getMember().getId() : null,
                    r.getFromGroup() != null ? r.getFromGroup().getId() : null,
                    r.getToGroup() != null ? r.getToGroup().getId() : null,
                    r.getStatus(),
                    r.getCreatedAt(),
                    r.getReviewedAt(),
                    r.getRequestedPackageType()
            );
        }
    }
}
