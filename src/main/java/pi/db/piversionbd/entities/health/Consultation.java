package pi.db.piversionbd.entities.health;

import jakarta.persistence.*;
import pi.db.piversionbd.entities.groups.Member;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "CONSULTATIONS")
@Data
public class Consultation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne
    @JoinColumn(name = "doctor_id", nullable = false)
    private Doctor doctor;

    @Column(name = "lien", nullable = false)
    private String lien;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_consultation", nullable = false)
    private TypeConsultation typeConsultation;

    @Enumerated(EnumType.STRING)
    @Column(name = "etat_consultation", nullable = false)
    private EtatConsultation etatConsultation;

    @Column(name = "date_consultation")
    private LocalDateTime dateConsultation;

    @Column(name = "notes")
    private String notes;
}

