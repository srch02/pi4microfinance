package pi.db.piversionbd.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pi.db.piversionbd.controller.PreRegistrationRequestDTO;
import pi.db.piversionbd.controller.PreRegistrationResponseDTO;
import pi.db.piversionbd.controller.PreRegistrationSummaryDTO;
import pi.db.piversionbd.controller.PreRegistrationException;
import pi.db.piversionbd.entities.admin.AdminReviewQueueItem;
import pi.db.piversionbd.entities.groups.Member;
import pi.db.piversionbd.entities.pre.*;
import pi.db.piversionbd.repository.*;

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

    private static final String STATUS_PENDING_REVIEW = "PENDING_REVIEW";
    private static final String STATUS_APPROVED = "APPROVED";
    private static final String STATUS_REJECTED = "REJECTED";
    private static final String STATUS_ACTIVATED = "ACTIVATED";
    private static final float BASE_MONTHLY_PRICE = 50.0f;

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

        // Step 4: Risk coefficient and personalized price
        float riskCoefficient = calculateRiskCoefficient(requestDTO);
        float calculatedPrice = Math.round(BASE_MONTHLY_PRICE * riskCoefficient * 100f) / 100f;

        // Persist: PreRegistration, MedicalHistory, RiskAssessment, AdminReviewQueueItem
        PreRegistration pre = new PreRegistration();
        pre.setCinNumber(cin);
        pre.setStatus(STATUS_PENDING_REVIEW);
        pre.setFraudScore(null);
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
        risk.setExclusions(null);
        riskAssessmentRepository.save(risk);

        AdminReviewQueueItem queueItem = new AdminReviewQueueItem();
        queueItem.setTaskType("PRE_REGISTRATION");
        queueItem.setPreRegistration(pre);
        queueItem.setClaim(null);
        queueItem.setMember(null);
        queueItem.setAssignedTo(null);
        queueItem.setPriorityScore(0.5f);
        adminReviewQueueItemRepository.save(queueItem);

        return PreRegistrationResponseDTO.builder()
            .success(true)
            .preRegistrationId(pre.getId())
            .status(STATUS_PENDING_REVIEW)
            .message("Application submitted. Admin will review within 2-48h.")
            .calculatedPrice(calculatedPrice)
            .riskCoefficient(riskCoefficient)
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
        if (STATUS_ACTIVATED.equals(pre.getStatus())) {
            throw new PreRegistrationException("Cannot update: pre-registration already activated.");
        }
        String declaration = buildMedicalDeclaration(requestDTO);
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
        float riskCoefficient = calculateRiskCoefficient(requestDTO);
        float calculatedPrice = Math.round(BASE_MONTHLY_PRICE * riskCoefficient * 100f) / 100f;
        List<RiskAssessment> riskList = riskAssessmentRepository.findByPreRegistration_Id(pre.getId());
        if (!riskList.isEmpty()) {
            RiskAssessment r = riskList.get(0);
            r.setRiskCoefficient(riskCoefficient);
            r.setCalculatedPrice(calculatedPrice);
            riskAssessmentRepository.save(r);
        } else {
            RiskAssessment risk = new RiskAssessment();
            risk.setPreRegistration(pre);
            risk.setRiskCoefficient(riskCoefficient);
            risk.setCalculatedPrice(calculatedPrice);
            risk.setExclusions(null);
            riskAssessmentRepository.save(risk);
        }
        return toSummary(preRegistrationRepository.save(pre));
    }

    @Override
    @Transactional
    public PreRegistration updatePreRegistrationStatus(Long id, String status) {
        PreRegistration pre = getPreRegistrationById(id);
        if (status == null || (!STATUS_APPROVED.equals(status) && !STATUS_REJECTED.equals(status) && !STATUS_PENDING_REVIEW.equals(status))) {
            throw new PreRegistrationException("Invalid status. Use APPROVED, REJECTED, or PENDING_REVIEW.");
        }
        pre.setStatus(status);
        return preRegistrationRepository.save(pre);
    }

    @Override
    @Transactional
    public PreRegistration confirmPayment(Long id, Double paymentAmount) {
        PreRegistration pre = getPreRegistrationById(id);
        if (!STATUS_APPROVED.equals(pre.getStatus())) {
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

        pre.setStatus(STATUS_ACTIVATED);
        preRegistrationRepository.save(pre);

        return pre;
    }

    @Override
    @Transactional
    public void deletePreRegistration(Long id) {
        PreRegistration pre = getPreRegistrationById(id);
        if (STATUS_ACTIVATED.equals(pre.getStatus())) {
            throw new PreRegistrationException("Cannot delete: account already activated.");
        }
        adminReviewQueueItemRepository.findByPreRegistration(pre).forEach(adminReviewQueueItemRepository::delete);
        riskAssessmentRepository.findByPreRegistration_Id(pre.getId()).forEach(riskAssessmentRepository::delete);
        medicalHistoryRepository.findByPreRegistration_Id(pre.getId()).forEach(medicalHistoryRepository::delete);
        documentUploadRepository.findByPreRegistration_Id(pre.getId()).forEach(documentUploadRepository::delete);
        preRegistrationRepository.delete(pre);
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
     * ML-like risk coefficient: base 1.0 + age (>50) + seasonal illness + family history + profession + financial.
     */
    private float calculateRiskCoefficient(PreRegistrationRequestDTO dto) {
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
            String fs = dto.getFinancialStability().toLowerCase();
            if (fs.contains("unstable") || fs.contains("low")) coef += 0.10f;
        }

        return Math.max(1.0f, Math.min(coef, 3.0f));
    }
}
