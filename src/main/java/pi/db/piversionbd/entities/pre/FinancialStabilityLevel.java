package pi.db.piversionbd.entities.pre;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Niveau de stabilité financière déclaré par le membre.
 */
@Schema(description = "Niveau de stabilité financière", allowableValues = {"STABLE", "MODERE", "INSTABLE"})
public enum FinancialStabilityLevel {
    STABLE,
    MODERE,
    INSTABLE
    ;

    @JsonCreator
    public static FinancialStabilityLevel from(String raw) {
        if (raw == null) return null;
        String v = raw.trim().toUpperCase();
        return switch (v) {
            case "STABLE" -> STABLE;
            case "MODERE", "MODÉRÉ", "MODEREE", "MODÉRÉE" -> MODERE;
            case "INSTABLE" -> INSTABLE;
            default -> FinancialStabilityLevel.valueOf(v);
        };
    }

    @JsonValue
    public String toJson() {
        return name();
    }
}

