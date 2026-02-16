package pi.db.piversionbd.entities.health;

import jakarta.persistence.*;
import lombok.Data;

import java.util.List;

@Entity
@Table(name = "MEDICATIONS")
@Data
public class Medication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(name = "generic_name")
    private String genericName;

    private String category;

    @Column(name = "typical_cost")
    private Float typicalCost;

    @Column(name = "discount_rate")
    private Float discountRate;

    @Column(name = "is_covered")
    private Boolean covered;

    @ManyToMany(mappedBy = "medications")
    private List<Consultation> consultations;

    @OneToMany(mappedBy = "medication")
    private List<PharmacyRecommendation> pharmacyRecommendations;

}

