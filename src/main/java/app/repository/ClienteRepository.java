package app.repository;

import app.exception.DatabaseException;
import app.exception.cliente.ClienteDuplicadoException;
import app.exception.cliente.ClienteException;
import app.exception.cliente.ClienteNotFoundException;
import app.model.Cliente;

import java.util.List;

/**
 * Interfaz del repositorio de clientes
 * Patrón: Repository + DAO
 * 
 * CAMBIOS:
 * - Excepciones específicas en lugar de 'throws Exception'
 * - Documentación clara de qué excepción se lanza en qué caso
 */
public interface ClienteRepository {
    
    /**
     * Crea un nuevo cliente en la base de datos
     * 
     * @param cliente Cliente a crear
     * @throws ClienteDuplicadoException si ya existe un cliente con la misma cédula
     * @throws DatabaseException si hay un error de base de datos
     * @throws ClienteException para otros errores relacionados con clientes
     */
    void create(Cliente cliente) throws ClienteDuplicadoException, DatabaseException, ClienteException;
    
    /**
     * Actualiza un cliente existente
     * 
     * @param cliente Cliente con los datos actualizados
     * @throws ClienteNotFoundException si no se encuentra el cliente
     * @throws DatabaseException si hay un error de base de datos
     * @throws ClienteException para otros errores relacionados con clientes
     */
    void update(Cliente cliente) throws ClienteNotFoundException, DatabaseException, ClienteException;
    
    /**
     * Elimina un cliente por su ID
     * 
     * @param id ID del cliente a eliminar
     * @throws ClienteNotFoundException si no se encuentra el cliente
     * @throws DatabaseException si hay un error de base de datos
     */
    void delete(int id) throws ClienteNotFoundException, DatabaseException;
    
    /**
     * Obtiene todos los clientes ordenados por nombre
     * 
     * @return Lista de todos los clientes
     * @throws DatabaseException si hay un error de base de datos
     */
    List<Cliente> findAll() throws DatabaseException;
    
    /**
     * Busca un cliente por su cédula
     * 
     * @param cedula Cédula del cliente
     * @return Cliente encontrado o null si no existe
     * @throws DatabaseException si hay un error de base de datos
     */
    Cliente findByCedula(String cedula) throws DatabaseException;
    
    /**
     * Busca un cliente por su ID
     * 
     * @param id ID del cliente
     * @return Cliente encontrado
     * @throws ClienteNotFoundException si no se encuentra el cliente
     * @throws DatabaseException si hay un error de base de datos
     */
    Cliente findById(int id) throws ClienteNotFoundException, DatabaseException;
    
    /**
     * Busca clientes por nombre (búsqueda parcial)
     * 
     * @param nombre Nombre o parte del nombre a buscar
     * @return Lista de clientes que coinciden
     * @throws DatabaseException si hay un error de base de datos
     */
    List<Cliente> searchByNombre(String nombre) throws DatabaseException;
    
    /**
     * Verifica si existe un cliente con la cédula dada
     * 
     * @param cedula Cédula a verificar
     * @return true si existe, false en caso contrario
     * @throws DatabaseException si hay un error de base de datos
     */
    boolean existsByCedula(String cedula) throws DatabaseException;
    
    /**
     * Cuenta el total de clientes en la base de datos
     * 
     * @return Número total de clientes
     * @throws DatabaseException si hay un error de base de datos
     */
    int count() throws DatabaseException;
}