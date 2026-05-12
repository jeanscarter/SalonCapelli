package app.model;


/**
 * Modelo para las reglas de comisión
 */
public class ReglaComision {

    private int id;
    private int trabajadoraId;
    private Trabajadora trabajadora;
    private String categoriaServicio;
    private double porcentajeComision;

    public ReglaComision() {
    }

    public ReglaComision(int trabajadoraId, String categoriaServicio, double porcentajeComision) {
        this.trabajadoraId = trabajadoraId;
        this.categoriaServicio = categoriaServicio;
        this.porcentajeComision = porcentajeComision;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getTrabajadoraId() { return trabajadoraId; }
    public void setTrabajadoraId(int trabajadoraId) { this.trabajadoraId = trabajadoraId; }

    public Trabajadora getTrabajadora() { return trabajadora; }
    public void setTrabajadora(Trabajadora trabajadora) { this.trabajadora = trabajadora; }

    public String getCategoriaServicio() { return categoriaServicio; }
    public void setCategoriaServicio(String categoriaServicio) { this.categoriaServicio = categoriaServicio; }

    public double getPorcentajeComision() { return porcentajeComision; }
    public void setPorcentajeComision(double porcentajeComision) { this.porcentajeComision = porcentajeComision; }
}
