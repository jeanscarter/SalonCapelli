package app.model;

/**
 * Modelo de Cuenta Receptora del Salón.
 * 
 * Representa las cuentas donde el salón recibe dinero de los clientes.
 * A diferencia de {@link CuentaBancaria} (cuentas de empleadas para pago de nómina),
 * estas son las cuentas destino para cobros:
 * 
 * Ejemplos:
 *   - "Cuenta Capelli" → Zelle Default, Banesco, Punto de Venta
 *   - "Cuenta Rosa"    → Zelle Hotmail, Zelle Ingrid, Mercantil
 *   - "Efectivo"       → Caja física
 */
public class CuentaReceptora {

    private int id;
    private String nombreCuenta;        // Ej: "Cuenta Capelli", "Cuenta Rosa", "Caja"
    private String bancoPlataforma;     // Ej: "Zelle", "BNC", "Bancrecer", "Banesco", "Efectivo"
    private String aliasReferencia;     // Ej: "Zelle Hotmail", "Zelle Ingrid", "Zelle Default"
    private boolean activa;

    public CuentaReceptora() {
        this.activa = true;
    }

    public CuentaReceptora(String nombreCuenta, String bancoPlataforma, String aliasReferencia) {
        this();
        this.nombreCuenta = nombreCuenta;
        this.bancoPlataforma = bancoPlataforma;
        this.aliasReferencia = aliasReferencia;
    }

    // ===== Getters & Setters =====

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNombreCuenta() { return nombreCuenta; }
    public void setNombreCuenta(String nombreCuenta) { this.nombreCuenta = nombreCuenta; }

    public String getBancoPlataforma() { return bancoPlataforma; }
    public void setBancoPlataforma(String bancoPlataforma) { this.bancoPlataforma = bancoPlataforma; }

    public String getAliasReferencia() { return aliasReferencia; }
    public void setAliasReferencia(String aliasReferencia) { this.aliasReferencia = aliasReferencia; }

    public boolean isActiva() { return activa; }
    public void setActiva(boolean activa) { this.activa = activa; }

    @Override
    public String toString() {
        return aliasReferencia != null && !aliasReferencia.isEmpty()
                ? aliasReferencia
                : nombreCuenta + " - " + bancoPlataforma;
    }
}
