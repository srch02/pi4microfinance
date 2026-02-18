package pi.db.piversionbd.entities.groups;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Insurance package types available for members.
 */
@Schema(description = "Type de package d'assurance", allowableValues = {"BASIC", "CONFORT", "PREMIUM"})
public enum PackageType {
    BASIC,
    CONFORT,
    PREMIUM
    ;

    @JsonCreator
    public static PackageType from(String raw) {
        if (raw == null) return null;
        String v = raw.trim().toUpperCase();
        // accept common misspelling without breaking Swagger dropdown
        if ("COMFORT".equals(v)) {
            v = "CONFORT";
        }
        return PackageType.valueOf(v);
    }

    @JsonValue
    public String toJson() {
        return name();
    }
}

