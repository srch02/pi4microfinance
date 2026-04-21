package pi.db.piversionbd.entities.health;

public enum EtatConsultation {
    EN_ATTENTE("En attente"),
    CONFIRMEE("Confirmée"),
    ANNULEE("Annulée"),
    COMPLETEE("Complétée");

    private final String label;

    EtatConsultation(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}

