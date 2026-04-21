package pi.db.piversionbd.controller.health;

import pi.db.piversionbd.service.health.EmailJSService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/emailjs")
@CrossOrigin(origins = "*")
public class EmailJSController {

    private static final Logger logger = LoggerFactory.getLogger(EmailJSController.class);

    @Autowired
    private EmailJSService emailJSService;

    /**
     * Tester la configuration EmailJS
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        try {
            String status = emailJSService.getConfigurationStatus();
            Map<String, String> response = new HashMap<>();
            response.put("status", status);
            response.put("timestamp", java.time.LocalDateTime.now().toString());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Erreur lors de la vérification de la santé: " + e.getMessage(), e);
            Map<String, String> response = new HashMap<>();
            response.put("error", "EmailJS service error: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Envoyer un email simple
     */
    @PostMapping("/send-simple")
    public ResponseEntity<Map<String, String>> sendSimpleEmail(
            @RequestParam String to,
            @RequestParam String subject,
            @RequestParam String body) {
        try {
            logger.info("Envoi d'email simple à: " + to);
            emailJSService.sendSimpleEmail(to, subject, body);

            Map<String, String> response = new HashMap<>();
            response.put("message", "Email sent successfully to " + to);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Erreur lors de l'envoi d'email simple: " + e.getMessage(), e);
            Map<String, String> response = new HashMap<>();
            response.put("error", "Failed to send email: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Envoyer email de consultation EN_LIGNE
     */
    @PostMapping("/send-online-consultation")
    public ResponseEntity<Map<String, String>> sendOnlineConsultation(
            @RequestParam String to,
            @RequestParam String memberName,
            @RequestParam String doctorName,
            @RequestParam String specialty,
            @RequestParam String dateConsultation,
            @RequestParam String meetLink) {
        try {
            logger.info("Envoi d'email consultation EN_LIGNE à: " + to);
            emailJSService.sendOnlineConsultationEmail(to, memberName, doctorName, specialty, dateConsultation, meetLink);

            Map<String, String> response = new HashMap<>();
            response.put("message", "Online consultation email sent successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Erreur lors de l'envoi d'email consultation EN_LIGNE: " + e.getMessage(), e);
            Map<String, String> response = new HashMap<>();
            response.put("error", "Failed to send consultation email: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Envoyer email de consultation SUR_PLACE
     */
    @PostMapping("/send-onsite-consultation")
    public ResponseEntity<Map<String, String>> sendOnSiteConsultation(
            @RequestParam String to,
            @RequestParam String memberName,
            @RequestParam String doctorName,
            @RequestParam String specialty,
            @RequestParam String dateConsultation,
            @RequestParam String mapsLink) {
        try {
            logger.info("Envoi d'email consultation SUR_PLACE à: " + to);
            emailJSService.sendOnSiteConsultationEmail(to, memberName, doctorName, specialty, dateConsultation, mapsLink);

            Map<String, String> response = new HashMap<>();
            response.put("message", "On-site consultation email sent successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Erreur lors de l'envoi d'email consultation SUR_PLACE: " + e.getMessage(), e);
            Map<String, String> response = new HashMap<>();
            response.put("error", "Failed to send consultation email: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Envoyer email de recommandation doctor
     */
    @PostMapping("/send-doctor-recommendation")
    public ResponseEntity<Map<String, String>> sendDoctorRecommendation(
            @RequestParam String to,
            @RequestParam String memberName,
            @RequestParam String doctorName,
            @RequestParam String specialty,
            @RequestParam String reason) {
        try {
            logger.info("Envoi d'email recommandation doctor à: " + to);
            emailJSService.sendDoctorRecommendationEmail(to, memberName, doctorName, specialty, reason);

            Map<String, String> response = new HashMap<>();
            response.put("message", "Doctor recommendation email sent successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Erreur lors de l'envoi d'email recommandation: " + e.getMessage(), e);
            Map<String, String> response = new HashMap<>();
            response.put("error", "Failed to send recommendation email: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Envoyer email de recommandation produit
     */
    @PostMapping("/send-product-recommendation")
    public ResponseEntity<Map<String, String>> sendProductRecommendation(
            @RequestParam String to,
            @RequestParam String memberName,
            @RequestParam String productName,
            @RequestParam String disease,
            @RequestParam String reason) {
        try {
            logger.info("Envoi d'email recommandation produit à: " + to);
            emailJSService.sendProductRecommendationEmail(to, memberName, productName, disease, reason);

            Map<String, String> response = new HashMap<>();
            response.put("message", "Product recommendation email sent successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Erreur lors de l'envoi d'email recommandation produit: " + e.getMessage(), e);
            Map<String, String> response = new HashMap<>();
            response.put("error", "Failed to send product recommendation email: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
}

