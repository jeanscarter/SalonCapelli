package app.repository;

import app.exception.DatabaseException;
import app.model.Venta;

import java.time.LocalDate;
import java.util.List;

/**
 * Interfaz del repositorio de Ventas.
 * 
 * Incluye el método transaccional saveCompleteVenta que persiste
 * la venta completa (cabecera + ítems + pagos + propinas) 
 * y actualiza el stock de productos usados, todo en una sola transacción SQL.
 */
public interface VentaRepository {

    /**
     * Guarda una venta completa en una sola transacción SQL atómica.
     * Inserta: Venta → Items → Pagos → Propinas y actualiza stock de productos.
     * 
     * @param venta La venta con sus colecciones hijas pobladas
     * @throws DatabaseException si ocurre un error (hace rollback automático)
     */
    void saveCompleteVenta(Venta venta) throws DatabaseException;

    void delete(int id) throws DatabaseException;

    Venta findById(int id) throws DatabaseException;

    /**
     * Busca ventas en un rango de fechas
     */
    List<Venta> findByRangoFechas(LocalDate inicio, LocalDate fin) throws DatabaseException;

    /**
     * Busca ventas de un cliente específico
     */
    List<Venta> findByClienteId(int clienteId) throws DatabaseException;

    /**
     * Retorna las últimas N ventas (para dashboard)
     */
    List<Venta> findRecientes(int limit) throws DatabaseException;

    int count() throws DatabaseException;

    /**
     * Obtiene el total de ventas para un rango de fechas
     */
    double sumTotalByRangoFechas(LocalDate inicio, LocalDate fin) throws DatabaseException;
}
