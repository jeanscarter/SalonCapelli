package app.model;

import java.util.EnumMap;
import java.util.Map;

/**
 * Modelo de Servicio del salón con precios dinámicos por TipoCabello
 * Migrado de CapelliSalesWindow con nueva estructura de precios
 */
public class Servicio {

    private int id;
    private String nombre;
    private CategoriaServicio categoria;
    private boolean activo;

    // Mapa de precios dinámicos por tipo de cabello
    private final Map<TipoCabello, Double> preciosPorTipo;

    // Cliente trae su producto (precio especial)
    private boolean permiteClienteProducto;
    private double precioClienteProducto;

    public Servicio() {
        this.preciosPorTipo = new EnumMap<>(TipoCabello.class);
        this.activo = true;
        this.permiteClienteProducto = false;
        this.precioClienteProducto = 0.0;
        // Inicializar todos los precios en 0
        for (TipoCabello tipo : TipoCabello.values()) {
            preciosPorTipo.put(tipo, 0.0);
        }
    }

    // ===== Datos básicos =====

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public CategoriaServicio getCategoria() { return categoria; }
    public void setCategoria(CategoriaServicio categoria) { this.categoria = categoria; }

    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }

    // ===== Precios dinámicos =====

    public Map<TipoCabello, Double> getPreciosPorTipo() { return preciosPorTipo; }

    public double getPrecio(TipoCabello tipo) {
        return preciosPorTipo.getOrDefault(tipo, 0.0);
    }

    public void setPrecio(TipoCabello tipo, double precio) {
        preciosPorTipo.put(tipo, precio);
    }

    // Atajos legacy-compatible
    public double getPrecioCorto() { return getPrecio(TipoCabello.CORTO); }
    public void setPrecioCorto(double p) { setPrecio(TipoCabello.CORTO, p); }

    public double getPrecioMediano() { return getPrecio(TipoCabello.MEDIANO); }
    public void setPrecioMediano(double p) { setPrecio(TipoCabello.MEDIANO, p); }

    public double getPrecioLargo() { return getPrecio(TipoCabello.LARGO); }
    public void setPrecioLargo(double p) { setPrecio(TipoCabello.LARGO, p); }

    public double getPrecioExtensiones() { return getPrecio(TipoCabello.CON_EXTENSIONES); }
    public void setPrecioExtensiones(double p) { setPrecio(TipoCabello.CON_EXTENSIONES, p); }

    // ===== Cliente trae producto =====

    public boolean isPermiteClienteProducto() { return permiteClienteProducto; }
    public void setPermiteClienteProducto(boolean permiteClienteProducto) { this.permiteClienteProducto = permiteClienteProducto; }

    public double getPrecioClienteProducto() { return precioClienteProducto; }
    public void setPrecioClienteProducto(double precioClienteProducto) { this.precioClienteProducto = precioClienteProducto; }
}
