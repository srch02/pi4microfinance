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
    
}

