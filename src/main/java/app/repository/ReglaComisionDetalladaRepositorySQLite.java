package app.repository;

import app.db.DatabaseConnection;
import app.exception.DatabaseException;
import app.model.ReglaComisionDetallada;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementación SQLite del repositorio de reglas de comisión detalladas.
 * 
 * El método findReglaMasEspecifica() reemplaza completamente la lógica hardcoded
 * de PayrollService.calculateCommissionForItem() del LEGACY. Ejecuta una query
 * SQL con filtros dinámicos y ordena por prioridad DESC para devolver la regla
 * más específica que aplique al caso dado.
 */
public class ReglaComisionDetalladaRepositorySQLite implements ReglaComisionDetalladaRepository {

    private static final Logger logger = LoggerFactory.getLogger(ReglaComisionDetalladaRepositorySQLite.class);

    private static final String SQL_CREATE =
        "INSERT INTO reglas_comision_detalladas " +
        "(trabajadora_id, servicio_id, categoria_servicio, cliente_trae_producto, " +
        "tipo_comision, valor_comision, precio_condicion, prioridad, activo, descripcion) " +
        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String SQL_UPDATE =
        "UPDATE reglas_comision_detalladas SET " +
        "trabajadora_id=?, servicio_id=?, categoria_servicio=?, cliente_trae_producto=?, " +
        "tipo_comision=?, valor_comision=?, precio_condicion=?, prioridad=?, activo=?, descripcion=? " +
        "WHERE id=?";

    private static final String SQL_DELETE =
        "DELETE FROM reglas_comision_detalladas WHERE id=?";

    private static final String SQL_FIND_ALL =
        "SELECT rcd.*, " +
        "(t.nombres || ' ' || t.apellidos) AS nombre_trabajadora, " +
        "s.nombre AS nombre_servicio " +
        "FROM reglas_comision_detalladas rcd " +
        "LEFT JOIN trabajadoras t ON rcd.trabajadora_id = t.id " +
        "LEFT JOIN servicios s ON rcd.servicio_id = s.id " +
        "ORDER BY rcd.prioridad DESC, rcd.id";

    private static final String SQL_FIND_BY_ID =
        "SELECT rcd.*, " +
        "(t.nombres || ' ' || t.apellidos) AS nombre_trabajadora, " +
        "s.nombre AS nombre_servicio " +
        "FROM reglas_comision_detalladas rcd " +
        "LEFT JOIN trabajadoras t ON rcd.trabajadora_id = t.id " +
        "LEFT JOIN servicios s ON rcd.servicio_id = s.id " +
        "WHERE rcd.id=?";

    private static final String SQL_FIND_BY_TRABAJADORA =
        "SELECT rcd.*, " +
        "(t.nombres || ' ' || t.apellidos) AS nombre_trabajadora, " +
        "s.nombre AS nombre_servicio " +
        "FROM reglas_comision_detalladas rcd " +
        "LEFT JOIN trabajadoras t ON rcd.trabajadora_id = t.id " +
        "LEFT JOIN servicios s ON rcd.servicio_id = s.id " +
        "WHERE rcd.trabajadora_id=? " +
        "ORDER BY rcd.prioridad DESC";

    /**
     * Query de resolución dinámica de comisiones.
     * 
     * Busca la regla activa con mayor prioridad que coincida con los parámetros.
     * Los filtros NULL en la regla actúan como wildcard (aplican a cualquier valor).
     * El precio_condicion se verifica con tolerancia de $0.01.
     */
    private static final String SQL_FIND_REGLA_MAS_ESPECIFICA =
        "SELECT rcd.*, " +
        "(t.nombres || ' ' || t.apellidos) AS nombre_trabajadora, " +
        "s.nombre AS nombre_servicio " +
        "FROM reglas_comision_detalladas rcd " +
        "LEFT JOIN trabajadoras t ON rcd.trabajadora_id = t.id " +
        "LEFT JOIN servicios s ON rcd.servicio_id = s.id " +
        "WHERE rcd.activo = 1 " +
        "  AND (rcd.trabajadora_id IS NULL OR rcd.trabajadora_id = ?) " +
        "  AND (rcd.servicio_id IS NULL OR rcd.servicio_id = ?) " +
        "  AND (rcd.categoria_servicio IS NULL OR rcd.categoria_servicio = ?) " +
        "  AND (rcd.cliente_trae_producto IS NULL OR rcd.cliente_trae_producto = ?) " +
        "  AND (rcd.precio_condicion IS NULL OR ABS(rcd.precio_condicion - ?) < 0.01) " +
        "ORDER BY rcd.prioridad DESC " +
        "LIMIT 1";


    @Override
    public void create(ReglaComisionDetallada regla) throws DatabaseException {
        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(SQL_CREATE, Statement.RETURN_GENERATED_KEYS)) {

            setNullableInt(pstmt, 1, regla.getTrabajadoraId());
            setNullableInt(pstmt, 2, regla.getServicioId());
            pstmt.setString(3, regla.getCategoriaServicio());
            setNullableBoolean(pstmt, 4, regla.getClienteTraeProducto());
            pstmt.setString(5, regla.getTipoComision());
            pstmt.setDouble(6, regla.getValorComision());
            setNullableDouble(pstmt, 7, regla.getPrecioCondicion());
            pstmt.setInt(8, regla.getPrioridad());
            pstmt.setInt(9, regla.isActivo() ? 1 : 0);
            pstmt.setString(10, regla.getDescripcion());

            pstmt.executeUpdate();

            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    regla.setId(rs.getInt(1));
                }
            }
            logger.debug("Regla comisión detallada creada (ID={}): {}", regla.getId(), regla.getDescripcion());
        } catch (SQLException e) {
            throw DatabaseException.queryFailed("CREATE REGLA_COMISION_DETALLADA", e);
        }
    }

    @Override
    public void update(ReglaComisionDetallada regla) throws DatabaseException {
        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(SQL_UPDATE)) {

            setNullableInt(pstmt, 1, regla.getTrabajadoraId());
            setNullableInt(pstmt, 2, regla.getServicioId());
            pstmt.setString(3, regla.getCategoriaServicio());
            setNullableBoolean(pstmt, 4, regla.getClienteTraeProducto());
            pstmt.setString(5, regla.getTipoComision());
            pstmt.setDouble(6, regla.getValorComision());
            setNullableDouble(pstmt, 7, regla.getPrecioCondicion());
            pstmt.setInt(8, regla.getPrioridad());
            pstmt.setInt(9, regla.isActivo() ? 1 : 0);
            pstmt.setString(10, regla.getDescripcion());
            pstmt.setInt(11, regla.getId());

            pstmt.executeUpdate();
            logger.debug("Regla comisión detallada actualizada (ID={})", regla.getId());
        } catch (SQLException e) {
            throw DatabaseException.queryFailed("UPDATE REGLA_COMISION_DETALLADA", e);
        }
    }

    @Override
    public void delete(int id) throws DatabaseException {
        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(SQL_DELETE)) {

            pstmt.setInt(1, id);
            pstmt.executeUpdate();
            logger.debug("Regla comisión detallada eliminada (ID={})", id);
        } catch (SQLException e) {
            throw DatabaseException.queryFailed("DELETE REGLA_COMISION_DETALLADA", e);
        }
    }

    @Override
    public List<ReglaComisionDetallada> findAll() throws DatabaseException {
        List<ReglaComisionDetallada> lista = new ArrayList<>();
        try (Connection conn = DatabaseConnection.connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(SQL_FIND_ALL)) {

            while (rs.next()) {
                lista.add(mapResultSetToRegla(rs));
            }
        } catch (SQLException e) {
            throw DatabaseException.queryFailed("FIND_ALL REGLAS_COMISION_DETALLADAS", e);
        }
        return lista;
    }

    @Override
    public ReglaComisionDetallada findById(int id) throws DatabaseException {
        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(SQL_FIND_BY_ID)) {

            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToRegla(rs);
                }
            }
        } catch (SQLException e) {
            throw DatabaseException.queryFailed("FIND_BY_ID REGLA_COMISION_DETALLADA", e);
        }
        return null;
    }

    @Override
    public List<ReglaComisionDetallada> findByTrabajadora(int trabajadoraId) throws DatabaseException {
        List<ReglaComisionDetallada> lista = new ArrayList<>();
        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(SQL_FIND_BY_TRABAJADORA)) {

            pstmt.setInt(1, trabajadoraId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapResultSetToRegla(rs));
                }
            }
        } catch (SQLException e) {
            throw DatabaseException.queryFailed("FIND_BY_TRABAJADORA REGLAS_COMISION_DETALLADAS", e);
        }
        return lista;
    }

    @Override
    public ReglaComisionDetallada findReglaMasEspecifica(
            int trabajadoraId,
            int servicioId,
            String categoriaServicio,
            boolean clienteTraeProducto,
            double precioVenta) throws DatabaseException {

        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(SQL_FIND_REGLA_MAS_ESPECIFICA)) {

            pstmt.setInt(1, trabajadoraId);
            pstmt.setInt(2, servicioId);
            pstmt.setString(3, categoriaServicio);
            pstmt.setInt(4, clienteTraeProducto ? 1 : 0);
            pstmt.setDouble(5, precioVenta);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToRegla(rs);
                }
            }
        } catch (SQLException e) {
            throw DatabaseException.queryFailed("FIND_REGLA_MAS_ESPECIFICA", e);
        }
        return null;
    }

    // ===== Helpers =====

    private ReglaComisionDetallada mapResultSetToRegla(ResultSet rs) throws SQLException {
        ReglaComisionDetallada regla = new ReglaComisionDetallada();
        regla.setId(rs.getInt("id"));

        int tId = rs.getInt("trabajadora_id");
        regla.setTrabajadoraId(rs.wasNull() ? null : tId);

        int sId = rs.getInt("servicio_id");
        regla.setServicioId(rs.wasNull() ? null : sId);

        regla.setCategoriaServicio(rs.getString("categoria_servicio"));

        int ctp = rs.getInt("cliente_trae_producto");
        regla.setClienteTraeProducto(rs.wasNull() ? null : (ctp == 1));

        regla.setTipoComision(rs.getString("tipo_comision"));
        regla.setValorComision(rs.getDouble("valor_comision"));

        double pc = rs.getDouble("precio_condicion");
        regla.setPrecioCondicion(rs.wasNull() ? null : pc);

        regla.setPrioridad(rs.getInt("prioridad"));
        regla.setActivo(rs.getInt("activo") == 1);
        regla.setDescripcion(rs.getString("descripcion"));

        // Campos transitorios del JOIN
        try {
            regla.setNombreTrabajadora(rs.getString("nombre_trabajadora"));
        } catch (SQLException ignored) {}
        try {
            regla.setNombreServicio(rs.getString("nombre_servicio"));
        } catch (SQLException ignored) {}

        return regla;
    }

    private void setNullableInt(PreparedStatement pstmt, int index, Integer value) throws SQLException {
        if (value != null) {
            pstmt.setInt(index, value);
        } else {
            pstmt.setNull(index, Types.INTEGER);
        }
    }

    private void setNullableDouble(PreparedStatement pstmt, int index, Double value) throws SQLException {
        if (value != null) {
            pstmt.setDouble(index, value);
        } else {
            pstmt.setNull(index, Types.REAL);
        }
    }

    private void setNullableBoolean(PreparedStatement pstmt, int index, Boolean value) throws SQLException {
        if (value != null) {
            pstmt.setInt(index, value ? 1 : 0);
        } else {
            pstmt.setNull(index, Types.INTEGER);
        }
    }
}
