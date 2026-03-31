package pi.db.piversionbd.Controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import pi.db.piversionbd.entities.health.MedicationAnalysis;
import pi.db.piversionbd.services.IMedicationAnalysisService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/pidb/api/medication-analysis")
@Tag(name = "Medication Analysis", description = "API pour analyser les images de médicaments avec l'IA")
@CrossOrigin(origins = "*", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE})
public class MedicationAnalysisController {

    @Autowired
    private IMedicationAnalysisService medicationAnalysisService;

    @PostMapping(value = "/analyze/{memberId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Analyser une image de médicament",
            description = "Télécharge une image JPG/PNG de médicament et obtient une analyse détaillée avec reconnaissance IA"
    )
    @ApiResponse(
            responseCode = "200",
            description = "Analyse réussie",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = MedicationAnalysis.class))
    )
    @ApiResponse(responseCode = "400", description = "Image vide ou format invalide")
    @ApiResponse(responseCode = "404", description = "Membre non trouvé")
    @ApiResponse(responseCode = "500", description = "Erreur serveur")
    public ResponseEntity<?> analyzeMedicationImage(
            @PathVariable(name = "memberId", required = true) Long memberId,
            @RequestParam(name = "image", required = true) MultipartFile imageFile) {
        try {
            log.info("Received medication analysis request for member: {}", memberId);

            if (imageFile.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("L'image ne peut pas être vide"));
            }

            MedicationAnalysis analysis = medicationAnalysisService.analyzeMedicationImage(imageFile, memberId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Analyse du médicament réussie");
            response.put("data", analysis);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error analyzing medication image: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Erreur lors de l'analyse: " + e.getMessage()));
        }
    }

    @GetMapping("/history/{memberId}")
    @Operation(
            summary = "Obtenir l'historique d'analyse",
            description = "Récupère toutes les analyses précédentes d'un membre"
    )
    @ApiResponse(
            responseCode = "200",
            description = "Historique récupéré avec succès"
    )
    public ResponseEntity<?> getAnalysisHistory(@PathVariable Long memberId) {
        try {
            List<MedicationAnalysis> history = medicationAnalysisService.getAnalysisHistoryByMember(memberId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("count", history.size());
            response.put("data", history);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error fetching analysis history: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Erreur lors de la récupération de l'historique"));
        }
    }

    @GetMapping("/{analysisId}")
    @Operation(
            summary = "Obtenir une analyse spécifique",
            description = "Récupère les détails d'une analyse précise"
    )
    public ResponseEntity<?> getAnalysis(@PathVariable Long analysisId) {
        try {
            MedicationAnalysis analysis = medicationAnalysisService.getAnalysisById(analysisId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", analysis);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error fetching analysis: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(createErrorResponse("Analyse non trouvée"));
        }
    }

    @DeleteMapping("/{analysisId}")
    @Operation(
            summary = "Supprimer une analyse",
            description = "Supprime une analyse de la base de données"
    )
    public ResponseEntity<?> deleteAnalysis(@PathVariable Long analysisId) {
        try {
            medicationAnalysisService.deleteAnalysis(analysisId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Analyse supprimée avec succès");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error deleting analysis: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Erreur lors de la suppression"));
        }
    }

    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", message);
        return response;
    }
}

