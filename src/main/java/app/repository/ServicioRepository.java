package app.repository;

import app.exception.DatabaseException;
import app.exception.servicio.ServicioException;
import app.exception.servicio.ServicioNotFoundException;
import app.model.Servicio;

import java.util.List;

/**
 * Interfaz del repositorio de servicios
 * Patrón: Repository + DAO
 */
public interface ServicioRepository {

    void create(Servicio servicio) throws DatabaseException, ServicioException;

    void update(Servicio servicio) throws ServicioNotFoundException, DatabaseException, ServicioException;

    void delete(int id) throws ServicioNotFoundException, DatabaseException;

    List<Servicio> findAll() throws DatabaseException;

    Servicio findById(int id) throws ServicioNotFoundException, DatabaseException;

    List<Servicio> searchByNombre(String nombre) throws DatabaseException;

    int count() throws DatabaseException;
}
