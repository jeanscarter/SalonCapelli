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

    /**
     * Resuelve un TipoCabello desde su nombre de constante o su label de display.
     * Soporta datos legacy almacenados como "Largo" en lugar de "LARGO".
     *
     * @param value el valor a resolver (nombre enum o label)
     * @return el TipoCabello correspondiente, o null si el valor es null/vacío
     * @throws IllegalArgumentException si el valor no coincide con ningún tipo
     */
    public static TipoCabello fromString(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        // Intento 1: nombre exacto del enum (ej. "LARGO")
        for (TipoCabello tipo : values()) {
            if (tipo.name().equalsIgnoreCase(value)) {
                return tipo;
            }
        }
        // Intento 2: label de display (ej. "Largo", "Con Extensiones")
        for (TipoCabello tipo : values()) {
            if (tipo.label.equalsIgnoreCase(value)) {
                return tipo;
            }
        }
        throw new IllegalArgumentException("TipoCabello no reconocido: " + value);
    }

    @Override
    public String toString() {
        return label;
    }
}