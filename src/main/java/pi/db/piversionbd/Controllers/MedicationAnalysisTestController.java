package pi.db.piversionbd.Controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/pidb/api/medication-analysis-test")
@Tag(name = "Medication Analysis Test", description = "API test simple")
@CrossOrigin(origins = "*")
public class MedicationAnalysisTestController {

    @PostMapping("/test-simple")
    @Operation(summary = "Test simple")
    @ApiResponse(responseCode = "200", description = "Succès - Réponse simulée")
    public ResponseEntity<?> testSimple() {
        try {
            log.info("Test simple endpoint called");
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Test simple réussi - Infrastructure OK");
            response.put("timestamp", java.time.LocalDateTime.now().toString());
            
            Map<String, Object> data = new HashMap<>();
            data.put("id", 1);
            data.put("medicationName", "Paracétamol 500mg (Test)");
            data.put("activeIngredients", "Paracétamol 500mg par comprimé");
            data.put("indications", "Douleur légère à modérée, fièvre");
            data.put("dosage", "1-2 comprimés toutes les 4-6 heures");
            data.put("sideEffects", "Nausées (rares), allergies (très rares)");
            data.put("contraindications", "Insuffisance hépatique sévère");
            data.put("usageAdvice", "Prendre avec nourriture");
            data.put("interactions", "Anticoagulants, alcool");
            data.put("recognitionConfidence", "test mode");
            
            response.put("data", data);
            
            log.info("Test simple response sent successfully");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Erreur test: {}", e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Erreur: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    @GetMapping("/health")
    @Operation(summary = "Vérifier la santé de l'API")
    @ApiResponse(responseCode = "200", description = "API fonctionnelle")
    public ResponseEntity<?> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("message", "API Medication Analysis est fonctionnelle");
        response.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(response);
    }
}

