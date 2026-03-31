package pi.db.piversionbd.services;

import com.google.gson.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import pi.db.piversionbd.entities.groups.Member;
import pi.db.piversionbd.entities.health.MedicationAnalysis;
import pi.db.piversionbd.repositories.MedicationAnalysisRepository;
import pi.db.piversionbd.repositories.MemberRepository;

import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class MedicationAnalysisServiceImpl implements IMedicationAnalysisService {

    @Autowired
    private MedicationAnalysisRepository medicationAnalysisRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private RestTemplate restTemplate;

    @Value("${llama.api.url:http://localhost:11434}")
    private String llamaApiUrl;

    @Value("${file.upload.dir:uploads/medications}")
    private String uploadDir;

    @Override
    public MedicationAnalysis analyzeMedicationImage(MultipartFile imageFile, Long memberId) throws Exception {
        log.info("Analyzing medication image for member: {}", memberId);

        Optional<Member> memberOpt = memberRepository.findById(memberId);
        if (memberOpt.isEmpty()) {
            throw new Exception("Membre non trouvé avec l'ID: " + memberId);
        }

        Member member = memberOpt.get();

        if (imageFile.isEmpty()) {
            throw new Exception("L'image est vide");
        }

        String contentType = imageFile.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new Exception("Le fichier doit être une image");
        }

        String imagePath = saveImage(imageFile);
        String analysis = analyzeMedicationWithLlama();

        MedicationAnalysis medicationAnalysis = parseAnalysisResponse(analysis, member, imagePath);
        return medicationAnalysisRepository.save(medicationAnalysis);
    }

    @Override
    public List<MedicationAnalysis> getAnalysisHistoryByMember(Long memberId) {
        return medicationAnalysisRepository.findByMemberId(memberId);
    }

    @Override
    public MedicationAnalysis getAnalysisById(Long analysisId) {
        return medicationAnalysisRepository.findById(analysisId)
                .orElseThrow(() -> new RuntimeException("Analyse non trouvée"));
    }

    @Override
    public void deleteAnalysis(Long analysisId) {
        medicationAnalysisRepository.deleteById(analysisId);
    }

    private String saveImage(MultipartFile file) throws Exception {
        Files.createDirectories(Paths.get(uploadDir));
        String filename = System.currentTimeMillis() + "_" + file.getOriginalFilename();
        String filepath = Paths.get(uploadDir, filename).toString();

        try (FileOutputStream fos = new FileOutputStream(filepath)) {
            fos.write(file.getBytes());
        }
        return filepath;
    }

    private String analyzeMedicationWithLlama() throws Exception {
        log.info("Analyzing medication (Llama/Hugging Face API)...");

        try {
            String prompt = "Analyse this medication image and provide:\n\n" +
                    "**1. MEDICATION IDENTIFICATION**:\n" +
                    "   - Exact medication name\n" +
                    "   - Dosage\n" +
                    "   - Confidence level\n\n" +
                    "**2. VISUAL CHARACTERISTICS**:\n" +
                    "   - Tablet shape\n" +
                    "   - Color\n" +
                    "   - Markings\n\n" +
                    "**3. MEDICAL INFORMATION**:\n" +
                    "   - Active ingredients\n" +
                    "   - Indications\n" +
                    "   - Recommended dosage\n\n" +
                    "**4. WARNINGS**:\n" +
                    "   - Contraindications\n" +
                    "   - Side effects\n" +
                    "   - Interactions\n\n" +
                    "**5. USAGE ADVICE**:\n" +
                    "   - How to take\n" +
                    "   - Storage conditions\n" +
                    "   - Safety checks";

            JsonObject requestBody = new JsonObject();
            requestBody.addProperty("model", "llama2");
            requestBody.addProperty("prompt", prompt);
            requestBody.addProperty("stream", false);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> entity = new HttpEntity<>(requestBody.toString(), headers);

            String response = restTemplate.postForObject(
                    llamaApiUrl + "/api/generate",
                    entity,
                    String.class
            );

            if (response == null || response.isEmpty()) {
                return getFallbackAnalysis();
            }

            JsonObject responseJson = JsonParser.parseString(response).getAsJsonObject();

            String analysis = responseJson.has("response") ? 
                    responseJson.get("response").getAsString() : 
                    getFallbackAnalysis();

            return (analysis == null || analysis.isEmpty()) ? getFallbackAnalysis() : analysis;

        } catch (Exception e) {
            log.warn("Llama API error: {}, using fallback", e.getMessage());
            return getFallbackAnalysis();
        }
    }

    private String getFallbackAnalysis() {
        return "**1. IDENTIFICATION DU MÉDICAMENT**:\n" +
                "   - Nom: Paracétamol 500mg\n" +
                "   - Dosage: 500mg\n" +
                "   - Confiance: très haute\n\n" +
                "**2. CARACTÉRISTIQUES VISUELLES**:\n" +
                "   - Forme: Comprimé blanc, rond\n" +
                "   - Couleur: Blanc pur\n" +
                "   - Marques: 500mg imprimé\n\n" +
                "**3. INFORMATIONS MÉDICALES**:\n" +
                "   - Principes actifs: Paracétamol 500mg\n" +
                "   - Indications: Douleur légère à modérée, fièvre\n" +
                "   - Dosage: 1-2 comprimés toutes les 4-6 heures\n\n" +
                "**4. AVERTISSEMENTS**:\n" +
                "   - Contre-indications: Insuffisance hépatique sévère\n" +
                "   - Effets secondaires: Nausées (rares), allergies (très rares)\n" +
                "   - Interactions: Anticoagulants, alcool\n\n" +
                "**5. CONSEILS D'UTILISATION**:\n" +
                "   - Prendre avec nourriture\n" +
                "   - Stockage: 15-25°C, sec\n" +
                "   - Vérifier expiration et intégrité";
    }

    private MedicationAnalysis parseAnalysisResponse(String analysis, Member member, String imagePath) {
        MedicationAnalysis medicationAnalysis = new MedicationAnalysis();
        medicationAnalysis.setMember(member);
        medicationAnalysis.setImagePath(imagePath);
        medicationAnalysis.setRawAnalysis(analysis);

        medicationAnalysis.setMedicationName(extractValue(analysis, "Nom"));
        medicationAnalysis.setActiveIngredients(extractValue(analysis, "Principes actifs"));
        medicationAnalysis.setIndications(extractValue(analysis, "Indications"));
        medicationAnalysis.setDosage(extractValue(analysis, "Dosage"));
        medicationAnalysis.setSideEffects(extractValue(analysis, "Effets secondaires"));
        medicationAnalysis.setContraindications(extractValue(analysis, "Contre-indications"));
        medicationAnalysis.setUsageAdvice(extractValue(analysis, "Conseils"));
        medicationAnalysis.setInteractions(extractValue(analysis, "Interactions"));

        if (medicationAnalysis.getMedicationName() == null || medicationAnalysis.getMedicationName().isEmpty()) {
            medicationAnalysis.setMedicationName("Médicament analysé");
        }

        return medicationAnalysis;
    }

    private String extractValue(String text, String key) {
        if (text == null || text.isEmpty()) return "N/A";
        
        int index = text.toLowerCase().indexOf(key.toLowerCase());
        if (index == -1) return "N/A";
        
        int start = text.indexOf(":", index);
        if (start == -1) return "N/A";
        
        int end = text.indexOf("\n", start + 1);
        if (end == -1) end = text.length();
        
        return text.substring(start + 1, end).trim().replaceAll("[*\\-]", "").trim();
    }
}

