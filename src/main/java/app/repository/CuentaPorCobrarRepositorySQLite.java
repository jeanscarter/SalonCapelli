package app.repository;

import app.db.DatabaseConnection;
import app.exception.DatabaseException;
import app.model.CuentaPorCobrar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CuentaPorCobrarRepositorySQLite implements CuentaPorCobrarRepository {

    private static final Logger logger = LoggerFactory.getLogger(CuentaPorCobrarRepositorySQLite.class);

    @Override
    public CuentaPorCobrar save(CuentaPorCobrar cxc) throws DatabaseException {
        String sql = "INSERT INTO cuentas_por_cobrar (cliente_id, venta_id, monto_original, monto_pendiente, estatus, fecha_creacion) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            pstmt.setInt(1, cxc.getClienteId());
            pstmt.setInt(2, cxc.getVentaId());
            pstmt.setDouble(3, cxc.getMontoOriginal());
            pstmt.setDouble(4, cxc.getMontoPendiente());
            pstmt.setString(5, cxc.getEstatus());
            pstmt.setString(6, cxc.getFechaCreacion() != null ? cxc.getFechaCreacion().toString() : LocalDateTime.now().toString());
            
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating CuentaPorCobrar failed, no rows affected.");
            }

            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    cxc.setId(generatedKeys.getInt(1));
                } else {
                    throw new SQLException("Creating CuentaPorCobrar failed, no ID obtained.");
                }
            }
            return cxc;
        } catch (SQLException e) {
            logger.error("Error al guardar CuentaPorCobrar", e);
            throw new DatabaseException("Error al guardar CuentaPorCobrar: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<CuentaPorCobrar> findById(Integer id) throws DatabaseException {
        String sql = "SELECT * FROM cuentas_por_cobrar WHERE id = ?";
        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToCuentaPorCobrar(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error al buscar CuentaPorCobrar por ID", e);
            throw new DatabaseException("Error al buscar CuentaPorCobrar por ID", e);
        }
        return Optional.empty();
    }

    @Override
    public List<CuentaPorCobrar> findAll() throws DatabaseException {
        List<CuentaPorCobrar> list = new ArrayList<>();
        String sql = "SELECT c.*, cl.nombres || ' ' || cl.apellidos AS nombreCliente, v.numero_correlativo AS numeroFactura " +
                     "FROM cuentas_por_cobrar c " +
                     "JOIN clientes cl ON c.cliente_id = cl.id " +
                     "JOIN ventas v ON c.venta_id = v.id ORDER BY c.fecha_creacion DESC";
        try (Connection conn = DatabaseConnection.connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                CuentaPorCobrar cxc = mapResultSetToCuentaPorCobrar(rs);
                cxc.setNombreCliente(rs.getString("nombreCliente"));
                cxc.setNumeroFactura(rs.getString("numeroFactura"));
                list.add(cxc);
            }
        } catch (SQLException e) {
            logger.error("Error al obtener cuentas por cobrar", e);
            throw new DatabaseException("Error al obtener cuentas por cobrar", e);
        }
        return list;
    }

    @Override
    public CuentaPorCobrar update(CuentaPorCobrar cxc) throws DatabaseException {
        String sql = "UPDATE cuentas_por_cobrar SET monto_pendiente = ?, estatus = ?, fecha_ultimo_abono = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setDouble(1, cxc.getMontoPendiente());
            pstmt.setString(2, cxc.getEstatus());
            pstmt.setString(3, cxc.getFechaUltimoAbono() != null ? cxc.getFechaUltimoAbono().toString() : null);
            pstmt.setInt(4, cxc.getId());
            
            pstmt.executeUpdate();
            return cxc;
        } catch (SQLException e) {
            logger.error("Error al actualizar CuentaPorCobrar", e);
            throw new DatabaseException("Error al actualizar CuentaPorCobrar", e);
        }
    }

    @Override
    public void delete(Integer id) throws DatabaseException {
        String sql = "DELETE FROM cuentas_por_cobrar WHERE id = ?";
        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error al eliminar CuentaPorCobrar", e);
            throw new DatabaseException("Error al eliminar CuentaPorCobrar", e);
        }
    }

    @Override
    public List<CuentaPorCobrar> findByClienteId(int clienteId) throws DatabaseException {
        List<CuentaPorCobrar> list = new ArrayList<>();
        String sql = "SELECT c.*, cl.nombres || ' ' || cl.apellidos AS nombreCliente, v.numero_correlativo AS numeroFactura " +
                     "FROM cuentas_por_cobrar c " +
                     "JOIN clientes cl ON c.cliente_id = cl.id " +
                     "JOIN ventas v ON c.venta_id = v.id WHERE c.cliente_id = ? ORDER BY c.fecha_creacion DESC";
        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, clienteId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    CuentaPorCobrar cxc = mapResultSetToCuentaPorCobrar(rs);
                    cxc.setNombreCliente(rs.getString("nombreCliente"));
                    cxc.setNumeroFactura(rs.getString("numeroFactura"));
                    list.add(cxc);
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error al buscar cuentas por cliente", e);
        }
        return list;
    }

    @Override
    public List<CuentaPorCobrar> findPendientes() throws DatabaseException {
        List<CuentaPorCobrar> list = new ArrayList<>();
        String sql = "SELECT c.*, cl.nombres || ' ' || cl.apellidos AS nombreCliente, v.numero_correlativo AS numeroFactura " +
                     "FROM cuentas_por_cobrar c " +
                     "JOIN clientes cl ON c.cliente_id = cl.id " +
                     "JOIN ventas v ON c.venta_id = v.id WHERE c.estatus IN ('PENDIENTE', 'PARCIAL') ORDER BY c.fecha_creacion ASC";
        try (Connection conn = DatabaseConnection.connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                CuentaPorCobrar cxc = mapResultSetToCuentaPorCobrar(rs);
                cxc.setNombreCliente(rs.getString("nombreCliente"));
                cxc.setNumeroFactura(rs.getString("numeroFactura"));
                list.add(cxc);
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error al buscar cuentas pendientes", e);
        }
        return list;
    }

    @Override
    public Optional<CuentaPorCobrar> findByVentaId(int ventaId) throws DatabaseException {
        String sql = "SELECT * FROM cuentas_por_cobrar WHERE venta_id = ?";
        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, ventaId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToCuentaPorCobrar(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error al buscar CuentaPorCobrar por venta_id", e);
        }
        return Optional.empty();
    }

    private CuentaPorCobrar mapResultSetToCuentaPorCobrar(ResultSet rs) throws SQLException {
        CuentaPorCobrar cxc = new CuentaPorCobrar();
        cxc.setId(rs.getInt("id"));
        cxc.setClienteId(rs.getInt("cliente_id"));
        cxc.setVentaId(rs.getInt("venta_id"));
        cxc.setMontoOriginal(rs.getDouble("monto_original"));
        cxc.setMontoPendiente(rs.getDouble("monto_pendiente"));
        
        String fechaStr = rs.getString("fecha_creacion");
        if (fechaStr != null) {
            cxc.setFechaCreacion(LocalDateTime.parse(fechaStr));
        }
        
        String fechaAbonoStr = rs.getString("fecha_ultimo_abono");
        if (fechaAbonoStr != null) {
            cxc.setFechaUltimoAbono(LocalDateTime.parse(fechaAbonoStr));
        }
        
        cxc.setEstatus(rs.getString("estatus"));
        return cxc;
    }
}
