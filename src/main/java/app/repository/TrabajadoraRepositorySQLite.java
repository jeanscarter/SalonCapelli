package app.repository;

import app.db.DatabaseConnection;
import app.exception.DatabaseException;
import app.exception.trabajadora.TrabajadoraDuplicadaException;
import app.exception.trabajadora.TrabajadoraException;
import app.exception.trabajadora.TrabajadoraNotFoundException;
import app.model.CuentaBancaria;
import app.model.Trabajadora;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementación SQLite del repositorio de trabajadoras
 * Patrón: Repository + Template Method
 */
public class TrabajadoraRepositorySQLite implements TrabajadoraRepository {

    private static final Logger logger = LoggerFactory.getLogger(TrabajadoraRepositorySQLite.class);

    // ===== SQL Constants =====

    private static final String SQL_CREATE = """
        INSERT INTO trabajadoras (cedula, nombres, apellidos, telefono, correo, foto,
        bono_activo, monto_bono, razon_bono)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
    """;

    private static final String SQL_UPDATE = """
        UPDATE trabajadoras SET cedula=?, nombres=?, apellidos=?, telefono=?, correo=?, foto=?,
        bono_activo=?, monto_bono=?, razon_bono=?
        WHERE id=?
    """;

    private static final String SQL_DELETE = "DELETE FROM trabajadoras WHERE id = ?";

    private static final String SQL_FIND_ALL =
        "SELECT * FROM trabajadoras ORDER BY nombres, apellidos";

    private static final String SQL_FIND_BY_CEDULA =
        "SELECT * FROM trabajadoras WHERE cedula = ?";

    private static final String SQL_FIND_BY_ID =
        "SELECT * FROM trabajadoras WHERE id = ?";

    private static final String SQL_SEARCH_BY_NOMBRE =
        "SELECT * FROM trabajadoras WHERE (nombres || ' ' || apellidos) LIKE ? ORDER BY nombres LIMIT 100";

    private static final String SQL_EXISTS_BY_CEDULA =
        "SELECT COUNT(*) FROM trabajadoras WHERE cedula = ?";

    private static final String SQL_COUNT =
        "SELECT COUNT(*) FROM trabajadoras";

    // ===== Cuentas bancarias =====

    private static final String SQL_DELETE_CUENTAS =
        "DELETE FROM cuentas_bancarias WHERE trabajadora_id = ?";

    private static final String SQL_INSERT_CUENTA =
        "INSERT INTO cuentas_bancarias (trabajadora_id, banco, tipo_cuenta, numero_cuenta, es_principal) VALUES (?, ?, ?, ?, ?)";

    private static final String SQL_FIND_CUENTAS =
        "SELECT * FROM cuentas_bancarias WHERE trabajadora_id = ?";

    // ===== CRUD =====

    @Override
    public void create(Trabajadora t) throws TrabajadoraDuplicadaException, DatabaseException, TrabajadoraException {
        logger.info("Intentando crear trabajadora: {}", t.getCedula());

        if (existsByCedula(t.getCedula())) {
            logger.warn("Intento de crear trabajadora duplicada: {}", t.getCedula());
            throw new TrabajadoraDuplicadaException(t.getCedula());
        }

        Connection conn = null;
        try {
            conn = DatabaseConnection.connect();
            conn.setAutoCommit(false);

            try (PreparedStatement pstmt = conn.prepareStatement(SQL_CREATE, Statement.RETURN_GENERATED_KEYS)) {
                mapTrabajadoraToStmt(t, pstmt);
                pstmt.executeUpdate();

                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        t.setId(rs.getInt(1));
                    }
                }
            }

            saveCuentas(conn, t);
            conn.commit();
            logger.info("✓ Trabajadora creada con ID: {} - {}", t.getId(), t.getNombreCompleto());

        } catch (SQLException e) {
            rollback(conn);
            logger.error("Error SQL al crear trabajadora: {}", e.getMessage(), e);

            if (e.getMessage().contains("UNIQUE constraint failed")) {
                throw new TrabajadoraDuplicadaException(t.getCedula());
            }
            throw DatabaseException.queryFailed("CREATE TRABAJADORA", e);
        } finally {
            restoreAutoCommit(conn);
        }
    }

    @Override
    public void update(Trabajadora t) throws TrabajadoraNotFoundException, DatabaseException, TrabajadoraException {
        logger.info("Actualizando trabajadora ID: {} - Cédula: {}", t.getId(), t.getCedula());

        if (!existsById(t.getId())) {
            throw TrabajadoraNotFoundException.byId(t.getId());
        }

        Connection conn = null;
        try {
            conn = DatabaseConnection.connect();
            conn.setAutoCommit(false);

            try (PreparedStatement pstmt = conn.prepareStatement(SQL_UPDATE)) {
                mapTrabajadoraToStmt(t, pstmt);
                pstmt.setInt(10, t.getId());

                int affected = pstmt.executeUpdate();
                if (affected == 0) {
                    throw TrabajadoraNotFoundException.byId(t.getId());
                }
            }

            saveCuentas(conn, t);
            conn.commit();
            logger.info("✓ Trabajadora actualizada: ID {} - {}", t.getId(), t.getNombreCompleto());

        } catch (SQLException e) {
            rollback(conn);
            logger.error("Error SQL al actualizar trabajadora: {}", e.getMessage(), e);
            throw DatabaseException.queryFailed("UPDATE TRABAJADORA", e);
        } finally {
            restoreAutoCommit(conn);
        }
    }

    @Override
    public void delete(int id) throws TrabajadoraNotFoundException, DatabaseException {
        logger.info("Eliminando trabajadora ID: {}", id);

        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(SQL_DELETE)) {

            pstmt.setInt(1, id);
            int affected = pstmt.executeUpdate();

            if (affected == 0) {
                throw TrabajadoraNotFoundException.byId(id);
            }

            logger.info("✓ Trabajadora eliminada: ID {}", id);

        } catch (SQLException e) {
            logger.error("Error SQL al eliminar trabajadora: {}", e.getMessage(), e);
            throw DatabaseException.queryFailed("DELETE TRABAJADORA", e);
        }
    }

    @Override
    public List<Trabajadora> findAll() throws DatabaseException {
        logger.debug("Obteniendo todas las trabajadoras");

        List<Trabajadora> lista = new ArrayList<>();

        try (Connection conn = DatabaseConnection.connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(SQL_FIND_ALL)) {

            while (rs.next()) {
                Trabajadora t = mapResultSetToTrabajadora(rs);
                t.setCuentas(findCuentas(conn, t.getId()));
                lista.add(t);
            }

            logger.info("✓ Se obtuvieron {} trabajadoras", lista.size());
            return lista;

        } catch (SQLException e) {
            logger.error("Error SQL al obtener trabajadoras", e);
            throw DatabaseException.queryFailed("SELECT ALL TRABAJADORAS", e);
        }
    }

    @Override
    public Trabajadora findByCedula(String cedula) throws DatabaseException {
        logger.debug("Buscando trabajadora por cédula: {}", cedula);

        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(SQL_FIND_BY_CEDULA)) {

            pstmt.setString(1, cedula);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Trabajadora t = mapResultSetToTrabajadora(rs);
                    t.setCuentas(findCuentas(conn, t.getId()));
                    return t;
                }
            }

            return null;

        } catch (SQLException e) {
            logger.error("Error SQL al buscar por cédula: {}", cedula, e);
            throw DatabaseException.queryFailed("FIND BY CEDULA", e);
        }
    }

    @Override
    public Trabajadora findById(int id) throws TrabajadoraNotFoundException, DatabaseException {
        logger.debug("Buscando trabajadora por ID: {}", id);

        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(SQL_FIND_BY_ID)) {

            pstmt.setInt(1, id);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Trabajadora t = mapResultSetToTrabajadora(rs);
                    t.setCuentas(findCuentas(conn, t.getId()));
                    return t;
                }
            }

            throw TrabajadoraNotFoundException.byId(id);

        } catch (SQLException e) {
            logger.error("Error SQL al buscar por ID: {}", id, e);
            throw DatabaseException.queryFailed("FIND BY ID", e);
        }
    }

    @Override
    public List<Trabajadora> searchByNombre(String nombre) throws DatabaseException {
        logger.debug("Buscando trabajadoras por nombre: {}", nombre);

        List<Trabajadora> lista = new ArrayList<>();

        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(SQL_SEARCH_BY_NOMBRE)) {

            pstmt.setString(1, "%" + nombre + "%");

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Trabajadora t = mapResultSetToTrabajadora(rs);
                    t.setCuentas(findCuentas(conn, t.getId()));
                    lista.add(t);
                }
            }

            return lista;

        } catch (SQLException e) {
            logger.error("Error SQL al buscar por nombre: {}", nombre, e);
            throw DatabaseException.queryFailed("SEARCH BY NOMBRE", e);
        }
    }

    @Override
    public boolean existsByCedula(String cedula) throws DatabaseException {
        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(SQL_EXISTS_BY_CEDULA)) {

            pstmt.setString(1, cedula);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
            return false;

        } catch (SQLException e) {
            throw DatabaseException.queryFailed("EXISTS BY CEDULA", e);
        }
    }

    @Override
    public int count() throws DatabaseException {
        try (Connection conn = DatabaseConnection.connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(SQL_COUNT)) {

            if (rs.next()) {
                return rs.getInt(1);
            }
            return 0;

        } catch (SQLException e) {
            throw DatabaseException.queryFailed("COUNT TRABAJADORAS", e);
        }
    }

    // ===== Helpers =====

    private void mapTrabajadoraToStmt(Trabajadora t, PreparedStatement pstmt) throws SQLException {
        pstmt.setString(1, t.getCedula());
        pstmt.setString(2, t.getNombres());
        pstmt.setString(3, t.getApellidos());
        pstmt.setString(4, t.getTelefono());
        pstmt.setString(5, t.getCorreoElectronico());
        if (t.getFoto() != null) {
            pstmt.setBytes(6, t.getFoto());
        } else {
            pstmt.setNull(6, Types.BLOB);
        }
        pstmt.setBoolean(7, t.isBonoActivo());
        pstmt.setDouble(8, t.getMontoBono());
        pstmt.setString(9, t.getRazonBono());
    }

    private Trabajadora mapResultSetToTrabajadora(ResultSet rs) throws SQLException {
        Trabajadora t = new Trabajadora();
        t.setId(rs.getInt("id"));
        t.setCedula(rs.getString("cedula"));
        t.setNombres(rs.getString("nombres"));
        t.setApellidos(rs.getString("apellidos"));
        t.setTelefono(rs.getString("telefono"));
        t.setCorreoElectronico(rs.getString("correo"));
        t.setFoto(rs.getBytes("foto"));
        t.setBonoActivo(rs.getBoolean("bono_activo"));
        t.setMontoBono(rs.getDouble("monto_bono"));
        t.setRazonBono(rs.getString("razon_bono"));
        return t;
    }

    private void saveCuentas(Connection conn, Trabajadora t) throws SQLException {
        // Eliminar cuentas existentes
        try (PreparedStatement del = conn.prepareStatement(SQL_DELETE_CUENTAS)) {
            del.setInt(1, t.getId());
            del.executeUpdate();
        }

        // Insertar nuevas
        if (t.getCuentas() != null && !t.getCuentas().isEmpty()) {
            try (PreparedStatement ins = conn.prepareStatement(SQL_INSERT_CUENTA)) {
                for (CuentaBancaria c : t.getCuentas()) {
                    ins.setInt(1, t.getId());
                    ins.setString(2, c.getBanco());
                    ins.setString(3, c.getTipoDeCuenta());
                    ins.setString(4, c.getNumeroDeCuenta());
                    ins.setBoolean(5, c.isEsPrincipal());
                    ins.addBatch();
                }
                ins.executeBatch();
            }
        }
    }

    private List<CuentaBancaria> findCuentas(Connection conn, int trabajadoraId) throws SQLException {
        List<CuentaBancaria> cuentas = new ArrayList<>();
        try (PreparedStatement pstmt = conn.prepareStatement(SQL_FIND_CUENTAS)) {
            pstmt.setInt(1, trabajadoraId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    CuentaBancaria c = new CuentaBancaria(
                        rs.getString("banco"),
                        rs.getString("tipo_cuenta"),
                        rs.getString("numero_cuenta"),
                        rs.getBoolean("es_principal")
                    );
                    c.setId(rs.getInt("id"));
                    cuentas.add(c);
                }
            }
        }
        return cuentas;
    }

    private boolean existsById(int id) throws DatabaseException {
        try {
            findById(id);
            return true;
        } catch (TrabajadoraNotFoundException e) {
            return false;
        }
    }

    private void rollback(Connection conn) {
        if (conn != null) {
            try { conn.rollback(); } catch (SQLException ex) {
                logger.error("Error haciendo rollback: {}", ex.getMessage());
            }
        }
    }

    private void restoreAutoCommit(Connection conn) {
        if (conn != null) {
            try { conn.setAutoCommit(true); } catch (SQLException ex) {
                logger.error("Error restaurando autoCommit: {}", ex.getMessage());
            }
        }
    }
}
