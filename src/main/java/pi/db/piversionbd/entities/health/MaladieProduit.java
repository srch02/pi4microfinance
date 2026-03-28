package pi.db.piversionbd.entities.health;

public enum MaladieProduit {
    GRIPPE("Grippe"),
    MIGRAINE("Migraine"),
    MAL_DE_TETE("Mal de tête"),
    FIEVRE("Fièvre"),
    DOULEUR_MUSCULAIRE("Douleur musculaire"),
    ALLERGIE("Allergie"),
    TOUX("Toux"),
    RHUME("Rhume"),
    CONSTIPATION("Constipation"),
    ACIDITE("Acidité gastrique"),
    HYPERTENSION("Hypertension"),
    INFECTION_BACTERIENNE("Infection bactérienne"),
    INFLAMMATION("Inflammation");

    private final String label;

    MaladieProduit(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}

