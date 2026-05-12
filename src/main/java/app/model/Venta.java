package app.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Modelo de Venta (cabecera).
 * Migrado desde LEGACY: tabla 'sales'.
 * 
 * Soporta lógica de:
 * - Múltiples ítems (servicios) por venta
 * - Múltiples pagos (multi-moneda, multi-método)
 * - Propinas asociadas
 * - Balance/saldo del cliente (vuelto y saldo a favor)
 * - Correlativo y tasa BCV al momento de la venta
 */
public class Venta {

    private int id;
    private Integer clienteId;          // Puede ser NULL (venta sin cliente registrado)
    private LocalDateTime fechaVenta;
    private double subtotal;
    private String tipoDescuento;       // "PORCENTAJE" | "MONTO" | null
    private double montoDescuento;
    private double montoIva;            // IVA calculado
    private double total;
    private double tasaBcv;             // Tasa BCV al momento de la venta
    private String numeroCorrelativo;   // Ej. "000042"

    // Colecciones hijas (cargadas bajo demanda o en transacciones)
    private List<VentaItem> items;
    private List<Pago> pagos;
    private List<Propina> propinas;

    // Campo transitorio para la UI
    private String nombreCliente;

    public Venta() {
        this.items = new ArrayList<>();
        this.pagos = new ArrayList<>();
        this.propinas = new ArrayList<>();
        this.fechaVenta = LocalDateTime.now();
    }

    // ===== Getters & Setters =====

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public Integer getClienteId() { return clienteId; }
    public void setClienteId(Integer clienteId) { this.clienteId = clienteId; }

    public LocalDateTime getFechaVenta() { return fechaVenta; }
    public void setFechaVenta(LocalDateTime fechaVenta) { this.fechaVenta = fechaVenta; }

    public double getSubtotal() { return subtotal; }
    public void setSubtotal(double subtotal) { this.subtotal = subtotal; }

    public String getTipoDescuento() { return tipoDescuento; }
    public void setTipoDescuento(String tipoDescuento) { this.tipoDescuento = tipoDescuento; }

    public double getMontoDescuento() { return montoDescuento; }
    public void setMontoDescuento(double montoDescuento) { this.montoDescuento = montoDescuento; }

    public double getMontoIva() { return montoIva; }
    public void setMontoIva(double montoIva) { this.montoIva = montoIva; }

    public double getTotal() { return total; }
    public void setTotal(double total) { this.total = total; }

    public double getTasaBcv() { return tasaBcv; }
    public void setTasaBcv(double tasaBcv) { this.tasaBcv = tasaBcv; }

    public String getNumeroCorrelativo() { return numeroCorrelativo; }
    public void setNumeroCorrelativo(String numeroCorrelativo) { this.numeroCorrelativo = numeroCorrelativo; }

    public List<VentaItem> getItems() { return items; }
    public void setItems(List<VentaItem> items) { this.items = items; }

    public List<Pago> getPagos() { return pagos; }
    public void setPagos(List<Pago> pagos) { this.pagos = pagos; }

    public List<Propina> getPropinas() { return propinas; }
    public void setPropinas(List<Propina> propinas) { this.propinas = propinas; }

    public String getNombreCliente() { return nombreCliente; }
    public void setNombreCliente(String nombreCliente) { this.nombreCliente = nombreCliente; }

    // ===== Cálculos derivados =====

    /**
     * Suma total de todos los pagos realizados
     */
    public double getTotalPagado() {
        return pagos.stream().mapToDouble(Pago::getMontoDolares).sum();
    }

    /**
     * Vuelto o saldo a favor del cliente.
     * Positivo = vuelto (el cliente pagó de más).
     * Negativo = saldo pendiente.
     */
    public double getVuelto() {
        return getTotalPagado() - total;
    }

    /**
     * Verifica si la venta está completamente pagada
     */
    public boolean isPagada() {
        return getTotalPagado() >= total - 0.01; // tolerancia flotante
    }

    /**
     * Total de propinas en esta venta
     */
    public double getTotalPropinas() {
        return propinas.stream().mapToDouble(Propina::getMonto).sum();
    }
}
