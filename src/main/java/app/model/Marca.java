package app.model;

/**
 * Modelo de Marca para el módulo de inventario.
 * Cada marca puede tener un logo y estar asociada a múltiples productos.
 */
public class Marca {

    private int id;
    private String nombre;
    private String rutaImagen;  // Ruta al archivo de logo/imagen
    private String descripcion;
    private boolean activa;

    public Marca() {
        this.activa = true;
    }

    public Marca(String nombre) {
        this();
        this.nombre = nombre;
    }

    // ===== Getters & Setters =====

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getRutaImagen() { return rutaImagen; }
    public void setRutaImagen(String rutaImagen) { this.rutaImagen = rutaImagen; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public boolean isActiva() { return activa; }
    public void setActiva(boolean activa) { this.activa = activa; }

    @Override
    public String toString() {
        return nombre;
    }
}
