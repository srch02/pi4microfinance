package pi.db.piversionbd.service;

import org.springframework.web.multipart.MultipartFile;
import pi.db.piversionbd.controller.CinOcrResponseDTO;
import pi.db.piversionbd.controller.PreRegistrationRequestDTO;
import pi.db.piversionbd.controller.PreRegistrationResponseDTO;
import pi.db.piversionbd.controller.PreRegistrationSummaryDTO;
import pi.db.piversionbd.entities.groups.PackageType;
import pi.db.piversionbd.entities.pre.PreRegistration;
import pi.db.piversionbd.entities.pre.PreRegistrationStatus;

import java.util.List;
import java.util.Map;

public interface IPreRegistrationService {
    List<PreRegistrationSummaryDTO> getAllPreRegistrations();
    PreRegistrationResponseDTO submitPreRegistration(PreRegistrationRequestDTO requestDTO);
    PreRegistration getPreRegistrationById(Long id);
    PreRegistrationSummaryDTO updatePreRegistration(Long id, PreRegistrationRequestDTO requestDTO);
    PreRegistration updatePreRegistrationStatus(Long id, PreRegistrationStatus status);
    PreRegistration confirmPayment(Long id, Double paymentAmount);
    void deletePreRegistration(Long id);

    // New: upload medical history document and create a DocumentUpload entry (linked via MedicalHistory)
    void uploadMedicalHistoryDocument(Long medicalHistoryId, MultipartFile file);

    // New: extract CIN number from an uploaded document (OCR / best-effort)
    CinOcrResponseDTO extractCinFromDocument(MultipartFile file);

    // New: upload CIN document and attach it to a pre-registration (with fraud scan)
    void uploadCinDocument(Long preRegistrationId, MultipartFile file);

    // New: submit medical history Q&A, run IA/ML-like assessment, and alert admin if suspicious
    pi.db.piversionbd.controller.MedicalHistoryAssessmentDTO submitMedicalHistoryQa(Long preRegistrationId, Map<String, String> answers);

    // New: confirm payment by package type (BASIC, CONFORT, PREMIUM) using calculated price
    PreRegistration confirmPaymentByPackage(Long id, PackageType packageType);
}
