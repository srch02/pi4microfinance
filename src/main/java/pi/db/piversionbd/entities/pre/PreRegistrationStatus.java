package pi.db.piversionbd.entities.pre;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
    description = "Statut d'une pré-inscription",
    allowableValues = {"PENDING_REVIEW", "APPROVED", "REJECTED", "ACTIVATED"}
)
public enum PreRegistrationStatus {
    PENDING_REVIEW,
    APPROVED,
    REJECTED,
    ACTIVATED
}

