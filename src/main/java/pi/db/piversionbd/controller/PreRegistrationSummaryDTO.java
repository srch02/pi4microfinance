package pi.db.piversionbd.controller;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import pi.db.piversionbd.entities.pre.PreRegistrationStatus;

import java.time.LocalDateTime;

@Schema(name = "PreRegistrationSummary", description = "Résumé d'une pré-inscription")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PreRegistrationSummaryDTO {

    @Schema(description = "ID", example = "1")
    private Long id;
    @Schema(description = "Numéro CIN", example = "12345678")
    private String cinNumber;
    @Schema(description = "Statut", example = "PENDING_REVIEW")
    private PreRegistrationStatus status;
    @Schema(description = "Score fraude", example = "0.2")
    private Float fraudScore;
    @Schema(description = "Date de création", type = "string", format = "date-time", example = "2026-02-14T10:30:00")
    private LocalDateTime createdAt;
}
