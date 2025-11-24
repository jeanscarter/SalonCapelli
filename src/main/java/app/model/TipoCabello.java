package app.model;

public enum TipoCabello {
    CORTO("Corto"),
    MEDIANO("Mediano"),
    LARGO("Largo"),
    CON_EXTENSIONES("Con Extensiones");

    private final String label;

    TipoCabello(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return label;
    }
}