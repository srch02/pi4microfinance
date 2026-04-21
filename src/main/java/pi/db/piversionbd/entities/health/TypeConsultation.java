package pi.db.piversionbd.entities.health;

public enum TypeConsultation {
    EN_LIGNE("En ligne"),
    SUR_PLACE("Sur place");

    private final String label;

    TypeConsultation(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}

