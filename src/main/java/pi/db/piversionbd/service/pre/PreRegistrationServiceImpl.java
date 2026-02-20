package pi.db.piversionbd.service.pre;
import pi.db.piversionbd.entities.groups.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import pi.db.piversionbd.dto.pre.CinOcrResponseDTO;
import pi.db.piversionbd.dto.pre.MedicalHistoryAssessmentDTO;
import pi.db.piversionbd.controller.pre.PreRegistrationException;
import pi.db.piversionbd.dto.pre.PreRegistrationRequestDTO;
import pi.db.piversionbd.dto.pre.PreRegistrationResponseDTO;
import pi.db.piversionbd.dto.pre.PreRegistrationSummaryDTO;
import pi.db.piversionbd.entities.admin.SystemAlert;
import pi.db.piversionbd.entities.groups.Member;
import pi.db.piversionbd.entities.groups.PackageType;
import pi.db.piversionbd.entities.pre.*;
import pi.db.piversionbd.entities.admin.AdminReviewQueueItem;
import pi.db.piversionbd.repository.admin.AdminReviewQueueItemRepository;
import pi.db.piversionbd.repository.admin.SystemAlertRepository;
import pi.db.piversionbd.repository.pre.BlacklistRepository;
import pi.db.piversionbd.repository.pre.DocumentUploadRepository;
import pi.db.piversionbd.repository.pre.ExcludedConditionRepository;
import pi.db.piversionbd.repository.pre.MedicalHistoryRepository;
import pi.db.piversionbd.repository.pre.RiskAssessmentRepository;
import pi.db.piversionbd.repository.groups.MemberRepository;


import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Implements Module 5: Pre-Registration & Insurance flow.
 * Steps: Identity (CIN duplicate/blacklist) → Medical declaration → Excluded conditions → Risk & price → Admin queue.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class PreRegistrationServiceImpl implements IPreRegistrationService {

    private static final float BASE_MONTHLY_PRICE = 25.0f;
    private static final float MAX_MONTHLY_PRICE = 70.0f;
    private static final Pattern CIN_PATTERN = Pattern.compile("\\b\\d{1,8}\\b");

    private final pi.db.piversionbd.repository.pre.PreRegistrationRepository preRegistrationRepository;
    private final BlacklistRepository blacklistRepository;
    private final ExcludedConditionRepository excludedConditionRepository;
    private final MedicalHistoryRepository medicalHistoryRepository;
    private final RiskAssessmentRepository riskAssessmentRepository;
    private final DocumentUploadRepository documentUploadRepository;
    private final AdminReviewQueueItemRepository adminReviewQueueItemRepository;
    private final SystemAlertRepository systemAlertRepository;
    private final MemberRepository memberRepository;
    private final MedicalHistoryAiService medicalHistoryAiService;

    @Override
    @Transactional
    public PreRegistrationResponseDTO submitPreRegistration(PreRegistrationRequestDTO requestDTO) {
        String cin = normalizeCin(requestDTO.getCinNumber());
        if (cin == null || cin.isBlank()) {
            throw new PreRegistrationException("CIN number is required.");
        }

        // Step 1: Duplicate CIN (already in pre-registration or already a member)
        if (preRegistrationRepository.existsByCinNumber(cin)) {
            throw new PreRegistrationException("You are already registered.");
        }
        if (memberRepository.existsByCinNumber(cin)) {
            throw new PreRegistrationException("This CIN already has a member account.");
        }

        // Step 2: Blacklist
        if (blacklistRepository.existsByCinNumber(cin)) {
            throw new PreRegistrationException("Registration not allowed: you are on the blacklist.");
        }

        // Build full medical declaration for analysis
        String declaration = buildMedicalDeclaration(requestDTO);

        // Step 3: Excluded conditions
        List<ExcludedCondition> excluded = excludedConditionRepository.findByAutoRejectTrue();
        String excludedMatch = findExcludedConditionInDeclaration(declaration, excluded);
        if (excludedMatch != null && !isNonRejectChronicCondition(excludedMatch)) {
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
        medical.setQaPayload(null);
        medical.setQualityScore(null);
        medical.setCrude(null);
        medical.setMlFraudScore(null);
        medical.setMlFraudReason(null);
        medical.setAssessedAt(null);
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
        queueItem.setAlert(fraudScore >= 0.6f);
        queueItem.setAlertReason(fraudScore >= 0.6f ? "Suspicious declaration detected by fraud heuristics." : null);
        queueItem.setCreatedAt(LocalDateTime.now());
        adminReviewQueueItemRepository.save(queueItem);

        float priceBasic = calculatedPrice;
        float priceConfort = Math.min(MAX_MONTHLY_PRICE, calculatedPrice * 1.3f);
        float pricePremium = Math.min(MAX_MONTHLY_PRICE, calculatedPrice * 1.6f);
        return PreRegistrationResponseDTO.builder()
            .success(true)
            .preRegistrationId(pre.getId())
            .status(PreRegistrationStatus.PENDING_REVIEW)
            .message("Application submitted. Admin will review within 2-48h.")
            .calculatedPrice(calculatedPrice)
            .priceBasic(priceBasic)
            .priceConfort(priceConfort)
            .pricePremium(pricePremium)
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
        float basePrice = latest.map(RiskAssessment::getCalculatedPrice).orElse(BASE_MONTHLY_PRICE);
        if (paymentAmount != null && Math.abs(paymentAmount - basePrice) > 0.01) {
            throw new PreRegistrationException("Payment amount does not match calculated price: " + basePrice);
        }

        Member member = new Member();
        member.setCinNumber(pre.getCinNumber());
        member.setPersonalizedMonthlyPrice(basePrice);
        applyPackagePricesToMember(member, basePrice);
        member.setAdherenceScore(null);
        member.setCurrentGroup(null);
        member.setPreRegistration(pre);
        member = memberRepository.save(member);
        enqueueMemberActivatedSystemAlert(pre, member);

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
        applyPackagePricesToMember(member, basePrice);
        member.setAdherenceScore(null);
        member.setCurrentGroup(null);
        member.setPreRegistration(pre);
        member = memberRepository.save(member);
        enqueueMemberActivatedSystemAlert(pre, member);

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

        // ADD THIS - nullify member's preRegistration reference first
        Member member = memberRepository.findByPreRegistration_Id(id);
        if (member != null) {
            member.setPreRegistration(null);
            memberRepository.save(member);
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

        // Best-effort text extraction for consistency check (works for txt; images require optional OCR)
        String extractedText = extractTextBestEffort(file, target);
        String declaration = medicalHistory.getExcludedConditionDetails();
        float docFraudScore = computeDocumentMismatchScore(declaration, extractedText);
        String analysisSummary = docFraudScore >= 0.7f
            ? "Possible mismatch between declared history and uploaded document."
            : "No major mismatch detected (best-effort).";

        // Create a DocumentUpload entry with a neutral fraud score for now
        DocumentUpload upload = new DocumentUpload();
        upload.setPreRegistration(pre);
        upload.setMember(medicalHistory.getMember());
        upload.setClaim(null);
        upload.setFraudDetectionScore(docFraudScore);
        upload.setDocumentType("MEDICAL_HISTORY");
        upload.setFilePath(target.toString());
        upload.setUploadedAt(LocalDateTime.now());
        upload.setExtractedText(truncate(extractedText, 2000));
        upload.setAnalysisSummary(analysisSummary);
        documentUploadRepository.save(upload);

        if (docFraudScore >= 0.7f) {
            enqueueAdminAlert(pre, "Medical document mismatch detected.", 1.0f);
        }
    }

    @Override
    @Transactional
    public MedicalHistoryAssessmentDTO submitMedicalHistoryQa(Long preRegistrationId, java.util.Map<String, String> answers) {
        if (preRegistrationId == null) {
            throw new PreRegistrationException("preRegistrationId is required.");
        }
        PreRegistration pre = getPreRegistrationById(preRegistrationId);

        List<MedicalHistory> list = medicalHistoryRepository.findByPreRegistration_Id(pre.getId());
        MedicalHistory mh = list.isEmpty() ? new MedicalHistory() : list.get(0);
        if (mh.getId() == null) {
            mh.setPreRegistration(pre);
            mh.setMember(null);
            mh.setExcludedConditionDetails(null);
        }

        MedicalHistoryAssessmentDTO assessment = medicalHistoryAiService.assess(answers);
        String payload = toJsonBestEffort(answers);

        mh.setQaPayload(payload);
        mh.setQualityScore(assessment.getQualityScore());
        mh.setCrude(assessment.getCrude());
        mh.setMlFraudScore(assessment.getFraudScore());
        mh.setMlFraudReason(assessment.getFraudReason());
        mh.setAssessedAt(LocalDateTime.now());
        medicalHistoryRepository.save(mh);

        if (assessment.getFraudScore() != null && assessment.getFraudScore() >= 0.7f) {
            enqueueAdminAlert(pre, "ML fraud suspicion on medical Q&A: " + assessment.getFraudReason(), 1.0f);
        }

        return assessment;
    }

    @Override
    @Transactional(readOnly = true)
    public CinOcrResponseDTO extractCinFromDocument(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new PreRegistrationException("Uploaded file is empty.");
        }
        String rawText = extractTextBestEffort(file, null);
        String cin = extractCinFromText(rawText);
        return CinOcrResponseDTO.builder()
            .cinNumber(cin)
            .rawText(truncate(rawText, 500))
            .build();
    }

    @Override
    @Transactional
    public void uploadCinDocument(Long preRegistrationId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new PreRegistrationException("Uploaded file is empty.");
        }
        PreRegistration pre = getPreRegistrationById(preRegistrationId);

        Path target;
        try {
            Path baseDir = Path.of("uploads", "pre-registration", String.valueOf(pre.getId()), "cin");
            Files.createDirectories(baseDir);
            target = baseDir.resolve(file.getOriginalFilename() != null ? file.getOriginalFilename() : "cin-" + System.currentTimeMillis());
            Files.write(target, file.getBytes());
        } catch (IOException e) {
            throw new PreRegistrationException("Failed to store CIN document.", e);
        }

        String rawText = extractTextBestEffort(file, target);
        String extractedCin = extractCinFromText(rawText);

        float fraud = 0.0f;
        String summary = "CIN upload stored.";
        if (extractedCin == null) {
            fraud = 0.2f;
            summary = "OCR could not extract CIN; manual verification needed.";
        } else if (!normalizeCin(extractedCin).equals(normalizeCin(pre.getCinNumber()))) {
            fraud = 0.9f;
            summary = "Extracted CIN does not match declared CIN.";
            enqueueAdminAlert(pre, "CIN mismatch: extracted=" + extractedCin + ", declared=" + pre.getCinNumber(), 1.0f);
        }

        DocumentUpload upload = new DocumentUpload();
        upload.setPreRegistration(pre);
        upload.setMember(pre.getMember());
        upload.setClaim(null);
        upload.setFraudDetectionScore(fraud);
        upload.setDocumentType("CIN");
        upload.setFilePath(target.toString());
        upload.setUploadedAt(LocalDateTime.now());
        upload.setExtractedCin(extractedCin);
        upload.setExtractedText(truncate(rawText, 2000));
        upload.setAnalysisSummary(summary);
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

    private String findExcludedConditionInDeclaration(String declaration, List<ExcludedCondition> excluded) {
        if (declaration == null || declaration.isEmpty()) return null;
        String lower = normalizeForMatching(declaration);
        for (ExcludedCondition ec : excluded) {
            if (ec.getConditionName() != null && lower.contains(normalizeForMatching(ec.getConditionName()))) {
                return ec.getConditionName();
            }
        }
        return null;
    }

    private boolean isNonRejectChronicCondition(String excludedConditionName) {
        String n = normalizeForMatching(excludedConditionName);
        return n.contains("diabet");
    }

    private float calculateRiskCoefficient(PreRegistrationRequestDTO dto, String declaration) {
        float coef = 1.0f;

        if (dto.getAge() != null && dto.getAge() > 50) {
            coef += 0.15f;
        }

        if (dto.getSeasonalIllnessMonthsPerYear() != null && dto.getSeasonalIllnessMonthsPerYear() >= 3) {
            coef += 0.70f;
        } else if (dto.getSeasonalIllnessMonthsPerYear() != null && dto.getSeasonalIllnessMonthsPerYear() > 0) {
            coef += 0.20f;
        }

        if (dto.getFamilyHistory() != null && !dto.getFamilyHistory().isBlank()) {
            String fh = dto.getFamilyHistory().toLowerCase();
            if (fh.contains("allerg") || fh.contains("asthma")) coef += 0.20f;
        }

        if (dto.getProfession() != null) {
            String p = dto.getProfession().toLowerCase();
            if (p.contains("construction") || p.contains("mining") || p.contains("heavy")) coef += 0.30f;
        }

        if (dto.getFinancialStability() != null) {
            switch (dto.getFinancialStability()) {
                case INSTABLE -> coef += 0.10f;
                case STABLE, MODERE -> {
                }
            }
        }

        String allText = (declaration == null ? "" : declaration.toLowerCase());

        if (allText.contains("hypertension") || allText.contains("high blood pressure")) {
            coef += 0.25f;
        }
        if (allText.contains("asthma")) {
            coef += 0.15f;
        }

        if (allText.contains("anticoagulant") || allText.contains("blood thinner")) {
            coef += 0.30f;
        }

        if (allText.contains("family") && allText.contains("cancer")) {
            coef += 0.30f;
        }

        if (hasChronicIllness(allText)) {
            coef *= 1.25f;
        }

        return Math.max(1.0f, Math.min(coef, 3.0f));
    }

    private float detectFraudScore(PreRegistrationRequestDTO dto, String declaration) {
        float score = 0.0f;
        String text = declaration == null ? "" : declaration.toLowerCase();

        if (text.contains("terminal") || text.contains("stage 4") || text.contains("dialysis")) {
            score += 0.6f;
        }
        if (text.contains("heart attack") || text.contains("stroke")) {
            score += 0.4f;
        }

        if (text.contains("no chronic") &&
            (text.contains("insulin") || text.contains("chemotherapy") || text.contains("radiotherapy"))) {
            score += 0.5f;
        }

        if (dto.getAge() != null && dto.getAge() > 55 &&
            dto.getProfession() != null &&
            dto.getProfession().toLowerCase().contains("construction") &&
            (dto.getMedicalDeclarationText() == null || dto.getMedicalDeclarationText().isBlank())) {
            score += 0.3f;
        }

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

    private boolean hasChronicIllness(String declaration) {
        if (declaration == null || declaration.isBlank()) {
            return false;
        }
        String lower = normalizeForMatching(declaration);
        return lower.contains("diabet") ||
            lower.contains("chronique") ||
            lower.contains("chronic");
    }

    private String normalizeForMatching(String input) {
        if (input == null) return "";
        String lower = input.toLowerCase();
        String nfd = Normalizer.normalize(lower, Normalizer.Form.NFD);
        return nfd.replaceAll("\\p{M}", "");
    }

    private void enqueueAdminAlert(PreRegistration pre, String reason, float priorityScore) {
        AdminReviewQueueItem item = new AdminReviewQueueItem();
        item.setTaskType("PRE_REGISTRATION_ALERT");
        item.setPreRegistration(pre);
        item.setClaim(null);
        item.setMember(null);
        item.setAssignedTo(null);
        item.setPriorityScore(priorityScore);
        item.setAlert(true);
        item.setAlertReason(reason);
        item.setCreatedAt(LocalDateTime.now());
        adminReviewQueueItemRepository.save(item);
    }

    /** System alert for admin dashboards when a pre-registration becomes an active member (deduped). */
    private void enqueueMemberActivatedSystemAlert(PreRegistration pre, Member member) {
        if (pre == null || pre.getId() == null) return;
        boolean exists = systemAlertRepository
            .findByAlertTypeAndSourceEntityTypeAndSourceEntityIdAndActive(
                "MEMBER_ACTIVATED", "PRE_REGISTRATION", pre.getId(), true)
            .isPresent();
        if (exists) return;

        SystemAlert alert = new SystemAlert();
        alert.setAlertType("MEMBER_ACTIVATED");
        alert.setSeverity("low");
        alert.setRegion(null);
        alert.setTitle("Member activated");
        alert.setMessage(String.format(
            "Pre-registration id=%d (CIN=%s) has been activated as member id=%s.",
            pre.getId(),
            pre.getCinNumber() != null ? pre.getCinNumber() : "—",
            member != null && member.getId() != null ? member.getId().toString() : "—"
        ));
        alert.setActive(true);
        alert.setSourceEntityType("PRE_REGISTRATION");
        alert.setSourceEntityId(pre.getId());
        systemAlertRepository.save(alert);
    }

    private String extractCinFromText(String text) {
        if (text == null || text.isBlank()) return null;
        Matcher m = CIN_PATTERN.matcher(text);
        if (!m.find()) return null;
        return m.group();
    }

    private String extractTextBestEffort(MultipartFile file, Path storedPathOrNull) {
        try {
            String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase();
            String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();

            if (contentType.contains("text") || name.endsWith(".txt") || name.endsWith(".csv") || name.endsWith(".log")) {
                return new String(file.getBytes(), StandardCharsets.UTF_8);
            }

            if (contentType.startsWith("image/") || name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg")) {
                return tryTesseractOcr(storedPathOrNull);
            }
        } catch (IOException ignored) {
        }
        return null;
    }

    private String tryTesseractOcr(Path imagePathOrNull) {
        if (imagePathOrNull == null) return null;
        try {
            Class<?> tesseractClass = Class.forName("net.sourceforge.tess4j.Tesseract");
            Object tesseract = tesseractClass.getDeclaredConstructor().newInstance();
            var doOcr = tesseractClass.getMethod("doOCR", java.io.File.class);
            Object res = doOcr.invoke(tesseract, imagePathOrNull.toFile());
            return res == null ? null : res.toString();
        } catch (Throwable ignored) {
            return null;
        }
    }

    private float computeDocumentMismatchScore(String declared, String extracted) {
        if (extracted == null || extracted.isBlank()) {
            return 0.0f;
        }
        String d = normalizeForMatching(declared);
        String e = normalizeForMatching(extracted);

        String[] severe = {"cancer", "terminal", "dialysis", "stroke", "heart attack", "infarctus", "avc"};
        for (String s : severe) {
            if (e.contains(normalizeForMatching(s)) && (d == null || !d.contains(normalizeForMatching(s)))) {
                return 0.8f;
            }
        }
        return 0.0f;
    }

    private String truncate(String s, int max) {
        if (s == null) return null;
        if (s.length() <= max) return s;
        return s.substring(0, max);
    }

    private String toJsonBestEffort(java.util.Map<String, String> answers) {
        if (answers == null) return null;
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        boolean first = true;
        for (var e : answers.entrySet()) {
            if (!first) sb.append(",");
            first = false;
            sb.append("\"").append(escapeJson(e.getKey())).append("\":");
            sb.append("\"").append(escapeJson(e.getValue())).append("\"");
        }
        sb.append("}");
        return sb.toString();
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }

    private float computePriorityScore(float fraudScore, float riskCoefficient) {
        float normRisk = Math.max(0f, Math.min((riskCoefficient - 1f) / 2f, 1f));
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

    /** Sets priceBasic, priceConfort, pricePremium on member from preinscription base price so member can choose package when creating membership. */
    private void applyPackagePricesToMember(Member member, float basePrice) {
        member.setPriceBasic(Math.min(MAX_MONTHLY_PRICE, basePrice));
        member.setPriceConfort(Math.min(MAX_MONTHLY_PRICE, basePrice * 1.3f));
        member.setPricePremium(Math.min(MAX_MONTHLY_PRICE, basePrice * 1.6f));
    }
}

