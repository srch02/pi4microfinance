package pi.db.piversionbd.service.pre;

import com.google.cloud.vision.v1.*;
import com.google.protobuf.ByteString;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import pi.db.piversionbd.dto.pre.MedicalScanResult;
import pi.db.piversionbd.dto.pre.PatientMedicalMatchDTO;
import pi.db.piversionbd.entities.pre.ExcludedCondition;
import pi.db.piversionbd.entities.pre.MedicalHistory;
import pi.db.piversionbd.repository.ExcludedConditionRepository;
import pi.db.piversionbd.repository.MedicalHistoryRepository;

import java.io.IOException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service de scan médical côté admin.
 * Responsibilities:
 * - Extract text (image OCR or PDF parsing)
 * - Detect excluded conditions from the extracted text
 * - Compare scanned content with stored medical history (consistency check)
 * - Provide keyword-based patient search for investigation workflows
 */
@Service
public class MedicalFormScannerService {

    @Autowired(required = false)
    private ImageAnnotatorClient visionClient;

    @Autowired
    private ExcludedConditionRepository excludedConditionRepository;

    @Autowired
    private MedicalHistoryRepository medicalHistoryRepository;

    /**
     * Extrait le texte d'une image (formulaire scanné) via Google Vision OCR.
     */
    public String extractTextFromImage(MultipartFile imageFile) throws IOException {
        if (visionClient == null) {
            throw new IllegalStateException(
                "Google Vision non configuré. Définissez ml.vision.enabled=true et GOOGLE_APPLICATION_CREDENTIALS.");
        }

        ByteString imgBytes = ByteString.readFrom(imageFile.getInputStream());
        Image img = Image.newBuilder().setContent(imgBytes).build();
        Feature feat = Feature.newBuilder()
                .setType(Feature.Type.DOCUMENT_TEXT_DETECTION)
                .build();
        AnnotateImageRequest request = AnnotateImageRequest.newBuilder()
                .addFeatures(feat)
                .setImage(img)
                .build();

        BatchAnnotateImagesResponse response = visionClient.batchAnnotateImages(List.of(request));
        AnnotateImageResponse imgResponse = response.getResponses(0);

        if (imgResponse.hasError()) {
            throw new IOException("Vision OCR error: " + imgResponse.getError().getMessage());
        }

        TextAnnotation fullText = imgResponse.getFullTextAnnotation();
        return fullText != null ? fullText.getText() : "";
    }

    /**
     * Analyse le texte extrait pour détecter les conditions exclues.
     */
    public MedicalScanResult analyzeMedicalForm(String extractedText) {
        MedicalScanResult result = MedicalScanResult.builder()
                .extractedText(extractedText)
                .detectedConditions(new ArrayList<>())
                .rejected(false)
                .confidenceScore(0.0)
                .build();

        if (extractedText == null || extractedText.isBlank()) {
            result.setConfidenceScore(0.0);
            return result;
        }

        String normalizedText = normalizeForMatching(extractedText);
        List<ExcludedCondition> excludedConditions = excludedConditionRepository.findAll();
        List<String> detected = new ArrayList<>();

        for (ExcludedCondition condition : excludedConditions) {
            String keyword = condition.getConditionName();
            if (keyword != null && !keyword.isBlank() && normalizedText.contains(normalizeForMatching(keyword))) {
                if (!detected.contains(condition.getConditionName())) {
                    detected.add(condition.getConditionName());
                }
            }
        }

        result.setDetectedConditions(detected);
        result.setRejected(!detected.isEmpty());
        if (result.isRejected()) {
            result.setRejectionReason(
                "Conditions exclues détectées: " + String.join(", ", detected));
            result.setConfidenceScore(0.95);
        } else {
            result.setConfidenceScore(normalizedText.length() > 50 ? 0.85 : 0.6);
        }

        return result;
    }

    /**
     * Full scan pipeline.
     * Decision rule:
     * - PDF -> PDFBox text extraction
     * - Otherwise -> Google Vision OCR
     * Then it runs exclusion detection and history consistency enrichment.
     */
    public MedicalScanResult scanMedicalForm(Long preRegistrationId, MultipartFile file) throws IOException {
        String originalName = file.getOriginalFilename() != null
            ? file.getOriginalFilename().toLowerCase()
            : "";
        String contentType = file.getContentType() != null
            ? file.getContentType().toLowerCase()
            : "";

        String text;
        if (originalName.endsWith(".pdf") || contentType.equals("application/pdf")) {
            text = extractTextFromPdf(file);
        } else {
            text = extractTextFromImage(file);
        }
        MedicalScanResult result = analyzeMedicalForm(text);
        enrichWithHistoryConsistency(preRegistrationId, result);
        return result;
    }

    /**
     * Analyse un texte déjà extrait (sans OCR). Utile si OCR est fait ailleurs.
     */
    public MedicalScanResult analyzeExtractedText(String text) {
        return analyzeMedicalForm(text);
    }

    /**
     * Recherche les patients dont l'historique médical contient un texte .
     */
    public List<PatientMedicalMatchDTO> searchPatientsByMedicalText(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }

        String needle = normalizeForMatching(text);
        List<MedicalHistory> matches = medicalHistoryRepository.findAll().stream()
            .filter(mh -> {
                String qa = normalizeForMatching(safe(mh.getQaPayload()));
                String details = normalizeForMatching(safe(mh.getExcludedConditionDetails()));
                return qa.contains(needle) || details.contains(needle);
            })
            .toList();

        return matches.stream()
            .map(mh -> toPatientMatchDto(mh, needle))
            .toList();
    }

    /**
     * Extraction texte pour un PDF (formulaire scanné en PDF).
     */
    private String extractTextFromPdf(MultipartFile pdfFile) throws IOException {
        try (PDDocument document = PDDocument.load(pdfFile.getInputStream())) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }

    private static String normalizeForMatching(String input) {
        if (input == null) return "";
        String lower = input.toLowerCase();
        String nfd = Normalizer.normalize(lower, Normalizer.Form.NFD);
        return nfd.replaceAll("\\p{M}", "");
    }

    /**
     * Compares scanned text and stored history with simple contradiction heuristics.
     * Goal is triage support (admin review), not medical diagnosis.
     */
    private void enrichWithHistoryConsistency(Long preRegistrationId, MedicalScanResult result) {
        if (preRegistrationId == null || result == null || result.getExtractedText() == null) {
            return;
        }

        List<MedicalHistory> histories = medicalHistoryRepository.findByPreRegistration_Id(preRegistrationId);
        if (histories == null || histories.isEmpty()) {
            return;
        }

        String historyRaw = histories.stream()
            .map(h -> safe(h.getExcludedConditionDetails()) + " " + safe(h.getQaPayload()))
            .collect(Collectors.joining(" "));

        String normHistory = normalizeForMatching(historyRaw);
        String normScan = normalizeForMatching(result.getExtractedText());

        if (normScan.isBlank()) {
            result.setConsistentWithHistory(null);
            result.setConsistencyReason("Texte scanné vide ou illisible, comparaison impossible.");
            return;
        }

        // Mots-clés graves à surveiller
        List<String> severeKeywords = Arrays.asList(
            "diabete", "diabetes", "insuffisance renale", "dialyse", "dialysis",
            "cancer", "chimio", "chemotherapie", "chemotherapy", "infarctus",
            "insuline", "insulin", "cirrhose", "avc", "stroke"
        );

        // Phrases indiquant "aucune maladie chronique"
        List<String> noChronicPatterns = Arrays.asList(
            "aucune maladie chronique",
            "pas de maladie chronique",
            "no chronic disease",
            "aucune maladie grave",
            "bonne sante generale"
        );

        boolean historySaysNoChronic = noChronicPatterns.stream()
            .anyMatch(p -> normHistory.contains(p));

        List<String> severeInHistory = severeKeywords.stream()
            .filter(k -> normHistory.contains(k))
            .toList();

        List<String> severeInScan = severeKeywords.stream()
            .filter(k -> normScan.contains(k))
            .toList();

        // Cas 1 : histoire dit "pas de maladie" mais scan montre des maladies graves
        if (historySaysNoChronic && !severeInScan.isEmpty()) {
            result.setConsistentWithHistory(false);
            result.setConsistencyReason(
                "Contradiction: l'historique Q&A indique 'pas de maladie chronique' "
                    + "mais le formulaire scanné mentionne: " + String.join(", ", severeInScan));
            return;
        }

        // Cas 2 : scan montre des maladies graves absentes de l'historique
        List<String> severeOnlyInScan = severeInScan.stream()
            .filter(k -> !severeInHistory.contains(k))
            .toList();

        if (!severeOnlyInScan.isEmpty()) {
            result.setConsistentWithHistory(false);
            result.setConsistencyReason(
                "Le formulaire scanné mentionne des conditions graves non présentes dans l'historique Q&A: "
                    + String.join(", ", severeOnlyInScan));
            return;
        }

        // Cas 3 : historique mentionne des maladies graves mais scan n'en parle pas du tout
        List<String> severeOnlyInHistory = severeInHistory.stream()
            .filter(k -> !severeInScan.contains(k))
            .toList();

        if (!severeOnlyInHistory.isEmpty()) {
            result.setConsistentWithHistory(false);
            result.setConsistencyReason(
                "L'historique Q&A mentionne des conditions graves non retrouvées dans le formulaire scanné: "
                    + String.join(", ", severeOnlyInHistory));
            return;
        }

        // Cas 4 : aucune contradiction évidente
        result.setConsistentWithHistory(true);
        result.setConsistencyReason("Aucune contradiction évidente détectée entre l'historique Q&A et le formulaire scanné.");
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    private static PatientMedicalMatchDTO toPatientMatchDto(MedicalHistory mh, String needle) {
        String qa = safe(mh.getQaPayload());
        String details = safe(mh.getExcludedConditionDetails());

        String matchedIn;
        String source;
        if (normalizeForMatching(qa).contains(needle)) {
            matchedIn = "qaPayload";
            source = qa;
        } else if (normalizeForMatching(details).contains(needle)) {
            matchedIn = "excludedConditionDetails";
            source = details;
        } else {
            matchedIn = "unknown";
            source = qa + " " + details;
        }

        return PatientMedicalMatchDTO.builder()
            .preRegistrationId(mh.getPreRegistration() != null ? mh.getPreRegistration().getId() : null)
            .medicalHistoryId(mh.getId())
            .memberId(mh.getMember() != null ? mh.getMember().getId() : null)
            .cinNumber(mh.getPreRegistration() != null ? mh.getPreRegistration().getCinNumber() : null)
            .status(mh.getPreRegistration() != null && mh.getPreRegistration().getStatus() != null
                ? mh.getPreRegistration().getStatus().name()
                : null)
            .memberEmail(null)
            .matchedIn(matchedIn)
            .matchedSnippet(abbreviate(source, 220))
            .build();
    }

    private static String abbreviate(String s, int max) {
        String v = safe(s).trim();
        if (v.length() <= max) return v;
        return v.substring(0, max) + "...";
    }
}
