package pi.db.piversionbd.entities.health;

public enum TypeDoctor {
    MEDECINS_GENERALISTES("Médecins Généralistes"),
    DENTISTES("Dentistes"),
    URGENCES("Urgences");

    private final String label;

    TypeDoctor(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}

