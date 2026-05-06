package pi.db.piversionbd.service.score;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pi.db.piversionbd.entities.groups.Membership;
import pi.db.piversionbd.entities.pre.DocumentUpload;
import pi.db.piversionbd.entities.score.Claim;
import pi.db.piversionbd.entities.score.ClaimScoring;
import pi.db.piversionbd.entities.score.ClaimStatus;
import pi.db.piversionbd.exception.ResourceNotFoundException;
import pi.db.piversionbd.repository.groups.MembershipRepository;
import pi.db.piversionbd.repository.groups.PaymentRepository;
import pi.db.piversionbd.repository.pre.DocumentUploadRepository;
import pi.db.piversionbd.repository.score.ClaimRepository;
import pi.db.piversionbd.repository.score.ClaimScoringRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ClaimScoringService {

    private final ClaimRepository claimRepository;
    private final ClaimScoringRepository claimScoringRepository;
    private final PaymentRepository paymentRepository;
    private final MembershipRepository membershipRepository;
    private final DocumentUploadRepository documentUploadRepository;

    @Value("${claims.scoring.auto-approve-threshold:85}")
    private BigDecimal autoApproveThreshold;

    @Value("${claims.scoring.manual-review-threshold:60}")
    private BigDecimal manualReviewThreshold;

    @Value("${claims.scoring.fraud-document-threshold:0.80}")
    private BigDecimal fraudDocumentThreshold;

    public Claim applyAutomaticScoringOnSubmit(Long claimId) {
        Claim claim = claimRepository.findDetailsById(claimId)
                .orElseThrow(() -> new ResourceNotFoundException("Claim introuvable: " + claimId));

        Long memberId = claim.getMember() != null ? claim.getMember().getId() : null;
        Long groupId = claim.getGroup() != null ? claim.getGroup().getId() : null;

        List<DocumentUpload> documents = documentUploadRepository.findByClaim_Id(claimId);
        long paymentCount = memberId != null && groupId != null
                ? paymentRepository.countByMember_IdAndGroup_Id(memberId, groupId) : 0;

        List<String> indicators = new ArrayList<>();

        // Simple scoring
        BigDecimal totalScore = calculateSimpleScore(claim, documents, paymentCount, indicators);
        boolean fraudDetected = hasFraudIndicator(indicators);

        ClaimScoring scoring = claimScoringRepository.findByClaimId(claimId)
                .orElseGet(ClaimScoring::new);
        scoring.setClaim(claim);
        scoring.setReliabilityScore(totalScore);
        scoring.setDocumentScore(totalScore);
        scoring.setMedicalScore(totalScore);
        scoring.setComplianceScore(totalScore);
        scoring.setTotalScore(totalScore);
        scoring.setExcludedConditionDetected(false);
        scoring.setFraudIndicators(String.join("\n", indicators));
        scoring.setScoredAt(LocalDateTime.now());
        claimScoringRepository.save(scoring);

        claim.setFinalScoreSnapshot(totalScore);
        claim.setExcludedConditionDetected(false);
        claim.setDecisionAt(LocalDateTime.now());
        claim.setDecisionComment("Score: " + totalScore + "/100. " + String.join(" ", indicators));

        applyDecision(claim, totalScore, totalScore, fraudDetected);
        return claimRepository.save(claim);
    }

    private BigDecimal calculateSimpleScore(Claim claim, List<DocumentUpload> documents,
                                             long paymentCount, List<String> indicators) {
        double score = 65.0; // Base — everyone starts at MANUAL_REVIEW territory

        // Document bonus
        if (documents != null && !documents.isEmpty()) {
            score += 15;
            indicators.add("Document joint.");
        } else {
            indicators.add("Aucun document joint.");
        }

        // Amount factor
        BigDecimal amount = claim.getAmountRequested();
        if (amount != null) {
            if (amount.compareTo(BigDecimal.valueOf(100)) <= 0) {
                score += 10;
            } else if (amount.compareTo(BigDecimal.valueOf(500)) <= 0) {
                score += 5;
            } else {
                score -= 5;
                indicators.add("Montant élevé.");
            }
        }

        // Payment history bonus
        if (paymentCount >= 6) {
            score += 10;
        } else if (paymentCount >= 1) {
            score += 5;
        }

        // Fraud check
        if (documents != null) {
            for (DocumentUpload doc : documents) {
                if (doc.getFraudDetectionScore() != null
                        && doc.getFraudDetectionScore() >= 0.80f) {
                    indicators.add("FRAUD: document suspect.");
                    score -= 50;
                }
            }
        }

        return bd(score);
    }

    /*
     * On garde cet ancien nom parce que ClaimService l'appelle déjà.
     */
    public Claim applyDefaultScoringOnSubmit(Long claimId) {
        return applyAutomaticScoringOnSubmit(claimId);
    }

    private BigDecimal calculateReliabilityScore(long paymentCount, List<String> indicators) {
        // Based on payment history — how long the member has been contributing
        double score;
        if (paymentCount >= 12) {
            score = 100;
        } else if (paymentCount >= 6) {
            score = 85;
        } else if (paymentCount >= 3) {
            score = 70;
        } else if (paymentCount >= 1) {
            score = 55;
            indicators.add("Reliability: historique de paiement faible (" + paymentCount + " paiement(s)).");
        } else {
            score = 40;
            indicators.add("Reliability: aucun paiement trouvé.");
        }
        return bd(score);
    }

    private static final List<String> MEDICAL_KEYWORDS = List.of(
        // French
        "consultation", "ordonnance", "diagnostic", "médecin", "docteur", "clinique",
        "hôpital", "pharmacie", "médicament", "traitement", "patient", "prescription",
        "analyse", "laboratoire", "radiologie", "chirurgie", "urgence", "infirmier",
        "certificat médical", "facture", "remboursement", "soin", "acte médical",
        // Arabic transliterated
        "استشارة", "وصفة", "طبيب", "مستشفى", "دواء", "علاج",
        // English
        "consultation", "prescription", "diagnosis", "doctor", "hospital", "pharmacy",
        "medication", "treatment", "patient", "laboratory", "invoice", "receipt",
        "medical", "clinic", "surgery", "emergency"
    );

    private int countMedicalKeywords(String text) {
        if (text == null || text.isBlank()) return 0;
        String lower = text.toLowerCase();
        return (int) MEDICAL_KEYWORDS.stream()
                .filter(kw -> lower.contains(kw.toLowerCase()))
                .count();
    }

    private BigDecimal calculateDocumentScore(List<DocumentUpload> documents, List<String> indicators) {
        if (documents == null || documents.isEmpty()) {
            indicators.add("Document: aucun bulletin attaché.");
            return bd(15);
        }

        double score = 30.0;

        boolean hasClaimBulletin = false;
        boolean hasValidContentType = false;
        boolean hasValidSize = false;
        boolean hasExtractedText = false;
        int totalMedicalKeywords = 0;

        for (DocumentUpload doc : documents) {
            String documentType = value(doc.getDocumentType());
            String contentType = value(doc.getContentType()).toLowerCase();

            if ("CLAIM_BULLETIN".equalsIgnoreCase(documentType)) {
                hasClaimBulletin = true;
            }

            if (contentType.contains("pdf")
                    || contentType.contains("png")
                    || contentType.contains("jpeg")
                    || contentType.contains("jpg")) {
                hasValidContentType = true;
            }

            if (doc.getSizeBytes() != null
                    && doc.getSizeBytes() > 0
                    && doc.getSizeBytes() <= 10L * 1024L * 1024L) {
                hasValidSize = true;
            }

            // Fraud detection score
            if (doc.getFraudDetectionScore() != null) {
                BigDecimal fraudScore = BigDecimal.valueOf(doc.getFraudDetectionScore());
                if (fraudScore.compareTo(fraudDocumentThreshold) >= 0) {
                    indicators.add("FRAUD: document suspect, fraudDetectionScore=" + doc.getFraudDetectionScore());
                    score -= 45;
                } else if (fraudScore.compareTo(BigDecimal.valueOf(0.50)) >= 0) {
                    indicators.add("Document: suspicion moyenne, fraudDetectionScore=" + doc.getFraudDetectionScore());
                    score -= 15;
                }
            }

            // Content analysis — check extracted text for medical keywords
            if (doc.getExtractedText() != null && !doc.getExtractedText().isBlank()) {
                hasExtractedText = true;
                int kwCount = countMedicalKeywords(doc.getExtractedText());
                totalMedicalKeywords += kwCount;
            } else if (contentType.contains("image") || contentType.contains("png")
                    || contentType.contains("jpeg") || contentType.contains("jpg")) {
                // Image without OCR — give benefit of the doubt, treat as partial content
                hasExtractedText = true;
                totalMedicalKeywords += 3; // assume some medical content in image
            }
        }

        // Base bonuses
        if (hasClaimBulletin) {
            score += 20; // reduced from 30 — content must also be valid
        } else {
            indicators.add("Document: aucun document marqué CLAIM_BULLETIN.");
        }

        if (hasValidContentType) {
            score += 15; // reduced from 20
        } else {
            indicators.add("Document: type de fichier invalide ou inconnu.");
        }

        if (hasValidSize) {
            score += 10; // reduced from 15
        } else {
            indicators.add("Document: taille de fichier invalide ou inconnue.");
        }

        // Content quality bonus — based on medical keywords found
        if (hasExtractedText) {
            if (totalMedicalKeywords >= 5) {
                score += 25; // rich medical content
                indicators.add("Document: contenu médical riche (" + totalMedicalKeywords + " mots-clés détectés).");
            } else if (totalMedicalKeywords >= 2) {
                score += 15; // some medical content
                indicators.add("Document: contenu médical partiel (" + totalMedicalKeywords + " mots-clés détectés).");
            } else if (totalMedicalKeywords == 1) {
                score += 5;
                indicators.add("Document: contenu médical minimal (1 mot-clé détecté).");
            } else {
                score -= 10; // text extracted but no medical keywords
                indicators.add("Document: texte extrait mais aucun mot-clé médical détecté — contenu suspect.");
            }
        } else if (hasClaimBulletin) {
            // File exists but no text extracted — could be image without OCR
            indicators.add("Document: aucun texte extrait du bulletin (OCR non disponible ou document vide).");
        }

        return bd(score);
    }

    private BigDecimal calculateMedicalScore(
            Claim claim,
            Membership membership,
            List<String> indicators
    ) {
        if (claim.getAmountRequested() == null
                || claim.getAmountRequested().compareTo(BigDecimal.ZERO) <= 0) {
            indicators.add("Medical: montant demandé invalide.");
            return bd(0);
        }

        BigDecimal amountRequested = claim.getAmountRequested();

        if (membership == null) {
            indicators.add("Medical: membership introuvable.");
            return bd(30);
        }

        if (membership.getAnnualLimit() != null && membership.getAnnualLimit() > 0f) {
            BigDecimal remaining = BigDecimal.valueOf(membership.getRemainingAnnualAmount());

            if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
                indicators.add("Medical: couverture annuelle épuisée.");
                return bd(0);
            }

            BigDecimal ratio = amountRequested.divide(remaining, 4, RoundingMode.HALF_UP);

            if (ratio.compareTo(BigDecimal.valueOf(0.30)) <= 0) {
                return bd(100);
            }

            if (ratio.compareTo(BigDecimal.valueOf(0.60)) <= 0) {
                return bd(85);
            }

            if (ratio.compareTo(BigDecimal.valueOf(0.85)) <= 0) {
                indicators.add("Medical: montant élevé par rapport à la couverture restante.");
                return bd(70);
            }

            if (ratio.compareTo(BigDecimal.ONE) <= 0) {
                indicators.add("Medical: montant très proche de la couverture restante.");
                return bd(55);
            }

            indicators.add("Medical: montant supérieur à la couverture restante.");
            return bd(0);
        }

        /*
         * Si pas de plafond annuel configuré, on utilise une logique simple par montant.
         */
        if (amountRequested.compareTo(BigDecimal.valueOf(100)) <= 0) {
            return bd(95);
        }

        if (amountRequested.compareTo(BigDecimal.valueOf(300)) <= 0) {
            return bd(80);
        }

        if (amountRequested.compareTo(BigDecimal.valueOf(700)) <= 0) {
            indicators.add("Medical: montant élevé, revue recommandée.");
            return bd(65);
        }

        indicators.add("Medical: montant très élevé, revue manuelle recommandée.");
        return bd(45);
    }

    private BigDecimal calculateComplianceScore(
            Claim claim,
            Membership membership,
            List<String> indicators
    ) {
        double score = 60.0; // Base score — member exists and submitted a valid claim

        if (membership == null) {
            // No membership — not a blocking factor, just a neutral indicator
            indicators.add("Compliance: aucune membership active trouvée (score de base appliqué).");
            return bd(score);
        }

        // Membership active bonus
        if (Membership.STATUS_ACTIVE.equalsIgnoreCase(membership.getStatus())) {
            score += 20;
        } else {
            indicators.add("Compliance: membership non active.");
            score -= 10;
        }

        // Annual limit check
        if (membership.getAnnualLimit() == null || membership.getAnnualLimit() <= 0f) {
            score += 10; // No limit configured — neutral
        } else if (membership.getRemainingAnnualAmount() > 0f) {
            score += 10;
        } else {
            indicators.add("Compliance: plafond annuel consommé.");
            score -= 20;
        }

        // Amount validity
        if (claim.getAmountRequested() != null
                && claim.getAmountRequested().compareTo(BigDecimal.ZERO) > 0) {
            score += 10;
        } else {
            indicators.add("Compliance: montant demandé invalide.");
            score -= 10;
        }

        return bd(score);
    }

    private BigDecimal calculateTotalScore(
            BigDecimal reliabilityScore,
            BigDecimal documentScore,
            BigDecimal medicalScore,
            BigDecimal complianceScore
    ) {
        BigDecimal total = BigDecimal.ZERO;

        total = total.add(reliabilityScore.multiply(BigDecimal.valueOf(0.25)));
        total = total.add(documentScore.multiply(BigDecimal.valueOf(0.25)));
        total = total.add(medicalScore.multiply(BigDecimal.valueOf(0.30)));
        total = total.add(complianceScore.multiply(BigDecimal.valueOf(0.20)));

        return total.setScale(2, RoundingMode.HALF_UP);
    }

    private void applyDecision(
            Claim claim,
            BigDecimal totalScore,
            BigDecimal documentScore,
            boolean fraudDetected
    ) {
        if (fraudDetected) {
            claim.setStatus(ClaimStatus.REJECTED_FRAUD);
            claim.setAmountApproved(null);
            return;
        }

        if (totalScore.compareTo(autoApproveThreshold) >= 0
                && documentScore.compareTo(BigDecimal.valueOf(70)) >= 0) {
            claim.setStatus(ClaimStatus.APPROVED_AUTO);
            claim.setAmountApproved(claim.getAmountRequested());
            return;
        }

        if (totalScore.compareTo(manualReviewThreshold) >= 0) {
            claim.setStatus(ClaimStatus.MANUAL_REVIEW);
            claim.setAmountApproved(null);
            return;
        }

        claim.setStatus(ClaimStatus.REJECTED_LOW_SCORE);
        claim.setAmountApproved(null);
    }

    private String buildDecisionComment(BigDecimal totalScore, List<String> indicators) {
        StringBuilder sb = new StringBuilder();

        sb.append("Automatic scoring completed. Total score=")
                .append(totalScore)
                .append("/100.");

        if (!indicators.isEmpty()) {
            sb.append("\nIndicators:\n");

            for (String indicator : indicators) {
                sb.append("- ").append(indicator).append("\n");
            }
        }

        return sb.toString();
    }

    private boolean hasFraudIndicator(List<String> indicators) {
        return indicators.stream()
                .anyMatch(indicator -> indicator != null && indicator.startsWith("FRAUD:"));
    }

    private BigDecimal bd(double value) {
        double capped = Math.max(0.0, Math.min(100.0, value));

        return BigDecimal.valueOf(capped)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private String value(String value) {
        return value == null ? "" : value;
    }
    @Transactional(readOnly = true)
    public ClaimScoring getById(Long id) {
        return claimScoringRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ClaimScoring introuvable: " + id));
    }

    @Transactional(readOnly = true)
    public ClaimScoring getByClaimId(Long claimId) {
        return claimScoringRepository.findByClaimId(claimId)
                .orElseThrow(() -> new ResourceNotFoundException("ClaimScoring introuvable pour claimId: " + claimId));
    }

    public ClaimScoring upsertByClaimId(Long claimId, ClaimScoring request) {
        Claim claim = claimRepository.findDetailsById(claimId)
                .orElseThrow(() -> new ResourceNotFoundException("Claim introuvable: " + claimId));

        ClaimScoring scoring = claimScoringRepository.findByClaimId(claimId)
                .orElseGet(ClaimScoring::new);

        scoring.setClaim(claim);

        scoring.setReliabilityScore(request.getReliabilityScore());
        scoring.setDocumentScore(request.getDocumentScore());
        scoring.setMedicalScore(request.getMedicalScore());
        scoring.setComplianceScore(request.getComplianceScore());

        BigDecimal totalScore = request.getTotalScore();

        if (totalScore == null) {
            totalScore = calculateTotalScore(
                    request.getReliabilityScore() != null ? request.getReliabilityScore() : BigDecimal.ZERO,
                    request.getDocumentScore() != null ? request.getDocumentScore() : BigDecimal.ZERO,
                    request.getMedicalScore() != null ? request.getMedicalScore() : BigDecimal.ZERO,
                    request.getComplianceScore() != null ? request.getComplianceScore() : BigDecimal.ZERO
            );
        }

        scoring.setTotalScore(totalScore);
        scoring.setExcludedConditionDetected(request.isExcludedConditionDetected());
        scoring.setFraudIndicators(request.getFraudIndicators());
        scoring.setScoredAt(LocalDateTime.now());

        ClaimScoring saved = claimScoringRepository.save(scoring);

        claim.setFinalScoreSnapshot(totalScore);
        claim.setExcludedConditionDetected(request.isExcludedConditionDetected());
        claim.setDecisionAt(LocalDateTime.now());
        claim.setDecisionComment("Manual scoring updated. Total score=" + totalScore + "/100.");

        if (request.isExcludedConditionDetected()) {
            claim.setStatus(ClaimStatus.REJECTED_EXCLUSION);
            claim.setAmountApproved(null);
        } else {
            applyDecision(
                    claim,
                    totalScore,
                    request.getDocumentScore() != null ? request.getDocumentScore() : BigDecimal.ZERO,
                    false
            );
        }

        claimRepository.save(claim);

        return saved;
    }
}