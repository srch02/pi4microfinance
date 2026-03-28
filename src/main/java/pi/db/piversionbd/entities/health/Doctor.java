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

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "password", nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_doctor", nullable = false)
    private TypeDoctor typeDoctor;

    @Enumerated(EnumType.STRING)
    @Column(name = "specialite", nullable = false)
    private Specialite specialite;

    @OneToMany(mappedBy = "doctor", cascade = jakarta.persistence.CascadeType.ALL, orphanRemoval = true)
    private List<Consultation> consultations;
}

