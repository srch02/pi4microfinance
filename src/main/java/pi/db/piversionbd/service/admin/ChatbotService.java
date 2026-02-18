package pi.db.piversionbd.service.admin;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pi.db.piversionbd.repository.admin.SystemAlertRepository;

@Service
@RequiredArgsConstructor
public class ChatbotService {

    private final SystemAlertRepository systemAlertRepository;

    public String answer(String question) {
        if (question == null || question.isBlank()) {
            return "Please provide a valid question.";
        }
        String q = question.toLowerCase();
        if (q.contains("critical")) {
            long count = systemAlertRepository.countBySeverity("CRITICAL");
            return "There are " + count + " critical alerts";
        }
        if (q.contains("inactive")) {
            long count = systemAlertRepository.countByActive(false);
            return "There are " + count + " inactive alerts";
        }
        if (q.contains("active")) {
            long count = systemAlertRepository.countByActive(true);
            return "There are " + count + " active alerts";
        }
        if (q.contains("total alerts")) {
            long count = systemAlertRepository.count();
            return "There are " + count + " total alerts";
        }
        if (q.contains("region")) {
            // Extract region name after keyword 'region'
            String region = extractRegion(question);
            if (region == null || region.isBlank()) {
                return "Please specify a region";
            }
            long count = systemAlertRepository.countByRegion(region);
            return "There are " + count + " alerts in region " + region;
        }
        return "I don't understand the question";
    }

    private String extractRegion(String question) {
        String[] tokens = question.split("\\s+");
        for (int i = 0; i < tokens.length; i++) {
            if (tokens[i].equalsIgnoreCase("region")) {
                if (i + 1 < tokens.length) {
                    return tokens[i + 1].replaceAll("[^A-Za-z0-9_-]", "");
                }
            }
        }
        return null;
    }
}

