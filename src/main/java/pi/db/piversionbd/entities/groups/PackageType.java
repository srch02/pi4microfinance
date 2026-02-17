package pi.db.piversionbd.entities.groups;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Insurance package types available for members.
 */
@Schema(description = "Type de package d'assurance", allowableValues = {"BASIC", "CONFORT", "PREMIUM"})
public enum PackageType {
    BASIC,
    CONFORT,
    PREMIUM
}

