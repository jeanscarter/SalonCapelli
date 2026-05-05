package app.model;

/**
 * Categorías de servicios del salón
 * Utilizadas para agrupar servicios y definir reglas de comisión
 */
public enum CategoriaServicio {
    LAVADO("Lavado"),
    QUIMICO("Químico"),
    CORTE("Corte"),
    PEINADO("Peinado"),
    TRATAMIENTO("Tratamiento"),
    COLOR("Color"),
    EXTENSIONES("Extensiones"),
    OTROS("Otros");

    private final String label;

    CategoriaServicio(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return label;
    }
}
