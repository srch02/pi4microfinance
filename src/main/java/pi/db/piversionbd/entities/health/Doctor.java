package pi.db.piversionbd.entities.health;

import jakarta.persistence.*;
import lombok.Data;

import java.util.List;

@Entity
@Table(name = "DOCTORS")
@Data
public class Doctor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private Specialty specialty;
    private Float rating;

    @Column(name = "accepts_telemedicine")
    private Boolean acceptsTelemedicine;

    private String region;

    @Column(name = "consultation_fee")
    private Float consultationFee;

    @OneToMany(mappedBy = "doctor")
    private List<Consultation> consultations;
}

