package pi.db.piversionbd.dto.pre;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "Patient correspondant à une recherche texte médicale")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PatientMedicalMatchDTO {
    /** Pre-registration dossier id used to open the full case. */
    private Long preRegistrationId;
    /** MedicalHistory row where the keyword hit was found. */
    private Long medicalHistoryId;
    /** Linked member id (null if applicant not activated yet). */
    private Long memberId;
    private String cinNumber;
    private String status;
    private String memberEmail;
    /** Indicates which field matched: qaPayload or excludedConditionDetails. */
    private String matchedIn;
    /** Short excerpt to help admin quickly inspect the hit context. */
    private String matchedSnippet;
}
