package pi.db.piversionbd.entities.pre;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
    description = "Statut d'une pré-inscription",
    allowableValues = {"PENDING_REVIEW", "APPROVED", "ACCEPTED", "REJECTED", "ACTIVATED"}
)
public enum PreRegistrationStatus {
    PENDING_REVIEW,
    APPROVED,
    ACCEPTED,
    REJECTED,
    ACTIVATED
    ;

    @JsonCreator
    public static PreRegistrationStatus from(String raw) {
        if (raw == null) return null;
        String normalized = raw.trim().toUpperCase();
        if ("ACCEPTED".equals(normalized)) {
            return APPROVED;
        }
        return PreRegistrationStatus.valueOf(normalized);
    }

    @JsonValue
    public String toJson() {
        return name();
    }

    /**
     * Whether a {@link pi.db.piversionbd.entities.groups.Member} may be created for this pre-registration.
     * {@link #APPROVED} / {@link #ACCEPTED} after admin review; {@link #ACTIVATED} after first membership
     * payment (member may be missing e.g. if data was reset) — all are treated as “cleared for member row”.
     */
    public static boolean allowsMemberCreation(PreRegistrationStatus status) {
        if (status == null) {
            return false;
        }
        return status == APPROVED || status == ACCEPTED || status == ACTIVATED;
    }
}

