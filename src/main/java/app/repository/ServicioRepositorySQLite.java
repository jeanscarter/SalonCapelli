package app.repository;

import app.db.DatabaseConnection;
import app.exception.DatabaseException;
import app.exception.servicio.ServicioException;
import app.exception.servicio.ServicioNotFoundException;
import app.model.CategoriaServicio;
import app.model.Servicio;
import app.model.TipoCabello;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServicioRepositorySQLite implements ServicioRepository {

    private static final Logger logger = LoggerFactory.getLogger(ServicioRepositorySQLite.class);

    private static final String SQL_CREATE = """
        INSERT INTO servicios (nombre, categoria, precio_corto, precio_mediano, precio_largo,
        precio_extensiones, permite_cliente_producto, precio_cliente_producto)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?)
    """;

    private static final String SQL_UPDATE = """
        UPDATE servicios SET nombre=?, categoria=?, precio_corto=?, precio_mediano=?,
        precio_largo=?, precio_extensiones=?, permite_cliente_producto=?,
        precio_cliente_producto=?, is_active=1
        WHERE id=?
    """;

    private static final String SQL_SOFT_DELETE = "UPDATE servicios SET is_active = 0 WHERE id = ?";
    private static final String SQL_FIND_ALL = "SELECT * FROM servicios WHERE is_active = 1 ORDER BY nombre";
    private static final String SQL_FIND_BY_ID = "SELECT * FROM servicios WHERE id = ?";
    private static final String SQL_SEARCH = "SELECT * FROM servicios WHERE is_active = 1 AND nombre LIKE ? ORDER BY nombre LIMIT 100";
    private static final String SQL_COUNT = "SELECT COUNT(*) FROM servicios WHERE is_active = 1";

    @Override
    public void create(Servicio s) throws DatabaseException, ServicioException {
        logger.info("Creando servicio: {}", s.getNombre());
        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(SQL_CREATE, Statement.RETURN_GENERATED_KEYS)) {
            mapServicioToStmt(s, pstmt);
            pstmt.executeUpdate();
            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) s.setId(rs.getInt(1));
            }
            logger.info("✓ Servicio creado con ID: {}", s.getId());
        } catch (SQLException e) {
            logger.error("Error SQL al crear servicio: {}", e.getMessage(), e);
            throw DatabaseException.queryFailed("CREATE SERVICIO", e);
        }
    }

    @Override
    public void update(Servicio s) throws ServicioNotFoundException, DatabaseException, ServicioException {
        logger.info("Actualizando servicio ID: {}", s.getId());
        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(SQL_UPDATE)) {
            mapServicioToStmt(s, pstmt);
            pstmt.setInt(9, s.getId());
            int affected = pstmt.executeUpdate();
            if (affected == 0) throw ServicioNotFoundException.byId(s.getId());
            logger.info("✓ Servicio actualizado: {}", s.getNombre());
        } catch (SQLException e) {
            logger.error("Error SQL al actualizar servicio: {}", e.getMessage(), e);
            throw DatabaseException.queryFailed("UPDATE SERVICIO", e);
        }
    }

    @Override
    public void delete(int id) throws ServicioNotFoundException, DatabaseException {
        logger.info("Desactivando servicio ID: {}", id);
        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(SQL_SOFT_DELETE)) {
            pstmt.setInt(1, id);
            int affected = pstmt.executeUpdate();
            if (affected == 0) throw ServicioNotFoundException.byId(id);
            logger.info("✓ Servicio desactivado: ID {}", id);
        } catch (SQLException e) {
            throw DatabaseException.queryFailed("DELETE SERVICIO", e);
        }
    }

    @Override
    public List<Servicio> findAll() throws DatabaseException {
        List<Servicio> lista = new ArrayList<>();
        try (Connection conn = DatabaseConnection.connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(SQL_FIND_ALL)) {
            while (rs.next()) lista.add(mapResultSetToServicio(rs));
            logger.info("✓ Se obtuvieron {} servicios", lista.size());
            return lista;
        } catch (SQLException e) {
            throw DatabaseException.queryFailed("SELECT ALL SERVICIOS", e);
        }
    }

    @Override
    public Servicio findById(int id) throws ServicioNotFoundException, DatabaseException {
        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(SQL_FIND_BY_ID)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return mapResultSetToServicio(rs);
            }
            throw ServicioNotFoundException.byId(id);
        } catch (SQLException e) {
            throw DatabaseException.queryFailed("FIND BY ID", e);
        }
    }

    @Override
    public List<Servicio> searchByNombre(String nombre) throws DatabaseException {
        List<Servicio> lista = new ArrayList<>();
        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(SQL_SEARCH)) {
            pstmt.setString(1, "%" + nombre + "%");
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) lista.add(mapResultSetToServicio(rs));
            }
            return lista;
        } catch (SQLException e) {
            throw DatabaseException.queryFailed("SEARCH SERVICIOS", e);
        }
    }

    @Override
    public int count() throws DatabaseException {
        try (Connection conn = DatabaseConnection.connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(SQL_COUNT)) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            throw DatabaseException.queryFailed("COUNT SERVICIOS", e);
        }
    }

    private void mapServicioToStmt(Servicio s, PreparedStatement pstmt) throws SQLException {
        pstmt.setString(1, s.getNombre());
        pstmt.setString(2, s.getCategoria() != null ? s.getCategoria().name() : null);
        pstmt.setDouble(3, s.getPrecioCorto());
        pstmt.setDouble(4, s.getPrecioMediano());
        pstmt.setDouble(5, s.getPrecioLargo());
        pstmt.setDouble(6, s.getPrecioExtensiones());
        pstmt.setBoolean(7, s.isPermiteClienteProducto());
        pstmt.setDouble(8, s.getPrecioClienteProducto());
    }

    private Servicio mapResultSetToServicio(ResultSet rs) throws SQLException {
        Servicio s = new Servicio();
        s.setId(rs.getInt("id"));
        s.setNombre(rs.getString("nombre"));
        String cat = rs.getString("categoria");
        if (cat != null) {
            try { s.setCategoria(CategoriaServicio.valueOf(cat)); } catch (IllegalArgumentException ignored) {}
        }
        s.setPrecioCorto(rs.getDouble("precio_corto"));
        s.setPrecioMediano(rs.getDouble("precio_mediano"));
        s.setPrecioLargo(rs.getDouble("precio_largo"));
        s.setPrecioExtensiones(rs.getDouble("precio_extensiones"));
        s.setPermiteClienteProducto(rs.getBoolean("permite_cliente_producto"));
        s.setPrecioClienteProducto(rs.getDouble("precio_cliente_producto"));
        s.setActivo(rs.getBoolean("is_active"));
        return s;
    }
}
