package pi.db.piversionbd.entities.health;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import pi.db.piversionbd.entities.groups.Member;
import java.time.LocalDateTime;

@Entity
@Table(name = "medication_analysis")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MedicationAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    @JsonIgnoreProperties({
            // Avoid lazy-init JSON errors when serializing medication analysis responses
            "memberships", "payments", "medicalHistories", "documentUploads",
            "adherenceTrackingEvents", "memberRewards", "consultations",
            "pharmacyRecommendations", "healthTrackingEntries",
            "memberChurnForecasts", "retentionInterventions",
            "adminReviewQueueItems", "claims", "createdGroups",
            "preRegistration", "currentGroup"
    })
    private Member member;

    @Column(name = "medication_name", nullable = false)
    private String medicationName;

    @Column(name = "active_ingredients", columnDefinition = "TEXT")
    private String activeIngredients;

    @Column(name = "indications", columnDefinition = "TEXT")
    private String indications;

    @Column(name = "dosage", columnDefinition = "TEXT")
    private String dosage;

    @Column(name = "side_effects", columnDefinition = "TEXT")
    private String sideEffects;

    @Column(name = "contraindications", columnDefinition = "TEXT")
    private String contraindications;

    @Column(name = "usage_advice", columnDefinition = "TEXT")
    private String usageAdvice;

    @Column(name = "interactions", columnDefinition = "TEXT")
    private String interactions;

    @Column(name = "raw_analysis", columnDefinition = "LONGTEXT")
    private String rawAnalysis;

    @Column(name = "image_path")
    private String imagePath;

    @Column(name = "analysis_date", nullable = false)
    private LocalDateTime analysisDate;

    @PrePersist
    protected void onCreate() {
        analysisDate = LocalDateTime.now();
    }
}

