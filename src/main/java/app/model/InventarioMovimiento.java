package app.model;

/**
 * Modelo para movimientos de inventario (auditoría).
 * Registra todas las entradas y salidas de productos del inventario.
 */
public class InventarioMovimiento {

    private int id;
    private int productoId;
    private String tipoMovimiento;  // 'ENTRADA' | 'SALIDA' | 'AJUSTE'
    private int cantidad;
    private String motivo;          // "Compra", "Uso en servicio", "Ajuste manual", etc.
    private Integer ventaId;        // Si el movimiento está asociado a una venta
    private String fechaMovimiento;

    // Campos transitorios para la UI
    private String nombreProducto;

    public InventarioMovimiento() {
    }

    // ===== Getters & Setters =====

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getProductoId() { return productoId; }
    public void setProductoId(int productoId) { this.productoId = productoId; }

    public String getTipoMovimiento() { return tipoMovimiento; }
    public void setTipoMovimiento(String tipoMovimiento) { this.tipoMovimiento = tipoMovimiento; }

    public int getCantidad() { return cantidad; }
    public void setCantidad(int cantidad) { this.cantidad = cantidad; }

    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }

    public Integer getVentaId() { return ventaId; }
    public void setVentaId(Integer ventaId) { this.ventaId = ventaId; }

    public String getFechaMovimiento() { return fechaMovimiento; }
    public void setFechaMovimiento(String fechaMovimiento) { this.fechaMovimiento = fechaMovimiento; }

    public String getNombreProducto() { return nombreProducto; }
    public void setNombreProducto(String nombreProducto) { this.nombreProducto = nombreProducto; }
}
