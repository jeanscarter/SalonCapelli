package app.model;

import java.time.LocalDateTime;

/**
 * Modelo de Cuenta por Cobrar.
 * Representa una deuda pendiente de un cliente (saldo de una venta no pagada totalmente).
 */
public class CuentaPorCobrar {

    private int id;
    private int clienteId;
    private int ventaId;
    private double montoOriginal;
    private double montoPendiente;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaUltimoAbono;
    private String estatus; // PENDIENTE, PARCIAL, PAGADA

    // Campos transitorios para UI
    private String nombreCliente;
    private String numeroFactura;

    public CuentaPorCobrar() {
        this.estatus = "PENDIENTE";
        this.fechaCreacion = LocalDateTime.now();
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getClienteId() { return clienteId; }
    public void setClienteId(int clienteId) { this.clienteId = clienteId; }

    public int getVentaId() { return ventaId; }
    public void setVentaId(int ventaId) { this.ventaId = ventaId; }

    public double getMontoOriginal() { return montoOriginal; }
    public void setMontoOriginal(double montoOriginal) { this.montoOriginal = montoOriginal; }

    public double getMontoPendiente() { return montoPendiente; }
    public void setMontoPendiente(double montoPendiente) { this.montoPendiente = montoPendiente; }

    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }

    public LocalDateTime getFechaUltimoAbono() { return fechaUltimoAbono; }
    public void setFechaUltimoAbono(LocalDateTime fechaUltimoAbono) { this.fechaUltimoAbono = fechaUltimoAbono; }

    public String getEstatus() { return estatus; }
    public void setEstatus(String estatus) { this.estatus = estatus; }

    public String getNombreCliente() { return nombreCliente; }
    public void setNombreCliente(String nombreCliente) { this.nombreCliente = nombreCliente; }

    public String getNumeroFactura() { return numeroFactura; }
    public void setNumeroFactura(String numeroFactura) { this.numeroFactura = numeroFactura; }
}
