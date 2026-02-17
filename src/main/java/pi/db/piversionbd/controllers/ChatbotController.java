package pi.db.piversionbd.controllers;

import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pi.db.piversionbd.services.ChatbotService;

@RestController
@RequestMapping("/api/chatbot")
public class ChatbotController {

    @Autowired
    private ChatbotService chatbotService;

    @PostMapping("/ask")
    public ResponseEntity<ChatbotAnswer> ask(@RequestBody ChatbotQuestion question) {
        String answer = chatbotService.answer(question.getQuestion());
        ChatbotAnswer dto = new ChatbotAnswer();
        dto.setAnswer(answer);
        return ResponseEntity.ok(dto);
    }

    @Data
    public static class ChatbotQuestion {
        private String question;
    }

    @Data
    public static class ChatbotAnswer {
        private String answer;
    }
}
