package app.service;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Definición de la plantilla litográfica fija de Capelli.
 * 
 * La plantilla tiene exactamente 22 filas pre-impresas con servicios fijos.
 * Las filas 17-22 pueden ser sobreescritas manualmente en casos de extensiones
 * donde se desglosan sub-tareas (Retiro, Colocación, Administración, etc.)
 * con distintas colaboradoras.
 * 
 * Coordenadas relativas (porcentuales) para la segmentación de ROIs.
 */
public final class CapelliTicketTemplate {

    private CapelliTicketTemplate() {}

    /** Número total de filas de servicios en la plantilla */
    public static final int TOTAL_ROWS = 22;

    /**
     * Mapeo fijo: número de fila → nombre del servicio pre-impreso.
     * Orden litográfico exacto del ticket físico.
     */
    public static final Map<Integer, String> ROW_TO_SERVICE;
    static {
        ROW_TO_SERVICE = new LinkedHashMap<>();
        ROW_TO_SERVICE.put(1,  "Lavado");
        ROW_TO_SERVICE.put(2,  "Hidratación");
        ROW_TO_SERVICE.put(3,  "Corte");
        ROW_TO_SERVICE.put(4,  "Secado");
        ROW_TO_SERVICE.put(5,  "Ondas");
        ROW_TO_SERVICE.put(6,  "Planchado");
        ROW_TO_SERVICE.put(7,  "Color / Tinte");
        ROW_TO_SERVICE.put(8,  "Mechas");
        ROW_TO_SERVICE.put(9,  "Keratina");
        ROW_TO_SERVICE.put(10, "Peinado");
        ROW_TO_SERVICE.put(11, "Maquillaje");
        ROW_TO_SERVICE.put(12, "Pestañas");
        ROW_TO_SERVICE.put(13, "Manicure");
        ROW_TO_SERVICE.put(14, "Pedicure");
        ROW_TO_SERVICE.put(15, "Hidrat. M/P");
        ROW_TO_SERVICE.put(16, "Sistema Uñas");
        ROW_TO_SERVICE.put(17, "Cejas Dep.");
        ROW_TO_SERVICE.put(18, "Bozo Dep.");
        ROW_TO_SERVICE.put(19, "Extensiones");
        ROW_TO_SERVICE.put(20, "Mant. Ext.");
        ROW_TO_SERVICE.put(21, "Productos");
        ROW_TO_SERVICE.put(22, "Propina / Libre");
    }

    /**
     * Filas que pueden contener texto sobreescrito manualmente.
     * Cuando un trabajo de extensiones se desglosa, las filas 17-22
     * se reutilizan para sub-tareas con distintas colaboradoras:
     *   - "Ret." / "Retiro" = Retiro de Extensiones
     *   - "Coloc." / "Colocación" = Colocación de Extensiones
     *   - "Admin." / "Administración" = Comisión administrativa
     *   - "Capelli" = Referencia al salón (comisión interna)
     */
    public static final int OVERWRITABLE_ROW_START = 17;
    public static final int OVERWRITABLE_ROW_END = 22;

    /**
     * Patrones comunes de texto sobreescrito en filas reutilizadas.
     * Clave = prefijo detectado, Valor = servicio normalizado.
     */
    public static final Map<String, String> OVERWRITE_PATTERNS;
    static {
        OVERWRITE_PATTERNS = new LinkedHashMap<>();
        OVERWRITE_PATTERNS.put("ret",     "Retiro de Extensiones");
        OVERWRITE_PATTERNS.put("coloc",   "Colocación de Extensiones");
        OVERWRITE_PATTERNS.put("admin",   "Administración Capelli");
        OVERWRITE_PATTERNS.put("capelli", "Servicio Capelli");
        OVERWRITE_PATTERNS.put("mant",    "Mantenimiento de Extensiones");
        OVERWRITE_PATTERNS.put("lavado",  "Lavado");
        OVERWRITE_PATTERNS.put("secado",  "Secado");
        OVERWRITE_PATTERNS.put("corte",   "Corte");
    }

    /**
     * Mapeo de apodos/abreviaciones conocidas de colaboradoras.
     * Se usa como diccionario base antes de consultar la BD.
     */
    public static final Map<String, String> KNOWN_NICKNAMES;
    static {
        KNOWN_NICKNAMES = new LinkedHashMap<>();
        KNOWN_NICKNAMES.put("jey",     "Jeimy");
        KNOWN_NICKNAMES.put("sey",     "Jeimy");
        KNOWN_NICKNAMES.put("marivi",  "Maria Virginia");
        KNOWN_NICKNAMES.put("marvi",   "Maria Virginia");
        KNOWN_NICKNAMES.put("daya",    "Dayana");
        KNOWN_NICKNAMES.put("sofa",    "Sofia");
        KNOWN_NICKNAMES.put("sofia",   "Sofia");
        KNOWN_NICKNAMES.put("jake",    "Jake");
        KNOWN_NICKNAMES.put("rosa",    "Rosa");
        KNOWN_NICKNAMES.put("mila",    "Milagros");
        KNOWN_NICKNAMES.put("marg",    "Margarita");
    }

    // =========================================================================
    // COORDENADAS RELATIVAS DE LA PLANTILLA (porcentajes del ancho/alto total)
    // =========================================================================

    // --- Encabezado ---
    /** Y relativa donde comienza la zona de la fecha y número de ticket */
    public static final double HEADER_Y_START = 0.06;
    public static final double HEADER_Y_END   = 0.12;

    // --- Tabla de servicios (22 filas) ---
    /** Y relativa donde comienza la primera fila de servicios */
    public static final double TABLE_Y_START  = 0.135;
    /** Y relativa donde termina la última fila de servicios */
    public static final double TABLE_Y_END    = 0.68;

    /** X relativa de las columnas */
    public static final double COL_COD_X_START       = 0.0;
    public static final double COL_COD_X_END         = 0.06;
    public static final double COL_SERVICIO_X_START   = 0.06;
    public static final double COL_SERVICIO_X_END     = 0.36;
    public static final double COL_COLABORADOR_X_START = 0.36;
    public static final double COL_COLABORADOR_X_END   = 0.72;
    public static final double COL_MONTO_X_START      = 0.72;
    public static final double COL_MONTO_X_END        = 0.98;

    // --- Zona de totales ---
    public static final double TOTALS_Y_START = 0.68;
    public static final double TOTALS_Y_END   = 0.76;

    // --- Forma de pago (checkboxes) ---
    public static final double PAYMENT_Y_START = 0.76;
    public static final double PAYMENT_Y_END   = 0.82;

    // --- Datos del cliente ---
    public static final double CLIENT_Y_START = 0.82;
    public static final double CLIENT_Y_END   = 0.96;

    /**
     * Calcula la altura relativa de cada fila de la tabla de servicios.
     */
    public static double getRowHeight() {
        return (TABLE_Y_END - TABLE_Y_START) / TOTAL_ROWS;
    }

    /**
     * Obtiene las coordenadas Y relativas de una fila específica.
     * @param row Número de fila (1-22)
     * @return Array de 2 elementos: [yStart, yEnd]
     */
    public static double[] getRowYBounds(int row) {
        if (row < 1 || row > TOTAL_ROWS) {
            throw new IllegalArgumentException("Fila fuera de rango: " + row);
        }
        double rowH = getRowHeight();
        double yStart = TABLE_Y_START + ((row - 1) * rowH);
        double yEnd = yStart + rowH;
        return new double[]{yStart, yEnd};
    }

    /**
     * Determina si una fila es "sobreescribible" (usada en desglose de extensiones).
     */
    public static boolean isOverwritableRow(int row) {
        return row >= OVERWRITABLE_ROW_START && row <= OVERWRITABLE_ROW_END;
    }

    /**
     * Intenta resolver un texto sobreescrito a un nombre de servicio normalizado.
     * @param rawText Texto manuscrito leído de la celda de servicio
     * @return Nombre normalizado o null si no se reconoce
     */
    public static String resolveOverwrittenService(String rawText) {
        if (rawText == null || rawText.isBlank()) return null;
        String lower = rawText.toLowerCase().trim();
        for (Map.Entry<String, String> entry : OVERWRITE_PATTERNS.entrySet()) {
            if (lower.startsWith(entry.getKey()) || lower.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        return rawText.trim(); // Devolver tal cual si no se reconoce el patrón
    }

    /**
     * Nombres de las casillas de verificación de forma de pago, en orden de aparición.
     */
    public static final String[] PAYMENT_METHODS = {
        "PAGO_MOVIL", "EFECTIVO", "TRANSFERENCIA", "TARJETA_DEBITO", "TARJETA_CREDITO"
    };

    /** Coordenadas X relativas de cada casilla de pago (aprox.) */
    public static final double[][] PAYMENT_CHECKBOX_X_BOUNDS = {
        {0.13, 0.25},  // PAGO MOVIL
        {0.30, 0.42},  // EFECTIVO
        {0.47, 0.60},  // TRANSFERENCIA
        {0.13, 0.22},  // TD (segunda línea)
        {0.25, 0.34}   // TC (segunda línea)
    };
}
