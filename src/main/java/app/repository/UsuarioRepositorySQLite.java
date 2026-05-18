package app.repository;

import app.db.DatabaseConnection;
import app.exception.DatabaseException;
import app.model.Usuario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementación SQLite del repositorio de Usuarios.
 */
public class UsuarioRepositorySQLite implements UsuarioRepository {

    private static final Logger logger = LoggerFactory.getLogger(UsuarioRepositorySQLite.class);

    private static final String SQL_FIND_ALL =
        "SELECT * FROM usuarios ORDER BY username";

    private static final String SQL_FIND_BY_ID =
        "SELECT * FROM usuarios WHERE id = ?";

    private static final String SQL_FIND_BY_USERNAME =
        "SELECT * FROM usuarios WHERE username = ?";

    private static final String SQL_INSERT =
        "INSERT INTO usuarios (username, password_hash, rol, activo) VALUES (?, ?, ?, ?)";

    private static final String SQL_UPDATE =
        "UPDATE usuarios SET username = ?, rol = ?, activo = ? WHERE id = ?";

    private static final String SQL_UPDATE_PASSWORD =
        "UPDATE usuarios SET password_hash = ? WHERE id = ?";

    private static final String SQL_DELETE =
        "DELETE FROM usuarios WHERE id = ?";

    @Override
    public List<Usuario> findAll() throws DatabaseException {
        List<Usuario> lista = new ArrayList<>();
        try (Connection conn = DatabaseConnection.connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(SQL_FIND_ALL)) {

            while (rs.next()) {
                lista.add(mapRow(rs));
            }
        } catch (SQLException e) {
            logger.error("Error al listar usuarios", e);
            throw DatabaseException.queryFailed("FIND_ALL_USUARIOS", e);
        }
        return lista;
    }

    @Override
    public Usuario findById(int id) throws DatabaseException {
        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(SQL_FIND_BY_ID)) {

            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        } catch (SQLException e) {
            logger.error("Error al buscar usuario ID={}", id, e);
            throw DatabaseException.queryFailed("FIND_BY_ID_USUARIO", e);
        }
        return null;
    }

    @Override
    public Usuario findByUsername(String username) throws DatabaseException {
        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(SQL_FIND_BY_USERNAME)) {

            pstmt.setString(1, username);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        } catch (SQLException e) {
            logger.error("Error al buscar usuario: {}", username, e);
            throw DatabaseException.queryFailed("FIND_BY_USERNAME", e);
        }
        return null;
    }

    @Override
    public void save(Usuario usuario) throws DatabaseException {
        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, usuario.getUsername());
            pstmt.setString(2, usuario.getPasswordHash());
            pstmt.setString(3, usuario.getRol());
            pstmt.setInt(4, usuario.isActivo() ? 1 : 0);
            pstmt.executeUpdate();

            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    usuario.setId(rs.getInt(1));
                }
            }
            logger.info("Usuario creado: {} (ID={})", usuario.getUsername(), usuario.getId());
        } catch (SQLException e) {
            logger.error("Error al guardar usuario", e);
            throw DatabaseException.queryFailed("SAVE_USUARIO", e);
        }
    }

    @Override
    public void update(Usuario usuario) throws DatabaseException {
        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(SQL_UPDATE)) {

            pstmt.setString(1, usuario.getUsername());
            pstmt.setString(2, usuario.getRol());
            pstmt.setInt(3, usuario.isActivo() ? 1 : 0);
            pstmt.setInt(4, usuario.getId());
            pstmt.executeUpdate();

            logger.info("Usuario actualizado: ID={}", usuario.getId());
        } catch (SQLException e) {
            logger.error("Error al actualizar usuario", e);
            throw DatabaseException.queryFailed("UPDATE_USUARIO", e);
        }
    }

    @Override
    public void updatePassword(int userId, String newPasswordHash) throws DatabaseException {
        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(SQL_UPDATE_PASSWORD)) {

            pstmt.setString(1, newPasswordHash);
            pstmt.setInt(2, userId);
            pstmt.executeUpdate();

            logger.info("Contraseña actualizada para usuario ID={}", userId);
        } catch (SQLException e) {
            logger.error("Error al actualizar contraseña", e);
            throw DatabaseException.queryFailed("UPDATE_PASSWORD", e);
        }
    }

    @Override
    public void delete(int id) throws DatabaseException {
        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(SQL_DELETE)) {

            pstmt.setInt(1, id);
            pstmt.executeUpdate();
            logger.info("Usuario eliminado: ID={}", id);
        } catch (SQLException e) {
            logger.error("Error al eliminar usuario", e);
            throw DatabaseException.queryFailed("DELETE_USUARIO", e);
        }
    }

    private Usuario mapRow(ResultSet rs) throws SQLException {
        Usuario u = new Usuario();
        u.setId(rs.getInt("id"));
        u.setUsername(rs.getString("username"));
        u.setPasswordHash(rs.getString("password_hash"));
        u.setRol(rs.getString("rol"));
        u.setActivo(rs.getInt("activo") == 1);

        String dateStr = rs.getString("fecha_creacion");
        if (dateStr != null) {
            try {
                u.setFechaCreacion(LocalDateTime.parse(dateStr.replace(" ", "T")));
            } catch (Exception ignored) {
                // Fallback if format is unexpected
            }
        }

        return u;
    }
}
