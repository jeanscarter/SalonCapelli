package app.model;

/**
 * Modelo de Pago asociado a una Venta.
 * Migrado desde LEGACY: tabla 'sale_payments'.
 * 
 * Soporta múltiples pagos por venta (split-payment):
 * - Diferentes monedas (USD, Bs)
 * - Diferentes métodos (Efectivo, Transferencia, Punto de Venta, Pago Móvil, Zelle)
 * - Conversión BCV para pagos en Bolívares
 */
public class Pago {

    private int id;
    private int ventaId;
    private double monto;               // Monto en la moneda original
    private String moneda;              // "$" o "Bs"
    private String metodoPago;          // "Efectivo", "Transferencia", "PdV", "Pago Móvil", "Zelle"
    private String destinoPago;         // Cuenta/banco destino (si aplica)
    private String referenciaPago;      // Número de referencia (si aplica)
    private double tasaBcvAlPago;       // Tasa BCV al momento del pago

    public Pago() {
        this.moneda = "$";
    }

    // ===== Getters & Setters =====

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getVentaId() { return ventaId; }
    public void setVentaId(int ventaId) { this.ventaId = ventaId; }

    public double getMonto() { return monto; }
    public void setMonto(double monto) { this.monto = monto; }

    public String getMoneda() { return moneda; }
    public void setMoneda(String moneda) { this.moneda = moneda; }

    public String getMetodoPago() { return metodoPago; }
    public void setMetodoPago(String metodoPago) { this.metodoPago = metodoPago; }

    public String getDestinoPago() { return destinoPago; }
    public void setDestinoPago(String destinoPago) { this.destinoPago = destinoPago; }

    public String getReferenciaPago() { return referenciaPago; }
    public void setReferenciaPago(String referenciaPago) { this.referenciaPago = referenciaPago; }

    public double getTasaBcvAlPago() { return tasaBcvAlPago; }
    public void setTasaBcvAlPago(double tasaBcvAlPago) { this.tasaBcvAlPago = tasaBcvAlPago; }

    // ===== Cálculos =====

    /**
     * Retorna el monto equivalente en dólares.
     * Si el pago fue en Bs, convierte usando la tasa BCV capturada.
     */
    public double getMontoDolares() {
        if ("Bs".equalsIgnoreCase(moneda) && tasaBcvAlPago > 0) {
            return monto / tasaBcvAlPago;
        }
        return monto;
    }

    /**
     * Retorna el monto equivalente en bolívares.
     * Si el pago fue en USD, convierte usando la tasa BCV capturada.
     */
    public double getMontoBolivares() {
        if ("$".equals(moneda) && tasaBcvAlPago > 0) {
            return monto * tasaBcvAlPago;
        }
        return monto;
    }
}
