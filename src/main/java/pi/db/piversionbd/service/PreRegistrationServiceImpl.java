package pi.db.piversionbd.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import pi.db.piversionbd.controller.PreRegistrationRequestDTO;
import pi.db.piversionbd.controller.PreRegistrationResponseDTO;
import pi.db.piversionbd.controller.PreRegistrationSummaryDTO;
import pi.db.piversionbd.controller.PreRegistrationException;
import pi.db.piversionbd.entities.admin.AdminReviewQueueItem;
import pi.db.piversionbd.entities.groups.Member;
import pi.db.piversionbd.entities.groups.PackageType;
import pi.db.piversionbd.entities.pre.*;
import pi.db.piversionbd.repository.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Implements Module 5: Pre-Registration & Insurance flow.
 * Steps: Identity (CIN duplicate/blacklist) → Medical declaration → Excluded conditions → Risk & price → Admin queue.
 */
@Service
@RequiredArgsConstructor
public class PreRegistrationServiceImpl implements IPreRegistrationService {

    private static final float BASE_MONTHLY_PRICE = 25.0f;
    private static final float MAX_MONTHLY_PRICE = 70.0f;

    private final PreRegistrationRepository preRegistrationRepository;
    private final BlacklistRepository blacklistRepository;
    private final ExcludedConditionRepository excludedConditionRepository;
    private final MedicalHistoryRepository medicalHistoryRepository;
    private final RiskAssessmentRepository riskAssessmentRepository;
    private final DocumentUploadRepository documentUploadRepository;
    private final AdminReviewQueueItemRepository adminReviewQueueItemRepository;
    private final MemberRepository memberRepository;

    @Override
    @Transactional
    public PreRegistrationResponseDTO submitPreRegistration(PreRegistrationRequestDTO requestDTO) {
        String cin = normalizeCin(requestDTO.getCinNumber());
        if (cin == null || cin.isBlank()) {
            throw new PreRegistrationException("CIN number is required.");
        }

        // Step 1: Duplicate CIN (already registered)
        if (preRegistrationRepository.existsByCinNumber(cin)) {
            throw new PreRegistrationException("You are already registered.");
        }

        // Step 1: Blacklist
        if (blacklistRepository.existsByCinNumber(cin)) {
            throw new PreRegistrationException("Registration not allowed: you are on the blacklist.");
        }

        // Build full medical declaration for analysis
        String declaration = buildMedicalDeclaration(requestDTO);

        // Step 3: Excluded conditions
        List<ExcludedCondition> excluded = excludedConditionRepository.findByAutoRejectTrue();
        String excludedMatch = findExcludedConditionInDeclaration(declaration, excluded);
        if (excludedMatch != null) {
            throw new PreRegistrationException(
                "Sorry, your medical condition requires premium insurance that we cannot provide.");
        }

        // Step 4: Fraud detection & risk coefficient and personalized price
        float fraudScore = detectFraudScore(requestDTO, declaration);
        boolean hasChronicIllness = hasChronicIllness(declaration);
        float riskCoefficient = calculateRiskCoefficient(requestDTO, declaration);
        float calculatedPrice = Math.round(BASE_MONTHLY_PRICE * riskCoefficient * 100f) / 100f;
        calculatedPrice = Math.min(MAX_MONTHLY_PRICE, calculatedPrice);

        // Persist: PreRegistration, MedicalHistory, RiskAssessment, AdminReviewQueueItem
        PreRegistration pre = new PreRegistration();
        pre.setCinNumber(cin);
        pre.setStatus(PreRegistrationStatus.PENDING_REVIEW);
        pre.setFraudScore(fraudScore);
        pre.setCreatedAt(LocalDateTime.now());
        pre = preRegistrationRepository.save(pre);

        MedicalHistory medical = new MedicalHistory();
        medical.setPreRegistration(pre);
        medical.setMember(null);
        medical.setExcludedConditionDetails(declaration);
        medicalHistoryRepository.save(medical);

        RiskAssessment risk = new RiskAssessment();
        risk.setPreRegistration(pre);
        risk.setRiskCoefficient(riskCoefficient);
        risk.setCalculatedPrice(calculatedPrice);
        risk.setExclusions(hasChronicIllness ? "This chronic illness is not covered, but other conditions are." : null);
        riskAssessmentRepository.save(risk);

        AdminReviewQueueItem queueItem = new AdminReviewQueueItem();
        queueItem.setTaskType("PRE_REGISTRATION");
        queueItem.setPreRegistration(pre);
        queueItem.setClaim(null);
        queueItem.setMember(null);
        queueItem.setAssignedTo(null);
        queueItem.setPriorityScore(computePriorityScore(fraudScore, riskCoefficient));
        adminReviewQueueItemRepository.save(queueItem);

        return PreRegistrationResponseDTO.builder()
            .success(true)
            .preRegistrationId(pre.getId())
            .status(PreRegistrationStatus.PENDING_REVIEW)
            .message("Application submitted. Admin will review within 2-48h.")
            .calculatedPrice(calculatedPrice)
            .riskCoefficient(riskCoefficient)
            .exclusionsNote(hasChronicIllness ? "This chronic illness is not covered, but other conditions are." : null)
            .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PreRegistrationSummaryDTO> getAllPreRegistrations() {
        return preRegistrationRepository.findAll().stream()
            .map(this::toSummary)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PreRegistration getPreRegistrationById(Long id) {
        return preRegistrationRepository.findById(id)
            .orElseThrow(() -> new PreRegistrationException("Pre-registration not found: " + id));
    }

    @Override
    @Transactional
    public PreRegistrationSummaryDTO updatePreRegistration(Long id, PreRegistrationRequestDTO requestDTO) {
        PreRegistration pre = getPreRegistrationById(id);
        if (pre.getStatus() == PreRegistrationStatus.ACTIVATED) {
            throw new PreRegistrationException("Cannot update: pre-registration already activated.");
        }
        String declaration = buildMedicalDeclaration(requestDTO);
        boolean hasChronicIllness = hasChronicIllness(declaration);
        List<MedicalHistory> medicalList = medicalHistoryRepository.findByPreRegistration_Id(pre.getId());
        if (!medicalList.isEmpty()) {
            MedicalHistory m = medicalList.get(0);
            m.setExcludedConditionDetails(declaration);
            medicalHistoryRepository.save(m);
        } else {
            MedicalHistory medical = new MedicalHistory();
            medical.setPreRegistration(pre);
            medical.setMember(null);
            medical.setExcludedConditionDetails(declaration);
            medicalHistoryRepository.save(medical);
        }
        float riskCoefficient = calculateRiskCoefficient(requestDTO, declaration);
        float calculatedPrice = Math.round(BASE_MONTHLY_PRICE * riskCoefficient * 100f) / 100f;
        calculatedPrice = Math.min(MAX_MONTHLY_PRICE, calculatedPrice);
        List<RiskAssessment> riskList = riskAssessmentRepository.findByPreRegistration_Id(pre.getId());
        if (!riskList.isEmpty()) {
            RiskAssessment r = riskList.get(0);
            r.setRiskCoefficient(riskCoefficient);
            r.setCalculatedPrice(calculatedPrice);
            r.setExclusions(hasChronicIllness ? "This chronic illness is not covered, but other conditions are." : null);
            riskAssessmentRepository.save(r);
        } else {
            RiskAssessment risk = new RiskAssessment();
            risk.setPreRegistration(pre);
            risk.setRiskCoefficient(riskCoefficient);
            risk.setCalculatedPrice(calculatedPrice);
            risk.setExclusions(hasChronicIllness ? "This chronic illness is not covered, but other conditions are." : null);
            riskAssessmentRepository.save(risk);
        }
        return toSummary(preRegistrationRepository.save(pre));
    }

    @Override
    @Transactional
    public PreRegistration updatePreRegistrationStatus(Long id, PreRegistrationStatus status) {
        PreRegistration pre = getPreRegistrationById(id);
        if (status == null ||
            (status != PreRegistrationStatus.APPROVED &&
                status != PreRegistrationStatus.REJECTED &&
                status != PreRegistrationStatus.PENDING_REVIEW)) {
            throw new PreRegistrationException("Invalid status. Use APPROVED, REJECTED, or PENDING_REVIEW.");
        }
        pre.setStatus(status);
        return preRegistrationRepository.save(pre);
    }

    @Override
    @Transactional
    public PreRegistration confirmPayment(Long id, Double paymentAmount) {
        PreRegistration pre = getPreRegistrationById(id);
        if (pre.getStatus() != PreRegistrationStatus.APPROVED) {
            throw new PreRegistrationException("Pre-registration must be approved before payment. Current status: " + pre.getStatus());
        }

        Optional<RiskAssessment> latest = riskAssessmentRepository.findByPreRegistration_Id(pre.getId()).stream().findFirst();
        float price = latest.map(RiskAssessment::getCalculatedPrice).orElse(BASE_MONTHLY_PRICE);
        if (paymentAmount != null && Math.abs(paymentAmount - price) > 0.01) {
            throw new PreRegistrationException("Payment amount does not match calculated price: " + price);
        }

        Member member = new Member();
        member.setCinNumber(pre.getCinNumber());
        member.setPersonalizedMonthlyPrice(price);
        member.setAdherenceScore(null);
        member.setCurrentGroup(null);
        member.setPreRegistration(pre);
        member = memberRepository.save(member);

        pre.setStatus(PreRegistrationStatus.ACTIVATED);
        preRegistrationRepository.save(pre);

        return pre;
    }

    @Override
    @Transactional
    public PreRegistration confirmPaymentByPackage(Long id, PackageType packageType) {
        PreRegistration pre = getPreRegistrationById(id);
        if (pre.getStatus() != PreRegistrationStatus.APPROVED) {
            throw new PreRegistrationException("Pre-registration must be approved before payment. Current status: " + pre.getStatus());
        }

        Optional<RiskAssessment> latest = riskAssessmentRepository.findByPreRegistration_Id(pre.getId()).stream().findFirst();
        float basePrice = latest.map(RiskAssessment::getCalculatedPrice).orElse(BASE_MONTHLY_PRICE);
        float finalPrice = applyPackageMultiplier(basePrice, packageType);

        Member member = new Member();
        member.setCinNumber(pre.getCinNumber());
        member.setPersonalizedMonthlyPrice(finalPrice);
        member.setAdherenceScore(null);
        member.setCurrentGroup(null);
        member.setPreRegistration(pre);
        member = memberRepository.save(member);

        pre.setStatus(PreRegistrationStatus.ACTIVATED);
        preRegistrationRepository.save(pre);

        return pre;
    }

    @Override
    @Transactional
    public void deletePreRegistration(Long id) {
        PreRegistration pre = getPreRegistrationById(id);
        if (pre.getStatus() == PreRegistrationStatus.ACTIVATED) {
            throw new PreRegistrationException("Cannot delete: account already activated.");
        }
        adminReviewQueueItemRepository.findByPreRegistration(pre).forEach(adminReviewQueueItemRepository::delete);
        riskAssessmentRepository.findByPreRegistration_Id(pre.getId()).forEach(riskAssessmentRepository::delete);
        medicalHistoryRepository.findByPreRegistration_Id(pre.getId()).forEach(medicalHistoryRepository::delete);
        documentUploadRepository.findByPreRegistration_Id(pre.getId()).forEach(documentUploadRepository::delete);
        preRegistrationRepository.delete(pre);
    }

    @Override
    @Transactional
    public void uploadMedicalHistoryDocument(Long medicalHistoryId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new PreRegistrationException("Uploaded file is empty.");
        }
        MedicalHistory medicalHistory = medicalHistoryRepository.findById(medicalHistoryId)
            .orElseThrow(() -> new PreRegistrationException("Medical history not found: " + medicalHistoryId));
        PreRegistration pre = medicalHistory.getPreRegistration();

        // Save file to disk under uploads/pre-registration/{id}/
        Path target;
        try {
            Path baseDir = Path.of("uploads", "pre-registration", String.valueOf(pre.getId()));
            Files.createDirectories(baseDir);
            target = baseDir.resolve(file.getOriginalFilename() != null ? file.getOriginalFilename() : "medical-history-" + System.currentTimeMillis());
            Files.write(target, file.getBytes());
        } catch (IOException e) {
            throw new PreRegistrationException("Failed to store medical history document.", e);
        }

        // Create a DocumentUpload entry with a neutral fraud score for now
        DocumentUpload upload = new DocumentUpload();
        upload.setPreRegistration(pre);
        upload.setMember(medicalHistory.getMember());
        upload.setClaim(null);
        upload.setFraudDetectionScore(0.0f);
        upload.setFilePath(target.toString());
        documentUploadRepository.save(upload);
    }

    private PreRegistrationSummaryDTO toSummary(PreRegistration pre) {
        return PreRegistrationSummaryDTO.builder()
            .id(pre.getId())
            .cinNumber(pre.getCinNumber())
            .status(pre.getStatus())
            .fraudScore(pre.getFraudScore())
            .createdAt(pre.getCreatedAt())
            .build();
    }

    private String normalizeCin(String cin) {
        return cin == null ? null : cin.trim().toUpperCase();
    }

    private String buildMedicalDeclaration(PreRegistrationRequestDTO dto) {
        StringBuilder sb = new StringBuilder();
        if (dto.getMedicalDeclarationText() != null) sb.append(dto.getMedicalDeclarationText()).append(" ");
        if (dto.getCurrentConditions() != null) sb.append(dto.getCurrentConditions()).append(" ");
        if (dto.getFamilyHistory() != null) sb.append(dto.getFamilyHistory()).append(" ");
        if (dto.getOngoingTreatments() != null) sb.append(dto.getOngoingTreatments()).append(" ");
        if (dto.getConsultationFrequency() != null) sb.append(dto.getConsultationFrequency()).append(" ");
        return sb.toString().trim().toLowerCase();
    }

    /** Returns first excluded condition name found in declaration, or null. */
    private String findExcludedConditionInDeclaration(String declaration, List<ExcludedCondition> excluded) {
        if (declaration == null || declaration.isEmpty()) return null;
        String lower = declaration.toLowerCase();
        for (ExcludedCondition ec : excluded) {
            if (ec.getConditionName() != null && lower.contains(ec.getConditionName().toLowerCase())) {
                return ec.getConditionName();
            }
        }
        return null;
    }

    /**
     * ML-like risk coefficient: base 1.0 + age (>50) + seasonal illness + family history + profession + financial +
     * declared diseases & treatments.
     */
    private float calculateRiskCoefficient(PreRegistrationRequestDTO dto, String declaration) {
        float coef = 1.0f;

        if (dto.getAge() != null && dto.getAge() > 50) {
            coef += 0.15f;
        }

        if (dto.getSeasonalIllnessMonthsPerYear() != null && dto.getSeasonalIllnessMonthsPerYear() >= 3) {
            coef += 0.70f; // flu 3 months/year = +70%
        } else if (dto.getSeasonalIllnessMonthsPerYear() != null && dto.getSeasonalIllnessMonthsPerYear() > 0) {
            coef += 0.20f;
        }

        if (dto.getFamilyHistory() != null && !dto.getFamilyHistory().isBlank()) {
            String fh = dto.getFamilyHistory().toLowerCase();
            if (fh.contains("allerg") || fh.contains("asthma")) coef += 0.20f; // minor family history
        }

        if (dto.getProfession() != null) {
            String p = dto.getProfession().toLowerCase();
            if (p.contains("construction") || p.contains("mining") || p.contains("heavy")) coef += 0.30f;
        }

        if (dto.getFinancialStability() != null) {
            switch (dto.getFinancialStability()) {
                case INSTABLE -> coef += 0.10f;
                case STABLE, MODERE -> {
                    // no extra risk
                }
            }
        }

        // Additional signal from full medical text
        String allText = (declaration == null ? "" : declaration.toLowerCase());

        // Chronic but not auto-reject
        if (allText.contains("hypertension") || allText.contains("high blood pressure")) {
            coef += 0.25f;
        }
        if (allText.contains("asthma")) {
            coef += 0.15f;
        }

        // Heavy medications
        if (allText.contains("anticoagulant") || allText.contains("blood thinner")) {
            coef += 0.30f;
        }

        // Strong family history of serious disease
        if (allText.contains("family") && allText.contains("cancer")) {
            coef += 0.30f;
        }

        // Diabetes or other chronic illness → +25% contribution
        if (hasChronicIllness(allText)) {
            coef *= 1.25f;
        }

        return Math.max(1.0f, Math.min(coef, 3.0f));
    }

    /** Simple heuristic fraud score based on declaration consistency & risk signals. */
    private float detectFraudScore(PreRegistrationRequestDTO dto, String declaration) {
        float score = 0.0f;
        String text = declaration == null ? "" : declaration.toLowerCase();

        // Suspicious: declares very serious disease in basic product
        if (text.contains("terminal") || text.contains("stage 4") || text.contains("dialysis")) {
            score += 0.6f;
        }
        if (text.contains("heart attack") || text.contains("stroke")) {
            score += 0.4f;
        }

        // Self-contradiction: "no chronic disease" but chronic meds
        if (text.contains("no chronic") &&
            (text.contains("insulin") || text.contains("chemotherapy") || text.contains("radiotherapy"))) {
            score += 0.5f;
        }

        // Age + heavy profession + empty history → slightly suspicious
        if (dto.getAge() != null && dto.getAge() > 55 &&
            dto.getProfession() != null &&
            dto.getProfession().toLowerCase().contains("construction") &&
            (dto.getMedicalDeclarationText() == null || dto.getMedicalDeclarationText().isBlank())) {
            score += 0.3f;
        }

        // Many blank answers
        int emptyCount = 0;
        if (isBlank(dto.getCurrentConditions())) emptyCount++;
        if (isBlank(dto.getFamilyHistory())) emptyCount++;
        if (isBlank(dto.getOngoingTreatments())) emptyCount++;
        if (isBlank(dto.getConsultationFrequency())) emptyCount++;
        if (emptyCount >= 3) {
            score += 0.2f;
        }

        return Math.max(0f, Math.min(score, 1f));
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    /** Detects diabetes or other chronic illness mentions in the medical declaration text. */
    private boolean hasChronicIllness(String declaration) {
        if (declaration == null || declaration.isBlank()) {
            return false;
        }
        String lower = declaration.toLowerCase();
        return lower.contains("diabet") || lower.contains("chronic");
    }

    /** Combine fraud score and risk coefficient into an admin priority score (0..1). */
    private float computePriorityScore(float fraudScore, float riskCoefficient) {
        float normRisk = Math.max(0f, Math.min((riskCoefficient - 1f) / 2f, 1f)); // risk 1..3 → 0..1
        float score = 0.7f * fraudScore + 0.3f * normRisk;
        return Math.max(0f, Math.min(score, 1f));
    }

    private float applyPackageMultiplier(float basePrice, PackageType packageType) {
        if (packageType == null) {
            return basePrice;
        }
        float raw = switch (packageType) {
            case CONFORT -> basePrice * 1.3f;
            case PREMIUM -> basePrice * 1.6f;
            case BASIC -> basePrice;
        };
        return Math.min(MAX_MONTHLY_PRICE, raw);
    }
}
