package pi.db.piversionbd.controller.health;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pi.db.piversionbd.entities.health.ChatbotMessage;
import pi.db.piversionbd.service.health.IChatbotService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/chatbot")
@CrossOrigin(origins = "*")
@Tag(name = "Chatbot", description = "API for AI Health Chatbot")
public class ChatbotController {

    @Autowired
    private IChatbotService chatbotService;

    @PostMapping("/message")
    @Operation(summary = "Send message to chatbot", description = "Send a health-related message and get AI recommendations")
    @ApiResponse(responseCode = "200", description = "Response generated successfully")
    public ResponseEntity<ChatbotMessage> sendMessage(
            @RequestParam(required = false) Long memberId,
            @RequestParam String message) {

        ChatbotMessage response = chatbotService.processUserMessage(memberId, message);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping("/ask")
    @Operation(summary = "Ask chatbot about a health issue", description = "Ask the chatbot about symptoms or health concerns")
    @ApiResponse(responseCode = "200", description = "Response generated successfully")
    public ResponseEntity<Map<String, Object>> askChatbot(
            @RequestParam(required = false) Long memberId,
            @RequestBody Map<String, String> request) {

        String message = request.get("message");
        ChatbotMessage chatbotMessage = chatbotService.processUserMessage(memberId, message);

        Map<String, Object> response = new HashMap<>();
        response.put("id", chatbotMessage.getId());
        response.put("userMessage", chatbotMessage.getUserMessage());
        response.put("botResponse", chatbotMessage.getBotResponse());
        response.put("detectedMaladie", chatbotMessage.getDetectedMaladie());
        response.put("recommendedDoctorId", chatbotMessage.getRecommendedDoctorId());
        response.put("recommendedProductId", chatbotMessage.getRecommendedProductId());
        response.put("timestamp", chatbotMessage.getCreatedAt());

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/detect-maladie")
    @Operation(summary = "Detect maladie from text", description = "Detects health condition from user input text")
    @ApiResponse(responseCode = "200", description = "Maladie detected")
    public ResponseEntity<Map<String, String>> detectMaladie(@RequestParam String text) {
        String detectedMaladie = chatbotService.detectMaladie(text);

        Map<String, String> response = new HashMap<>();
        response.put("text", text);
        response.put("detectedMaladie", detectedMaladie);

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/recommend-doctor/{maladie}")
    @Operation(summary = "Get recommended doctor for maladie", description = "Get a doctor recommendation based on health condition")
    @ApiResponse(responseCode = "200", description = "Doctor recommendation found")
    public ResponseEntity<Map<String, Object>> recommendDoctor(@PathVariable String maladie) {
        var doctor = chatbotService.recommendDoctorForMaladie(maladie);

        Map<String, Object> response = new HashMap<>();
        response.put("maladie", maladie);

        if (doctor != null) {
            response.put("doctorFound", true);
            response.put("id", doctor.getId());
            response.put("name", doctor.getName());
            response.put("email", doctor.getEmail());
            response.put("specialite", doctor.getSpecialite());
            response.put("typeDoctor", doctor.getTypeDoctor());
        } else {
            response.put("doctorFound", false);
            response.put("message", "No doctor found for this condition");
        }

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/recommend-product/{maladie}")
    @Operation(summary = "Get recommended product for maladie", description = "Get a product recommendation based on health condition")
    @ApiResponse(responseCode = "200", description = "Product recommendation found")
    public ResponseEntity<Map<String, Object>> recommendProduct(@PathVariable String maladie) {
        var product = chatbotService.recommendProductForMaladie(maladie);

        Map<String, Object> response = new HashMap<>();
        response.put("maladie", maladie);

        if (product != null) {
            response.put("productFound", true);
            response.put("id", product.getId());
            response.put("name", product.getName());
            response.put("typeProduit", product.getTypeProduit());
            response.put("maladieProduit", product.getMaladieProduit());
        } else {
            response.put("productFound", false);
            response.put("message", "No product found for this condition");
        }

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/health-advice/{maladie}")
    @Operation(summary = "Get health advice for maladie", description = "Get general health advice for a specific condition")
    @ApiResponse(responseCode = "200", description = "Health advice provided")
    public ResponseEntity<Map<String, String>> getHealthAdvice(@PathVariable String maladie) {
        Map<String, String> response = new HashMap<>();
        response.put("maladie", maladie);
        response.put("message", "Health advice functionality available through chatbot messages");

        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}

