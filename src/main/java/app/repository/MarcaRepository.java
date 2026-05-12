package app.repository;

import app.exception.DatabaseException;
import app.model.Marca;

import java.util.List;

/**
 * Interfaz del repositorio de Marcas
 * Patrón: Repository + DAO
 */
public interface MarcaRepository {

    void create(Marca marca) throws DatabaseException;

    void update(Marca marca) throws DatabaseException;

    void delete(int id) throws DatabaseException;

    List<Marca> findAll() throws DatabaseException;

    List<Marca> findAllActivas() throws DatabaseException;

    Marca findById(int id) throws DatabaseException;

    Marca findByNombre(String nombre) throws DatabaseException;
}
