package pi.db.piversionbd.entities.health;

import jakarta.persistence.*;
import lombok.Data;
import pi.db.piversionbd.entities.groups.Member;
import pi.db.piversionbd.entities.groups.Payment;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "CONSULTATIONS")
@Data
public class Consultation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "member_id")
    private Member member;

    @ManyToOne
    @JoinColumn(name = "doctor_id")
    private Doctor doctor;

    @Column(name = "scheduled_time")
    private LocalDateTime scheduledTime;

    @Enumerated(EnumType.STRING)
    private ConsultationStatus status;


    @Lob
    private String diagnosis;

    @Lob
    private String prescription;

    @Column(name = "is_telemedicine")
    private Boolean telemedicine;

    @ManyToMany
    private List<Medication> medications;
    @OneToOne
    @JoinColumn(name = "payment_id")
    private Payment payment;

}

