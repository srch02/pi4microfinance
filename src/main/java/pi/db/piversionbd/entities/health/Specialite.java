package pi.db.piversionbd.entities.health;

public enum Specialite {
    DENTISTE("Dentiste"),
    CARDIOLOGIE("Cardiologie"),
    NEUROLOGIE("Neurologie"),
    ORTHOPÉDIE("Orthopédie"),
    DERMATOLOGIE("Dermatologie"),
    PNEUMOLOGIE("Pneumologie"),
    GASTRO_ENTEROLOGIE("Gastro-entérologie"),
    OPHTALMOLOGIE("Ophtalmologie"),
    UROLOGIE("Urologie"),
    RHUMATOLOGIE("Rhumatologie"),
    TRAUMATOLOGIE("Traumatologie"),
    MÉDECINE_GÉNÉRALE("Médecine générale"),
    PÉDIATRIE("Pédiatrie"),
    GYNÉCOLOGIE("Gynécologie");

    private final String label;

    Specialite(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}

