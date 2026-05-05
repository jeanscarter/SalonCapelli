package app.repository;

import app.exception.DatabaseException;
import app.exception.trabajadora.TrabajadoraDuplicadaException;
import app.exception.trabajadora.TrabajadoraException;
import app.exception.trabajadora.TrabajadoraNotFoundException;
import app.model.Trabajadora;

import java.util.List;

/**
 * Interfaz del repositorio de trabajadoras
 * Patrón: Repository + DAO
 */
public interface TrabajadoraRepository {

    /**
     * Crea una nueva trabajadora (incluye cuentas bancarias)
     *
     * @param trabajadora Trabajadora a crear
     * @throws TrabajadoraDuplicadaException si ya existe una con la misma cédula
     * @throws DatabaseException si hay un error de base de datos
     */
    void create(Trabajadora trabajadora) throws TrabajadoraDuplicadaException, DatabaseException, TrabajadoraException;

    /**
     * Actualiza una trabajadora existente (reemplaza cuentas bancarias)
     *
     * @param trabajadora Trabajadora con los datos actualizados
     * @throws TrabajadoraNotFoundException si no se encuentra
     * @throws DatabaseException si hay un error de base de datos
     */
    void update(Trabajadora trabajadora) throws TrabajadoraNotFoundException, DatabaseException, TrabajadoraException;

    /**
     * Elimina una trabajadora por su ID
     *
     * @param id ID de la trabajadora a eliminar
     * @throws TrabajadoraNotFoundException si no se encuentra
     * @throws DatabaseException si hay un error de base de datos
     */
    void delete(int id) throws TrabajadoraNotFoundException, DatabaseException;

    /**
     * Obtiene todas las trabajadoras ordenadas por nombre
     *
     * @return Lista de todas las trabajadoras con sus cuentas
     * @throws DatabaseException si hay un error de base de datos
     */
    List<Trabajadora> findAll() throws DatabaseException;

    /**
     * Busca una trabajadora por su cédula
     *
     * @param cedula Cédula de la trabajadora
     * @return Trabajadora encontrada o null
     * @throws DatabaseException si hay un error de base de datos
     */
    Trabajadora findByCedula(String cedula) throws DatabaseException;

    /**
     * Busca una trabajadora por su ID
     *
     * @param id ID de la trabajadora
     * @return Trabajadora encontrada
     * @throws TrabajadoraNotFoundException si no se encuentra
     * @throws DatabaseException si hay un error de base de datos
     */
    Trabajadora findById(int id) throws TrabajadoraNotFoundException, DatabaseException;

    /**
     * Busca trabajadoras por nombre (búsqueda parcial)
     *
     * @param nombre Nombre o parte del nombre a buscar
     * @return Lista de trabajadoras que coinciden
     * @throws DatabaseException si hay un error de base de datos
     */
    List<Trabajadora> searchByNombre(String nombre) throws DatabaseException;

    /**
     * Verifica si existe una trabajadora con la cédula dada
     *
     * @param cedula Cédula a verificar
     * @return true si existe
     * @throws DatabaseException si hay un error de base de datos
     */
    boolean existsByCedula(String cedula) throws DatabaseException;

    /**
     * Cuenta el total de trabajadoras
     *
     * @return Número total
     * @throws DatabaseException si hay un error de base de datos
     */
    int count() throws DatabaseException;
}
