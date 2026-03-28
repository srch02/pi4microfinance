package pi.db.piversionbd.repositories;

import pi.db.piversionbd.entities.health.Produit;
import pi.db.piversionbd.entities.health.MaladieProduit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProduitRepository extends JpaRepository<Produit, Long> {
    Optional<Produit> findByName(String name);
    List<Produit> findByMaladieProduit(MaladieProduit maladieProduit);
}

