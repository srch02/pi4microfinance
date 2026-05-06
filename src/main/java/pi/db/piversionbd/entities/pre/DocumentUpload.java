package pi.db.piversionbd.entities.pre;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import pi.db.piversionbd.entities.groups.Member;
import pi.db.piversionbd.entities.score.Claim;

import java.time.LocalDateTime;

@Entity
@Table(name = "DOCUMENT_UPLOADS")
@Data
public class DocumentUpload {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /*
     * Membre propriétaire du document.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    @JsonIgnore
    private Member member;

    /*
     * Pré-inscription liée au document.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pre_registration_id")
    @JsonIgnore
    private PreRegistration preRegistration;

    /*
     * Claim lié au bulletin.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "claim_id")
    @JsonIgnore
    private Claim claim;

    /*
     * Champs anciens utilisés par PreRegistrationServiceImpl.
     */
    @Column(name = "file_name")
    private String fileName;

    @Column(name = "file_type")
    private String fileType;

    @Column(name = "file_url", length = 1000)
    private String fileUrl;

    @Column(name = "uploaded_at")
    private LocalDateTime uploadedAt;

    @Column(name = "extracted_cin")
    private String extractedCin;

    @Lob
    @Column(name = "extracted_text")
    private String extractedText;

    @Lob
    @Column(name = "analysis_summary")
    private String analysisSummary;

    @Column(name = "fraud_detection_score")
    private Float fraudDetectionScore;

    /*
     * Nouveaux champs pour bulletin claim.
     */
    @Column(name = "original_filename")
    private String originalFilename;

    @Column(name = "stored_filename")
    private String storedFilename;

    @Column(name = "file_path", length = 1000)
    private String filePath;

    @Column(name = "content_type")
    private String contentType;

    @Column(name = "size_bytes")
    private Long sizeBytes;

    @Column(name = "document_type")
    private String documentType;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}