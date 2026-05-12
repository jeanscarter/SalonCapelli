package app.model;

/**
 * Modelo avanzado de reglas de comisión detalladas.
 * 
 * Reemplaza TODA la lógica hardcoded de PayrollService.calculateCommissionForItem()
 * del proyecto LEGACY. Cada fila en esta tabla representa una regla de comisión
 * que se evalúa dinámicamente por filtros:
 * 
 * - trabajadora_id: Aplica solo a una trabajadora específica (NULL = todas)
 * - servicio_id: Aplica solo a un servicio específico (NULL = todos de la categoría)
 * - categoria_servicio: Aplica a una categoría completa (NULL = cualquiera)
 * - cliente_trae_producto: Distingue comisión según si el cliente trae su producto
 * - tipo_comision: 'PORCENTAJE' o 'MONTO_FIJO'
 * - prioridad: Reglas con mayor prioridad ganan sobre las genéricas
 * 
 * Ejemplo de migración LEGACY → TARGET:
 * 
 *   LEGACY (hardcoded):
 *     if (tName.equals("Belkis Gutierrez") && sName.equals("Mechas")) return price * 0.36;
 *   
 *   TARGET (base de datos):
 *     INSERT INTO reglas_comision_detalladas
 *       (trabajadora_id, servicio_id, tipo_comision, valor_comision, prioridad)
 *     VALUES (6, id_de_mechas, 'PORCENTAJE', 0.36, 100);
 */
public class ReglaComisionDetallada {

    private int id;
    private Integer trabajadoraId;      // NULL = aplica a todas
    private Integer servicioId;         // NULL = aplica a todos los de la categoría
    private String categoriaServicio;   // NULL = cualquier categoría
    private Boolean clienteTraeProducto; // NULL = no importa, true/false = filtro activo

    /**
     * Tipo de comisión: PORCENTAJE (ej. 0.50 = 50%) o MONTO_FIJO (ej. 8.0 = $8)
     */
    private String tipoComision;        // 'PORCENTAJE' | 'MONTO_FIJO'
    private double valorComision;       // El valor según el tipo

    /**
     * Condición opcional para precio exacto (ej. "Lavado de $8 paga $3 fijos").
     * Si es NULL se ignora. Si tiene valor, la regla SOLO aplica si el precio coincide.
     */
    private Double precioCondicion;

    /**
     * Prioridad de evaluación. Reglas con mayor número se evalúan primero.
     * Permite resolver conflictos cuando múltiples reglas aplican al mismo caso.
     * 
     * Convención:
     *   200+ = Excepciones ultra-específicas (trabajadora + servicio + condición de precio)
     *   100  = Excepciones por trabajadora + servicio
     *    50  = Excepciones por trabajadora + categoría
     *    10  = Reglas generales por categoría
     *     1  = Regla default
     */
    private int prioridad;

    private boolean activo;
    private String descripcion;         // Nota legible: "Belkis - Mechas 36%"

    // Campos transitorios para la UI (no persistidos directamente)
    private String nombreTrabajadora;
    private String nombreServicio;

    public ReglaComisionDetallada() {
        this.activo = true;
        this.prioridad = 10;
        this.tipoComision = "PORCENTAJE";
    }

    // ===== Getters & Setters =====

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public Integer getTrabajadoraId() { return trabajadoraId; }
    public void setTrabajadoraId(Integer trabajadoraId) { this.trabajadoraId = trabajadoraId; }

    public Integer getServicioId() { return servicioId; }
    public void setServicioId(Integer servicioId) { this.servicioId = servicioId; }

    public String getCategoriaServicio() { return categoriaServicio; }
    public void setCategoriaServicio(String categoriaServicio) { this.categoriaServicio = categoriaServicio; }

    public Boolean getClienteTraeProducto() { return clienteTraeProducto; }
    public void setClienteTraeProducto(Boolean clienteTraeProducto) { this.clienteTraeProducto = clienteTraeProducto; }

    public String getTipoComision() { return tipoComision; }
    public void setTipoComision(String tipoComision) { this.tipoComision = tipoComision; }

    public double getValorComision() { return valorComision; }
    public void setValorComision(double valorComision) { this.valorComision = valorComision; }

    public Double getPrecioCondicion() { return precioCondicion; }
    public void setPrecioCondicion(Double precioCondicion) { this.precioCondicion = precioCondicion; }

    public int getPrioridad() { return prioridad; }
    public void setPrioridad(int prioridad) { this.prioridad = prioridad; }

    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public String getNombreTrabajadora() { return nombreTrabajadora; }
    public void setNombreTrabajadora(String nombreTrabajadora) { this.nombreTrabajadora = nombreTrabajadora; }

    public String getNombreServicio() { return nombreServicio; }
    public void setNombreServicio(String nombreServicio) { this.nombreServicio = nombreServicio; }

    /**
     * Calcula la comisión resultante dado un precio de venta.
     * 
     * @param precioVenta el precio del servicio en la venta
     * @return el monto de comisión a pagar
     */
    public double calcularComision(double precioVenta) {
        return switch (tipoComision) {
            case "MONTO_FIJO" -> valorComision;
            case "PORCENTAJE" -> precioVenta * valorComision;
            default -> 0.0;
        };
    }
}
