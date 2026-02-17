package pi.db.piversionbd.entities.pre;

import jakarta.persistence.*;
import lombok.Data;
import pi.db.piversionbd.entities.groups.Member;
import pi.db.piversionbd.entities.score.Claim;

@Entity
@Table(name = "DOCUMENT_UPLOADS")
@Data
public class DocumentUpload {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "pre_registration_id")
    private PreRegistration preRegistration;

    @ManyToOne
    @JoinColumn(name = "member_id")
    private Member member;

    @ManyToOne
    @JoinColumn(name = "claim_id")
    private Claim claim;

    @Column(name = "fraud_detection_score")
    private Float fraudDetectionScore;

    // Optional: store file path on disk for reference / download
    @Column(name = "file_path")
    private String filePath;
}

