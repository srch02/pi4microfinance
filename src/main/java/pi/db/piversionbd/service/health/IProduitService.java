package pi.db.piversionbd.service.health;

import pi.db.piversionbd.entities.health.Produit;
import pi.db.piversionbd.entities.health.MaladieProduit;

import java.util.List;
import java.util.Optional;

public interface IProduitService {
    Produit saveProduit(Produit produit);
    Optional<Produit> getProduitById(Long id);
    List<Produit> getAllProduits();
    Produit updateProduit(Long id, Produit produit);
    void deleteProduit(Long id);
    Optional<Produit> getProduitByName(String name);
    List<Produit> getProduitsByMaladie(MaladieProduit maladieProduit);
}

