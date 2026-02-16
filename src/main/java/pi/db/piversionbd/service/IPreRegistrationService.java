package pi.db.piversionbd.service;

import pi.db.piversionbd.controller.PreRegistrationRequestDTO;
import pi.db.piversionbd.controller.PreRegistrationResponseDTO;
import pi.db.piversionbd.controller.PreRegistrationSummaryDTO;
import pi.db.piversionbd.entities.pre.PreRegistration;

import java.util.List;

public interface IPreRegistrationService {
    List<PreRegistrationSummaryDTO> getAllPreRegistrations();
    PreRegistrationResponseDTO submitPreRegistration(PreRegistrationRequestDTO requestDTO);
    PreRegistration getPreRegistrationById(Long id);
    PreRegistrationSummaryDTO updatePreRegistration(Long id, PreRegistrationRequestDTO requestDTO);
    PreRegistration updatePreRegistrationStatus(Long id, String status);
    PreRegistration confirmPayment(Long id, Double paymentAmount);
    void deletePreRegistration(Long id);
}
