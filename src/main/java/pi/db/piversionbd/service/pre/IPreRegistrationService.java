package pi.db.piversionbd.service.pre;

import org.springframework.web.multipart.MultipartFile;
import pi.db.piversionbd.dto.pre.CinOcrResponseDTO;
import pi.db.piversionbd.dto.pre.PreRegistrationRequestDTO;
import pi.db.piversionbd.dto.pre.PreRegistrationResponseDTO;
import pi.db.piversionbd.dto.pre.PreRegistrationSummaryDTO;
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
    pi.db.piversionbd.dto.pre.MedicalHistoryAssessmentDTO submitMedicalHistoryQa(Long preRegistrationId, Map<String, String> answers);

    /** Does not collect payment; package choice and payment occur in groups / memberships. */
    PreRegistration confirmPaymentByPackage(Long id, PackageType packageType);
}

