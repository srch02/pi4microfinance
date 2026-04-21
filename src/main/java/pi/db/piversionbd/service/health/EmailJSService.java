package pi.db.piversionbd.service.health;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;

@Service
public class EmailJSService {

    private static final Logger logger = LoggerFactory.getLogger(EmailJSService.class);
    private static final String EMAILJS_API_URL = "https://api.emailjs.com/api/v1.0/email/send";

    @Value("${emailjs.service.id:YOUR_SERVICE_ID}")
    private String serviceId;

    @Value("${emailjs.template.id:YOUR_TEMPLATE_ID}")
    private String templateId;

    @Value("${emailjs.public.key:YOUR_PUBLIC_KEY}")
    private String publicKey;

    @Value("${emailjs.private.key:YOUR_PRIVATE_KEY}")
    private String privateKey;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Envoyer un email simple
     */
    public void sendSimpleEmail(String to, String subject, String body) {
        try {
            Map<String, Object> templateParams = new HashMap<>();
            templateParams.put("to_email", to);
            templateParams.put("subject", subject);
            templateParams.put("message", body);

            sendEmail(templateParams);
        } catch (Exception e) {
            logger.error("Erreur lors de l'envoi d'email simple: " + e.getMessage(), e);
            throw new RuntimeException("Erreur lors de l'envoi d'email: " + e.getMessage());
        }
    }

    /**
     * Envoyer email de consultation EN_LIGNE (Google Meet)
     */
    public void sendOnlineConsultationEmail(String to, String memberName, String doctorName, 
                                           String specialty, String dateConsultation, String meetLink) {
        try {
            Map<String, Object> templateParams = new HashMap<>();
            templateParams.put("to_email", to);
            templateParams.put("member_name", memberName);
            templateParams.put("doctor_name", doctorName);
            templateParams.put("specialty", specialty);
            templateParams.put("consultation_date", dateConsultation);
            templateParams.put("meet_link", meetLink);
            templateParams.put("consultation_type", "EN_LIGNE");

            sendEmail(templateParams);
            logger.info("Email consultation EN_LIGNE envoyé à: " + to);
        } catch (Exception e) {
            logger.error("Erreur lors de l'envoi d'email de consultation EN_LIGNE: " + e.getMessage(), e);
            throw new RuntimeException("Erreur lors de l'envoi d'email: " + e.getMessage());
        }
    }

    /**
     * Envoyer email de consultation SUR_PLACE (Google Maps)
     */
    public void sendOnSiteConsultationEmail(String to, String memberName, String doctorName, 
                                           String specialty, String dateConsultation, String mapsLink) {
        try {
            Map<String, Object> templateParams = new HashMap<>();
            templateParams.put("to_email", to);
            templateParams.put("member_name", memberName);
            templateParams.put("doctor_name", doctorName);
            templateParams.put("specialty", specialty);
            templateParams.put("consultation_date", dateConsultation);
            templateParams.put("maps_link", mapsLink);
            templateParams.put("consultation_type", "SUR_PLACE");

            sendEmail(templateParams);
            logger.info("Email consultation SUR_PLACE envoyé à: " + to);
        } catch (Exception e) {
            logger.error("Erreur lors de l'envoi d'email de consultation SUR_PLACE: " + e.getMessage(), e);
            throw new RuntimeException("Erreur lors de l'envoi d'email: " + e.getMessage());
        }
    }

    /**
     * Envoyer email de recommandation doctor
     */
    public void sendDoctorRecommendationEmail(String to, String memberName, String doctorName, 
                                             String specialty, String reason) {
        try {
            Map<String, Object> templateParams = new HashMap<>();
            templateParams.put("to_email", to);
            templateParams.put("member_name", memberName);
            templateParams.put("recommended_doctor", doctorName);
            templateParams.put("specialty", specialty);
            templateParams.put("reason", reason);

            sendEmail(templateParams);
            logger.info("Email recommandation doctor envoyé à: " + to);
        } catch (Exception e) {
            logger.error("Erreur lors de l'envoi d'email de recommandation: " + e.getMessage(), e);
            throw new RuntimeException("Erreur lors de l'envoi d'email: " + e.getMessage());
        }
    }

    /**
     * Envoyer email de recommandation produit
     */
    public void sendProductRecommendationEmail(String to, String memberName, String productName, 
                                              String disease, String reason) {
        try {
            Map<String, Object> templateParams = new HashMap<>();
            templateParams.put("to_email", to);
            templateParams.put("member_name", memberName);
            templateParams.put("product_name", productName);
            templateParams.put("disease", disease);
            templateParams.put("reason", reason);

            sendEmail(templateParams);
            logger.info("Email recommandation produit envoyé à: " + to);
        } catch (Exception e) {
            logger.error("Erreur lors de l'envoi d'email de recommandation produit: " + e.getMessage(), e);
            throw new RuntimeException("Erreur lors de l'envoi d'email: " + e.getMessage());
        }
    }

    /**
     * Méthode interne pour envoyer un email via EmailJS API
     */
    private void sendEmail(Map<String, Object> templateParams) throws Exception {
        // Préparer le body de la requête
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("service_id", serviceId);
        requestBody.put("template_id", templateId);
        requestBody.put("user_id", publicKey);
        requestBody.put("accessToken", privateKey);
        requestBody.put("template_params", templateParams);

        String jsonBody = objectMapper.writeValueAsString(requestBody);

        logger.info("Envoi email à EmailJS API...");
        logger.debug("Payload: " + jsonBody);

        // Créer la requête HTTP
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(EMAILJS_API_URL))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        // Envoyer la requête
        HttpClient client = HttpClient.newHttpClient();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        logger.info("EmailJS API Response: " + response.statusCode());
        logger.debug("Response body: " + response.body());

        // Vérifier la réponse
        if (response.statusCode() != 200) {
            throw new RuntimeException("EmailJS API error: " + response.body());
        }
    }

    /**
     * Vérifier la configuration EmailJS
     */
    public boolean isConfigured() {
        return !serviceId.contains("YOUR_") && 
               !templateId.contains("YOUR_") && 
               !publicKey.contains("YOUR_");
    }

    /**
     * Obtenir le statut de la configuration
     */
    public String getConfigurationStatus() {
        if (!isConfigured()) {
            return "❌ EmailJS not configured. Please set the following properties in application.properties:\n" +
                   "  - emailjs.service.id\n" +
                   "  - emailjs.template.id\n" +
                   "  - emailjs.public.key";
        }
        return "✅ EmailJS configured successfully";
    }
}

