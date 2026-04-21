package pi.db.piversionbd.service.health;

import pi.db.piversionbd.entities.health.ChatbotMessage;
import pi.db.piversionbd.entities.health.Doctor;
import pi.db.piversionbd.entities.health.Produit;

public interface IChatbotService {
    ChatbotMessage processUserMessage(Long memberId, String userMessage);
    String generateResponse(String userMessage, Doctor recommendedDoctor, Produit recommendedProduct, String detectedMaladie);
    Doctor recommendDoctorForMaladie(String maladie);
    Produit recommendProductForMaladie(String maladie);
    String detectMaladie(String userMessage);
}

