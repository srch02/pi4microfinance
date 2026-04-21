package pi.db.piversionbd.entities.health;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "medication_recognition_features")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MedicationRecognitionFeature {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medication_analysis_id", nullable = false)
    private MedicationAnalysis medicationAnalysis;

    @Column(name = "recognition_confidence")
    private String recognitionConfidence; // très haute, haute, moyenne
    
    @Column(name = "visual_shape")
    private String visualShape; // forme du comprimé/capsule
    
    @Column(name = "visual_color")
    private String visualColor; // couleur
    
    @Column(name = "visual_markings")
    private String visualMarkings; // marques imprimées
    
    @Column(name = "identification_code", unique = true)
    private String identificationCode; // code/numéro identifiant
    
    @Column(name = "serious_contraindications", columnDefinition = "TEXT")
    private String seriousContraindications; // contre-indications graves
    
    @Column(name = "serious_side_effects", columnDefinition = "TEXT")
    private String seriousSideEffects; // effets secondaires graves
    
    @Column(name = "dangerous_interactions", columnDefinition = "TEXT")
    private String dangerousInteractions; // interactions dangereuses
    
    @Column(name = "storage_conditions")
    private String storageConditions; // conditions de stockage
    
    @Column(name = "safety_checks", columnDefinition = "TEXT")
    private String safetyChecks; // vérifications de sécurité
}

