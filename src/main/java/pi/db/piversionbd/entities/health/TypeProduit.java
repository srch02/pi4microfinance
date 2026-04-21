package pi.db.piversionbd.entities.health;

public enum TypeProduit {
    PARACETAMOL("Paracétamol"),
    PENICILLINE("Pénicilline"),
    IBUPROFEN("Ibuprofène"),
    ASPIRIN("Aspirine"),
    AMOXICILLIN("Amoxicilline"),
    CETIRIZINE("Cétirizine"),
    OMEPRAZOLE("Oméprazole"),
    LORATADINE("Loratadine"),
    METFORMIN("Metformine"),
    LISINOPRIL("Lisinopril");

    private final String label;

    TypeProduit(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}

