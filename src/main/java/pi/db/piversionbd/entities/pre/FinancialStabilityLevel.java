package pi.db.piversionbd.entities.pre;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Niveau de stabilité financière déclaré par le membre.
 */
@Schema(description = "Niveau de stabilité financière", allowableValues = {"STABLE", "MODERE", "INSTABLE"})
public enum FinancialStabilityLevel {
    STABLE,
    MODERE,
    INSTABLE
}

