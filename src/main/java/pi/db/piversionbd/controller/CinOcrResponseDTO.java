package pi.db.piversionbd.controller;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(name = "CinOcrResponse", description = "Résultat extraction CIN (OCR)")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CinOcrResponseDTO {
    @Schema(description = "CIN extrait (1 à 8 chiffres)", example = "12345678")
    private String cinNumber;

    @Schema(description = "Texte OCR brut (si disponible, tronqué)", example = "CIN: 12345678 ...")
    private String rawText;
}

