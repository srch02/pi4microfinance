package pi.db.piversionbd.entities.health;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "PRODUITS")
@Data
public class Produit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_produit", nullable = false)
    private TypeProduit typeProduit;

    @Enumerated(EnumType.STRING)
    @Column(name = "maladie_produit", nullable = false)
    private MaladieProduit maladieProduit;
}

