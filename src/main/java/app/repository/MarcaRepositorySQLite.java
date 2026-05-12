package app.repository;

import app.db.DatabaseConnection;
import app.exception.DatabaseException;
import app.model.Marca;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementación SQLite del repositorio de Marcas
 */
public class MarcaRepositorySQLite implements MarcaRepository {

    private static final Logger logger = LoggerFactory.getLogger(MarcaRepositorySQLite.class);

    private static final String SQL_CREATE =
        "INSERT INTO marcas (nombre, ruta_imagen, descripcion, activa) VALUES (?, ?, ?, ?)";

    private static final String SQL_UPDATE =
        "UPDATE marcas SET nombre=?, ruta_imagen=?, descripcion=?, activa=? WHERE id=?";

    private static final String SQL_DELETE =
        "DELETE FROM marcas WHERE id=?";

    private static final String SQL_FIND_ALL =
        "SELECT * FROM marcas ORDER BY nombre";

    private static final String SQL_FIND_ALL_ACTIVAS =
        "SELECT * FROM marcas WHERE activa=1 ORDER BY nombre";

    private static final String SQL_FIND_BY_ID =
        "SELECT * FROM marcas WHERE id=?";

    private static final String SQL_FIND_BY_NOMBRE =
        "SELECT * FROM marcas WHERE nombre=?";

    @Override
    public void create(Marca marca) throws DatabaseException {
        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(SQL_CREATE, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, marca.getNombre());
            pstmt.setString(2, marca.getRutaImagen());
            pstmt.setString(3, marca.getDescripcion());
            pstmt.setInt(4, marca.isActiva() ? 1 : 0);

            pstmt.executeUpdate();

            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    marca.setId(rs.getInt(1));
                }
            }
            logger.debug("Marca creada: {} (ID={})", marca.getNombre(), marca.getId());
        } catch (SQLException e) {
            if (e.getMessage() != null && e.getMessage().contains("UNIQUE constraint failed")) {
                throw new DatabaseException("Ya existe una marca con el nombre: " + marca.getNombre(), e);
            }
            throw DatabaseException.queryFailed("CREATE MARCA", e);
        }
    }

    @Override
    public void update(Marca marca) throws DatabaseException {
        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(SQL_UPDATE)) {

            pstmt.setString(1, marca.getNombre());
            pstmt.setString(2, marca.getRutaImagen());
            pstmt.setString(3, marca.getDescripcion());
            pstmt.setInt(4, marca.isActiva() ? 1 : 0);
            pstmt.setInt(5, marca.getId());

            pstmt.executeUpdate();
            logger.debug("Marca actualizada: {} (ID={})", marca.getNombre(), marca.getId());
        } catch (SQLException e) {
            throw DatabaseException.queryFailed("UPDATE MARCA", e);
        }
    }

    @Override
    public void delete(int id) throws DatabaseException {
        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(SQL_DELETE)) {

            pstmt.setInt(1, id);
            pstmt.executeUpdate();
            logger.debug("Marca eliminada (ID={})", id);
        } catch (SQLException e) {
            throw DatabaseException.queryFailed("DELETE MARCA", e);
        }
    }

    @Override
    public List<Marca> findAll() throws DatabaseException {
        List<Marca> lista = new ArrayList<>();
        try (Connection conn = DatabaseConnection.connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(SQL_FIND_ALL)) {

            while (rs.next()) {
                lista.add(mapResultSetToMarca(rs));
            }
        } catch (SQLException e) {
            throw DatabaseException.queryFailed("FIND_ALL MARCAS", e);
        }
        return lista;
    }

    @Override
    public List<Marca> findAllActivas() throws DatabaseException {
        List<Marca> lista = new ArrayList<>();
        try (Connection conn = DatabaseConnection.connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(SQL_FIND_ALL_ACTIVAS)) {

            while (rs.next()) {
                lista.add(mapResultSetToMarca(rs));
            }
        } catch (SQLException e) {
            throw DatabaseException.queryFailed("FIND_ALL_ACTIVAS MARCAS", e);
        }
        return lista;
    }

    @Override
    public Marca findById(int id) throws DatabaseException {
        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(SQL_FIND_BY_ID)) {

            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToMarca(rs);
                }
            }
        } catch (SQLException e) {
            throw DatabaseException.queryFailed("FIND_BY_ID MARCA", e);
        }
        return null;
    }

    @Override
    public Marca findByNombre(String nombre) throws DatabaseException {
        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(SQL_FIND_BY_NOMBRE)) {

            pstmt.setString(1, nombre);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToMarca(rs);
                }
            }
        } catch (SQLException e) {
            throw DatabaseException.queryFailed("FIND_BY_NOMBRE MARCA", e);
        }
        return null;
    }

    private Marca mapResultSetToMarca(ResultSet rs) throws SQLException {
        Marca marca = new Marca();
        marca.setId(rs.getInt("id"));
        marca.setNombre(rs.getString("nombre"));
        marca.setRutaImagen(rs.getString("ruta_imagen"));
        marca.setDescripcion(rs.getString("descripcion"));
        marca.setActiva(rs.getInt("activa") == 1);
        return marca;
    }
}
