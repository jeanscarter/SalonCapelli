package app.model;

/**
 * Categorías de servicios del salón.
 * Alineadas con la estructura LEGACY para compatibilidad de datos.
 * Utilizadas para agrupar servicios y definir reglas de comisión.
 */
public enum CategoriaServicio {
    PELUQUERIA("Peluqueria"),
    LAVADO("Lavado"),
    QUIMICO("Quimico"),
    MANOS_PIES("Manos/Pies"),
    EXTENSIONES("Extensiones"),
    OTROS("Otros"),
    PAGO_MANUAL("PAGO-MANUAL");

    private final String label;

    CategoriaServicio(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /**
     * Busca una categoría por su label (case-insensitive).
     * Útil para mapear strings de la base de datos al enum.
     */
    public static CategoriaServicio fromLabel(String label) {
        if (label == null) return null;
        for (CategoriaServicio cat : values()) {
            if (cat.label.equalsIgnoreCase(label)) {
                return cat;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return label;
    }
}
