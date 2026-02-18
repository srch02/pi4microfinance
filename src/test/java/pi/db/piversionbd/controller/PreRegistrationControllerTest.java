package pi.db.piversionbd.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import pi.db.piversionbd.entities.pre.FinancialStabilityLevel;
import pi.db.piversionbd.entities.pre.PreRegistrationStatus;
import pi.db.piversionbd.service.IPreRegistrationService;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests du module Pre-Registration (sans MockMvc).
 * Pour tester l'API REST : démarrer l'app puis http://localhost:8080/swagger-ui.html
 * ou utiliser Postman/curl sur POST /api/pre-registration.
 */
@SpringBootTest
class PreRegistrationControllerTest {

    @Autowired(required = false)
    private IPreRegistrationService preRegistrationService;

    @Test
    @DisplayName("Contexte Spring charge le service Pre-Registration")
    void contextLoads_serviceInjected() {
        assertNotNull(preRegistrationService, "IPreRegistrationService doit être injecté");
    }

    @Test
    @DisplayName("Soumission valide retourne succès et prix calculé")
    void submitPreRegistration_validRequest_returnsSuccessAndPrice() {
        if (preRegistrationService == null) return;

        PreRegistrationRequestDTO request = new PreRegistrationRequestDTO();
        request.setCinNumber("TEST-CIN-" + System.currentTimeMillis());
        request.setMedicalDeclarationText("recurrent flu, seasonal allergies");
        request.setAge(35);
        request.setProfession("office");
        request.setFinancialStability(FinancialStabilityLevel.STABLE);
        request.setSeasonalIllnessMonthsPerYear(1);

        var response = preRegistrationService.submitPreRegistration(request);

        assertTrue(response.isSuccess());
        assertNotNull(response.getPreRegistrationId());
        assertNotNull(response.getCalculatedPrice());
        assertEquals(PreRegistrationStatus.PENDING_REVIEW, response.getStatus());
    }

    @Test
    @DisplayName("CIN vide lève PreRegistrationException")
    void submitPreRegistration_missingCin_throws() {
        if (preRegistrationService == null) return;

        PreRegistrationRequestDTO request = new PreRegistrationRequestDTO();
        request.setCinNumber("");
        request.setMedicalDeclarationText("flu");
        request.setAge(30);

        assertThrows(PreRegistrationException.class, () ->
            preRegistrationService.submitPreRegistration(request));
    }

    @Test
    @DisplayName("getById avec id inexistant lève PreRegistrationException")
    void getById_notFound_throws() {
        if (preRegistrationService == null) return;

        assertThrows(PreRegistrationException.class, () ->
            preRegistrationService.getPreRegistrationById(999999L));
    }
}
