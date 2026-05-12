package app.model;

/**
 * Modelo de ítem de venta (línea de detalle).
 * Migrado desde LEGACY: tabla 'sale_items'.
 * 
 * Cada VentaItem representa un servicio realizado por una trabajadora
 * dentro de una venta específica.
 */
public class VentaItem {

    private int id;
    private int ventaId;
    private int servicioId;
    private int trabajadoraId;
    private double precioVenta;           // Precio aplicado en esta venta
    private boolean clienteTrajoProducto; // Si el cliente trajo su producto (afecta comisión)
    private Integer productoId;           // Producto de inventario usado (si aplica)

    // Campos transitorios para la UI
    private String nombreServicio;
    private String nombreTrabajadora;

    public VentaItem() {
    }

    // ===== Getters & Setters =====

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getVentaId() { return ventaId; }
    public void setVentaId(int ventaId) { this.ventaId = ventaId; }

    public int getServicioId() { return servicioId; }
    public void setServicioId(int servicioId) { this.servicioId = servicioId; }

    public int getTrabajadoraId() { return trabajadoraId; }
    public void setTrabajadoraId(int trabajadoraId) { this.trabajadoraId = trabajadoraId; }

    public double getPrecioVenta() { return precioVenta; }
    public void setPrecioVenta(double precioVenta) { this.precioVenta = precioVenta; }

    public boolean isClienteTrajoProducto() { return clienteTrajoProducto; }
    public void setClienteTrajoProducto(boolean clienteTrajoProducto) { this.clienteTrajoProducto = clienteTrajoProducto; }

    public Integer getProductoId() { return productoId; }
    public void setProductoId(Integer productoId) { this.productoId = productoId; }

    public String getNombreServicio() { return nombreServicio; }
    public void setNombreServicio(String nombreServicio) { this.nombreServicio = nombreServicio; }

    public String getNombreTrabajadora() { return nombreTrabajadora; }
    public void setNombreTrabajadora(String nombreTrabajadora) { this.nombreTrabajadora = nombreTrabajadora; }
}
