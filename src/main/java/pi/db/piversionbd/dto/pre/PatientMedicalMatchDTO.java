package pi.db.piversionbd.dto.pre;

/**
 * DTO pour retourner des correspondances (match) entre un texte scanné et l'historique médical.
 * Utilisé par {@code MedicalFormScannerService}.
 */
public class PatientMedicalMatchDTO {

    private final Long preRegistrationId;
    private final Long medicalHistoryId;
    private final Long memberId;
    private final String cinNumber;
    private final String status;
    private final String memberEmail;
    private final String matchedIn;
    private final String matchedSnippet;

    private PatientMedicalMatchDTO(Builder builder) {
        this.preRegistrationId = builder.preRegistrationId;
        this.medicalHistoryId = builder.medicalHistoryId;
        this.memberId = builder.memberId;
        this.cinNumber = builder.cinNumber;
        this.status = builder.status;
        this.memberEmail = builder.memberEmail;
        this.matchedIn = builder.matchedIn;
        this.matchedSnippet = builder.matchedSnippet;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Long getPreRegistrationId() {
        return preRegistrationId;
    }

    public Long getMedicalHistoryId() {
        return medicalHistoryId;
    }

    public Long getMemberId() {
        return memberId;
    }

    public String getCinNumber() {
        return cinNumber;
    }

    public String getStatus() {
        return status;
    }

    public String getMemberEmail() {
        return memberEmail;
    }

    public String getMatchedIn() {
        return matchedIn;
    }

    public String getMatchedSnippet() {
        return matchedSnippet;
    }

    public static final class Builder {
        private Long preRegistrationId;
        private Long medicalHistoryId;
        private Long memberId;
        private String cinNumber;
        private String status;
        private String memberEmail;
        private String matchedIn;
        private String matchedSnippet;

        public Builder preRegistrationId(Long preRegistrationId) {
            this.preRegistrationId = preRegistrationId;
            return this;
        }

        public Builder medicalHistoryId(Long medicalHistoryId) {
            this.medicalHistoryId = medicalHistoryId;
            return this;
        }

        public Builder memberId(Long memberId) {
            this.memberId = memberId;
            return this;
        }

        public Builder cinNumber(String cinNumber) {
            this.cinNumber = cinNumber;
            return this;
        }

        public Builder status(String status) {
            this.status = status;
            return this;
        }

        public Builder memberEmail(String memberEmail) {
            this.memberEmail = memberEmail;
            return this;
        }

        public Builder matchedIn(String matchedIn) {
            this.matchedIn = matchedIn;
            return this;
        }

        public Builder matchedSnippet(String matchedSnippet) {
            this.matchedSnippet = matchedSnippet;
            return this;
        }

        public PatientMedicalMatchDTO build() {
            return new PatientMedicalMatchDTO(this);
        }
    }
}
