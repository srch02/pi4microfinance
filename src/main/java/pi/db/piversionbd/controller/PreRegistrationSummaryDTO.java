package pi.db.piversionbd.controller;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Schema(name = "PreRegistrationSummary", description = "Résumé d'une pré-inscription")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PreRegistrationSummaryDTO {

    @Schema(description = "ID")
    private Long id;
    @Schema(description = "Numéro CIN")
    private String cinNumber;
    @Schema(description = "Statut")
    private String status;
    @Schema(description = "Score fraude")
    private Float fraudScore;
    @Schema(description = "Date de création", type = "string", format = "date-time")
    private LocalDateTime createdAt;
}
