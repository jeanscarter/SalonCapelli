package app.repository;

import app.db.DatabaseConnection;
import app.exception.DatabaseException;
import app.model.CuentaReceptora;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementación SQLite del repositorio de Cuentas Receptoras.
 */
public class CuentaReceptoraRepositorySQLite implements CuentaReceptoraRepository {

    private static final Logger logger = LoggerFactory.getLogger(CuentaReceptoraRepositorySQLite.class);

    private static final String SQL_FIND_ALL =
        "SELECT * FROM cuentas_receptoras ORDER BY nombre_cuenta, alias_referencia";

    private static final String SQL_FIND_ACTIVAS =
        "SELECT * FROM cuentas_receptoras WHERE activa = 1 ORDER BY nombre_cuenta, alias_referencia";

    private static final String SQL_FIND_BY_PLATAFORMA =
        "SELECT * FROM cuentas_receptoras WHERE activa = 1 AND banco_plataforma = ? ORDER BY alias_referencia";

    private static final String SQL_FIND_BY_ID =
        "SELECT * FROM cuentas_receptoras WHERE id = ?";

    private static final String SQL_INSERT =
        "INSERT INTO cuentas_receptoras (nombre_cuenta, banco_plataforma, alias_referencia, activa) VALUES (?, ?, ?, ?)";

    private static final String SQL_UPDATE =
        "UPDATE cuentas_receptoras SET nombre_cuenta = ?, banco_plataforma = ?, alias_referencia = ?, activa = ? WHERE id = ?";

    private static final String SQL_DELETE =
        "DELETE FROM cuentas_receptoras WHERE id = ?";

    @Override
    public List<CuentaReceptora> findAll() throws DatabaseException {
        List<CuentaReceptora> lista = new ArrayList<>();
        try (Connection conn = DatabaseConnection.connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(SQL_FIND_ALL)) {

            while (rs.next()) {
                lista.add(mapRow(rs));
            }
        } catch (SQLException e) {
            logger.error("Error al listar cuentas receptoras", e);
            throw DatabaseException.queryFailed("FIND_ALL_CUENTAS_RECEPTORAS", e);
        }
        return lista;
    }

    @Override
    public List<CuentaReceptora> findActivas() throws DatabaseException {
        List<CuentaReceptora> lista = new ArrayList<>();
        try (Connection conn = DatabaseConnection.connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(SQL_FIND_ACTIVAS)) {

            while (rs.next()) {
                lista.add(mapRow(rs));
            }
        } catch (SQLException e) {
            logger.error("Error al listar cuentas receptoras activas", e);
            throw DatabaseException.queryFailed("FIND_ACTIVAS_CUENTAS_RECEPTORAS", e);
        }
        return lista;
    }

    @Override
    public List<CuentaReceptora> findByPlataforma(String bancoPlataforma) throws DatabaseException {
        List<CuentaReceptora> lista = new ArrayList<>();
        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(SQL_FIND_BY_PLATAFORMA)) {

            pstmt.setString(1, bancoPlataforma);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error al filtrar cuentas por plataforma: {}", bancoPlataforma, e);
            throw DatabaseException.queryFailed("FIND_BY_PLATAFORMA", e);
        }
        return lista;
    }

    @Override
    public CuentaReceptora findById(int id) throws DatabaseException {
        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(SQL_FIND_BY_ID)) {

            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        } catch (SQLException e) {
            logger.error("Error al buscar cuenta receptora ID={}", id, e);
            throw DatabaseException.queryFailed("FIND_BY_ID_CUENTA_RECEPTORA", e);
        }
        return null;
    }

    @Override
    public void save(CuentaReceptora cuenta) throws DatabaseException {
        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, cuenta.getNombreCuenta());
            pstmt.setString(2, cuenta.getBancoPlataforma());
            pstmt.setString(3, cuenta.getAliasReferencia());
            pstmt.setInt(4, cuenta.isActiva() ? 1 : 0);
            pstmt.executeUpdate();

            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    cuenta.setId(rs.getInt(1));
                }
            }
            logger.info("Cuenta receptora creada: {} (ID={})", cuenta.getAliasReferencia(), cuenta.getId());
        } catch (SQLException e) {
            logger.error("Error al guardar cuenta receptora", e);
            throw DatabaseException.queryFailed("SAVE_CUENTA_RECEPTORA", e);
        }
    }

    @Override
    public void update(CuentaReceptora cuenta) throws DatabaseException {
        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(SQL_UPDATE)) {

            pstmt.setString(1, cuenta.getNombreCuenta());
            pstmt.setString(2, cuenta.getBancoPlataforma());
            pstmt.setString(3, cuenta.getAliasReferencia());
            pstmt.setInt(4, cuenta.isActiva() ? 1 : 0);
            pstmt.setInt(5, cuenta.getId());
            pstmt.executeUpdate();

            logger.info("Cuenta receptora actualizada: ID={}", cuenta.getId());
        } catch (SQLException e) {
            logger.error("Error al actualizar cuenta receptora", e);
            throw DatabaseException.queryFailed("UPDATE_CUENTA_RECEPTORA", e);
        }
    }

    @Override
    public void delete(int id) throws DatabaseException {
        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(SQL_DELETE)) {

            pstmt.setInt(1, id);
            pstmt.executeUpdate();
            logger.info("Cuenta receptora eliminada: ID={}", id);
        } catch (SQLException e) {
            logger.error("Error al eliminar cuenta receptora", e);
            throw DatabaseException.queryFailed("DELETE_CUENTA_RECEPTORA", e);
        }
    }

    private CuentaReceptora mapRow(ResultSet rs) throws SQLException {
        CuentaReceptora c = new CuentaReceptora();
        c.setId(rs.getInt("id"));
        c.setNombreCuenta(rs.getString("nombre_cuenta"));
        c.setBancoPlataforma(rs.getString("banco_plataforma"));
        c.setAliasReferencia(rs.getString("alias_referencia"));
        c.setActiva(rs.getInt("activa") == 1);
        return c;
    }
}
