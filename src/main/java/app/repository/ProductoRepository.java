package app.repository;

import app.exception.DatabaseException;
import app.model.Producto;

import java.util.List;

/**
 * Interfaz del repositorio de Productos
 * Patrón: Repository + DAO
 */
public interface ProductoRepository {

    void create(Producto producto) throws DatabaseException;

    void update(Producto producto) throws DatabaseException;

    void delete(int id) throws DatabaseException;

    List<Producto> findAll() throws DatabaseException;

    List<Producto> findAllActivos() throws DatabaseException;

    Producto findById(int id) throws DatabaseException;

    List<Producto> findByMarca(int marcaId) throws DatabaseException;

    List<Producto> searchByNombre(String nombre) throws DatabaseException;

    /**
     * Actualiza el stock del producto sumando la cantidad indicada.
     * Usa valores negativos para salidas de stock.
     * 
     * @param productoId ID del producto
     * @param cantidad   Cantidad a sumar (positivo = entrada, negativo = salida)
     */
    void actualizarStock(int productoId, int cantidad) throws DatabaseException;

    /**
     * Retorna productos con stock por debajo del mínimo
     */
    List<Producto> findStockBajo() throws DatabaseException;

    int count() throws DatabaseException;
}
