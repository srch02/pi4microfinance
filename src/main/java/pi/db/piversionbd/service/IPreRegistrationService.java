package pi.db.piversionbd.service;

import org.springframework.web.multipart.MultipartFile;
import pi.db.piversionbd.controller.PreRegistrationRequestDTO;
import pi.db.piversionbd.controller.PreRegistrationResponseDTO;
import pi.db.piversionbd.controller.PreRegistrationSummaryDTO;
import pi.db.piversionbd.entities.groups.PackageType;
import pi.db.piversionbd.entities.pre.PreRegistration;
import pi.db.piversionbd.entities.pre.PreRegistrationStatus;

import java.util.List;

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

    // New: confirm payment by package type (BASIC, CONFORT, PREMIUM) using calculated price
    PreRegistration confirmPaymentByPackage(Long id, PackageType packageType);
}
