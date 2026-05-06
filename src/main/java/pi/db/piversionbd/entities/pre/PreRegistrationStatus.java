package pi.db.piversionbd.entities.pre;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import pi.db.piversionbd.entities.groups.Member;
import pi.db.piversionbd.entities.score.Claim;

import java.time.LocalDateTime;

@Schema(
    description = "Statut d'une pré-inscription",
    allowableValues = {"PENDING_REVIEW", "APPROVED", "ACCEPTED", "REJECTED", "ACTIVATED"}
)
public enum PreRegistrationStatus {
    PENDING_REVIEW,
    APPROVED,
    ACCEPTED,
    REJECTED,
    ACTIVATED
    ;

    @JsonCreator
    public static PreRegistrationStatus from(String raw) {
        if (raw == null) return null;
        String normalized = raw.trim().toUpperCase();
        if ("ACCEPTED".equals(normalized)) {
            return APPROVED;
        }
        return PreRegistrationStatus.valueOf(normalized);
    }

    @JsonValue
    public String toJson() {
        return name();
    }

    /**
     * Whether a {@link pi.db.piversionbd.entities.groups.Member} may be created for this pre-registration.
     * {@link #APPROVED} / {@link #ACCEPTED} after admin review; {@link #ACTIVATED} after first membership
     * payment (member may be missing e.g. if data was reset) — all are treated as “cleared for member row”.
     */
    public static boolean allowsMemberCreation(PreRegistrationStatus status) {
        if (status == null) {
            return false;
        }
        return status == APPROVED || status == ACCEPTED || status == ACTIVATED;
    }

    @Entity
    @Table(name = "DOCUMENT_UPLOADS")
    @Data
    public static class DocumentUpload {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        /*
         * Membre propriétaire du document.
         * Utilisé dans ClaimService avec doc.getMember().
         */
        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "member_id")
        @JsonIgnore
        private Member member;

        /*
         * Pré-inscription liée au document.
         * Garde ce champ car ton ClaimService utilise :
         * doc.getPreRegistration().getCinNumber()
         */
        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "pre_registration_id")
        @JsonIgnore
        private PreRegistration preRegistration;

        /*
         * Claim lié au bulletin.
         * Utilisé avec doc.setClaim(saved).
         */
        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "claim_id")
        @JsonIgnore
        private Claim claim;

        /*
         * Nom original du fichier uploadé par l'utilisateur.
         * Exemple : bulletin.pdf
         */
        @Column(name = "original_filename")
        private String originalFilename;

        /*
         * Nom réel stocké sur le serveur.
         * Exemple : claim-12-uuid.pdf
         */
        @Column(name = "stored_filename")
        private String storedFilename;

        /*
         * Chemin complet du fichier sur le serveur.
         */
        @Column(name = "file_path", length = 1000)
        private String filePath;

        /*
         * Type MIME du fichier.
         * Exemple : application/pdf, image/png, image/jpeg
         */
        @Column(name = "content_type")
        private String contentType;

        /*
         * Taille du fichier en bytes.
         */
        @Column(name = "size_bytes")
        private Long sizeBytes;

        /*
         * Type métier du document.
         * Exemple : CLAIM_BULLETIN
         */
        @Column(name = "document_type")
        private String documentType;

        /*
         * Date d'upload.
         */
        @CreationTimestamp
        @Column(name = "created_at", updatable = false)
        private LocalDateTime createdAt;
    }
}

