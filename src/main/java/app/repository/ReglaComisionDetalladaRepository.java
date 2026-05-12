package app.repository;

import app.exception.DatabaseException;
import app.model.ReglaComisionDetallada;

import java.util.List;

/**
 * Interfaz del repositorio de reglas de comisión detalladas.
 * Reemplaza la lógica hardcoded de PayrollService.calculateCommissionForItem().
 */
public interface ReglaComisionDetalladaRepository {

    void create(ReglaComisionDetallada regla) throws DatabaseException;

    void update(ReglaComisionDetallada regla) throws DatabaseException;

    void delete(int id) throws DatabaseException;

    List<ReglaComisionDetallada> findAll() throws DatabaseException;

    ReglaComisionDetallada findById(int id) throws DatabaseException;

    /**
     * Busca reglas de comisión por trabajadora
     */
    List<ReglaComisionDetallada> findByTrabajadora(int trabajadoraId) throws DatabaseException;

    /**
     * Busca la regla de comisión más específica (mayor prioridad) que aplique
     * a los parámetros dados. Retorna null si no hay regla aplicable.
     * 
     * Este método reemplaza TODA la lógica if/else del LEGACY PayrollService.
     * 
     * @param trabajadoraId       ID de la trabajadora
     * @param servicioId          ID del servicio
     * @param categoriaServicio   Categoría del servicio
     * @param clienteTraeProducto Si el cliente trae su propio producto
     * @param precioVenta         Precio de venta (para condiciones por precio)
     * @return La regla aplicable con mayor prioridad, o null
     */
    ReglaComisionDetallada findReglaMasEspecifica(
            int trabajadoraId,
            int servicioId,
            String categoriaServicio,
            boolean clienteTraeProducto,
            double precioVenta
    ) throws DatabaseException;
}
