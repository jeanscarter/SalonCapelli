package app.model;

/**
 * Modelo de Propina asociada a una Venta.
 * Migrado desde LEGACY: tabla 'tips'.
 * 
 * La propina va dirigida a una trabajadora específica (por ID, no por nombre).
 * En el LEGACY se usaba recipient_name (string), aquí se normaliza con FK.
 */
public class Propina {

    private int id;
    private int ventaId;
    private int trabajadoraId;
    private double monto;

    // Campo transitorio para UI
    private String nombreTrabajadora;

    public Propina() {
    }

    public Propina(int ventaId, int trabajadoraId, double monto) {
        this.ventaId = ventaId;
        this.trabajadoraId = trabajadoraId;
        this.monto = monto;
    }

    // ===== Getters & Setters =====

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getVentaId() { return ventaId; }
    public void setVentaId(int ventaId) { this.ventaId = ventaId; }

    public int getTrabajadoraId() { return trabajadoraId; }
    public void setTrabajadoraId(int trabajadoraId) { this.trabajadoraId = trabajadoraId; }

    public double getMonto() { return monto; }
    public void setMonto(double monto) { this.monto = monto; }

    public String getNombreTrabajadora() { return nombreTrabajadora; }
    public void setNombreTrabajadora(String nombreTrabajadora) { this.nombreTrabajadora = nombreTrabajadora; }
}
