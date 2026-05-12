package app.model;

/**
 * Modelo de Producto del módulo de inventario.
 * Cada producto pertenece a una Marca y mantiene un stock actual.
 */
public class Producto {

    private int id;
    private String nombre;
    private String descripcion;
    private int marcaId;
    private double precioCompra;
    private double precioVenta;
    private int stockActual;
    private int stockMinimo;        // Alerta cuando stock cae bajo este valor
    private String unidadMedida;    // "unidad", "ml", "g", etc.
    private boolean activo;
    private String fechaCreacion;

    // Campo transitorio para UI
    private Marca marca;

    public Producto() {
        this.activo = true;
        this.stockActual = 0;
        this.stockMinimo = 1;
        this.unidadMedida = "unidad";
    }

    // ===== Getters & Setters =====

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public int getMarcaId() { return marcaId; }
    public void setMarcaId(int marcaId) { this.marcaId = marcaId; }

    public double getPrecioCompra() { return precioCompra; }
    public void setPrecioCompra(double precioCompra) { this.precioCompra = precioCompra; }

    public double getPrecioVenta() { return precioVenta; }
    public void setPrecioVenta(double precioVenta) { this.precioVenta = precioVenta; }

    public int getStockActual() { return stockActual; }
    public void setStockActual(int stockActual) { this.stockActual = stockActual; }

    public int getStockMinimo() { return stockMinimo; }
    public void setStockMinimo(int stockMinimo) { this.stockMinimo = stockMinimo; }

    public String getUnidadMedida() { return unidadMedida; }
    public void setUnidadMedida(String unidadMedida) { this.unidadMedida = unidadMedida; }

    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }

    public String getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(String fechaCreacion) { this.fechaCreacion = fechaCreacion; }

    public Marca getMarca() { return marca; }
    public void setMarca(Marca marca) { this.marca = marca; }

    /**
     * Verifica si el stock está por debajo del mínimo configurado
     */
    public boolean isStockBajo() {
        return stockActual <= stockMinimo;
    }

    /**
     * Margen de ganancia unitario
     */
    public double getMargenUnitario() {
        return precioVenta - precioCompra;
    }

    @Override
    public String toString() {
        return nombre + (marca != null ? " (" + marca.getNombre() + ")" : "");
    }
}
