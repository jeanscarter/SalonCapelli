package app.repository;

import app.exception.DatabaseException;
import app.model.Usuario;

import java.util.List;

/**
 * Interfaz del repositorio de Usuarios del sistema.
 */
public interface UsuarioRepository {

    List<Usuario> findAll() throws DatabaseException;

    Usuario findById(int id) throws DatabaseException;

    Usuario findByUsername(String username) throws DatabaseException;

    void save(Usuario usuario) throws DatabaseException;

    void update(Usuario usuario) throws DatabaseException;

    void updatePassword(int userId, String newPasswordHash) throws DatabaseException;

    void delete(int id) throws DatabaseException;
}
