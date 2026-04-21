package pi.db.piversionbd.service.health;

import pi.db.piversionbd.entities.health.Produit;
import pi.db.piversionbd.entities.health.MaladieProduit;
import pi.db.piversionbd.repository.health.ProduitRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ProduitServiceImpl implements IProduitService {

    @Autowired
    private ProduitRepository produitRepository;

    @Override
    public Produit saveProduit(Produit produit) {
        return produitRepository.save(produit);
    }

    @Override
    public Optional<Produit> getProduitById(Long id) {
        return produitRepository.findById(id);
    }

    @Override
    public List<Produit> getAllProduits() {
        return produitRepository.findAll();
    }

    @Override
    public Produit updateProduit(Long id, Produit produit) {
        Optional<Produit> existingProduit = produitRepository.findById(id);
        if (existingProduit.isPresent()) {
            Produit p = existingProduit.get();
            if (produit.getName() != null) {
                p.setName(produit.getName());
            }
            if (produit.getTypeProduit() != null) {
                p.setTypeProduit(produit.getTypeProduit());
            }
            if (produit.getMaladieProduit() != null) {
                p.setMaladieProduit(produit.getMaladieProduit());
            }
            return produitRepository.save(p);
        }
        return null;
    }

    @Override
    public void deleteProduit(Long id) {
        produitRepository.deleteById(id);
    }

    @Override
    public Optional<Produit> getProduitByName(String name) {
        return produitRepository.findByName(name);
    }

    @Override
    public List<Produit> getProduitsByMaladie(MaladieProduit maladieProduit) {
        return produitRepository.findByMaladieProduit(maladieProduit);
    }
}

