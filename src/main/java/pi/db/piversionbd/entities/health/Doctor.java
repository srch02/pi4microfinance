package pi.db.piversionbd.entities.health;

import com.fasterxml.jackson.annotation.JsonIgnore;
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

    // This relationship is lazy by default; ignoring it prevents "no session" serialization failures
    // when returning doctors via REST endpoints.
    @JsonIgnore
    @OneToMany(mappedBy = "doctor", cascade = jakarta.persistence.CascadeType.ALL, orphanRemoval = true)
    private List<Consultation> consultations;
}

