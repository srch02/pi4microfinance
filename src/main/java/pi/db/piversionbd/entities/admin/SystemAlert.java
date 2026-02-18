package pi.db.piversionbd.entities.admin;

import jakarta.persistence.*;

import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "SYSTEM_ALERTS")
@Data
public class SystemAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "alert_type")
    private String alertType;

    private String severity;

    private String region;

    private String title;

    @Lob
    private String message;

    private Boolean active;

    @Column(name = "expiration_date")
    private LocalDateTime expirationDate;
    @Lob


    /** Source entity type for linking (e.g. GROUP_CHANGE_REQUEST, GROUP). */
    @Column(name = "source_entity_type", length = 64)
    private String sourceEntityType;

    /** Source entity ID (e.g. request id, group id). */
    @Column(name = "source_entity_id")
    private Long sourceEntityId;
}

