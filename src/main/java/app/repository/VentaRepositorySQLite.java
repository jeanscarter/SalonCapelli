package app.repository;

import app.db.DatabaseConnection;
import app.exception.DatabaseException;
import app.model.Pago;
import app.model.Propina;
import app.model.Venta;
import app.model.VentaItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementación SQLite del repositorio de Ventas.
 * 
 * El método saveCompleteVenta ejecuta toda la operación en una sola transacción SQL:
 *   BEGIN → INSERT venta → INSERT items → INSERT pagos → INSERT propinas 
 *   → UPDATE stock productos → UPDATE saldo cliente → COMMIT
 * 
 * Si algo falla: ROLLBACK automático.
 */
public class VentaRepositorySQLite implements VentaRepository {

    private static final Logger logger = LoggerFactory.getLogger(VentaRepositorySQLite.class);

    private static final DateTimeFormatter DB_DATETIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // ===== SQL Statements =====

    private static final String SQL_INSERT_VENTA =
        "INSERT INTO ventas (cliente_id, fecha_venta, subtotal, tipo_descuento, monto_descuento, " +
        "monto_iva, total, tasa_bcv, numero_correlativo) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String SQL_INSERT_ITEM =
        "INSERT INTO venta_items (venta_id, servicio_id, trabajadora_id, precio_venta, " +
        "cliente_trajo_producto, producto_id) VALUES (?, ?, ?, ?, ?, ?)";

    private static final String SQL_INSERT_PAGO =
        "INSERT INTO venta_pagos (venta_id, monto, moneda, metodo_pago, destino_pago, " +
        "referencia_pago, tasa_bcv_al_pago) VALUES (?, ?, ?, ?, ?, ?, ?)";

    private static final String SQL_INSERT_PROPINA =
        "INSERT INTO propinas (venta_id, trabajadora_id, monto) VALUES (?, ?, ?)";

    private static final String SQL_UPDATE_STOCK =
        "UPDATE productos SET stock_actual = stock_actual - 1 WHERE id = ?";

    private static final String SQL_INSERT_MOVIMIENTO =
        "INSERT INTO inventario_movimientos (producto_id, tipo_movimiento, cantidad, motivo, venta_id) " +
        "VALUES (?, 'SALIDA', 1, 'Uso en servicio', ?)";

    private static final String SQL_UPDATE_CLIENTE_SALDO =
        "UPDATE clientes SET saldo_favor = COALESCE(saldo_favor, 0) + ? WHERE id = ?";

    private static final String SQL_DELETE =
        "DELETE FROM ventas WHERE id=?";

    private static final String SQL_FIND_BY_ID =
        "SELECT v.*, c.nombre_completo AS nombre_cliente FROM ventas v " +
        "LEFT JOIN clientes c ON v.cliente_id = c.id WHERE v.id=?";

    private static final String SQL_FIND_BY_RANGO =
        "SELECT v.*, c.nombre_completo AS nombre_cliente FROM ventas v " +
        "LEFT JOIN clientes c ON v.cliente_id = c.id " +
        "WHERE DATE(v.fecha_venta) BETWEEN ? AND ? ORDER BY v.fecha_venta DESC";

    private static final String SQL_FIND_BY_CLIENTE =
        "SELECT v.*, c.nombre_completo AS nombre_cliente FROM ventas v " +
        "LEFT JOIN clientes c ON v.cliente_id = c.id " +
        "WHERE v.cliente_id=? ORDER BY v.fecha_venta DESC";

    private static final String SQL_FIND_RECIENTES =
        "SELECT v.*, c.nombre_completo AS nombre_cliente FROM ventas v " +
        "LEFT JOIN clientes c ON v.cliente_id = c.id " +
        "ORDER BY v.fecha_venta DESC LIMIT ?";

    private static final String SQL_COUNT =
        "SELECT COUNT(*) FROM ventas";

    private static final String SQL_SUM_TOTAL_RANGO =
        "SELECT COALESCE(SUM(total), 0) FROM ventas WHERE DATE(fecha_venta) BETWEEN ? AND ?";

    // Queries para cargar colecciones hijas
    private static final String SQL_FIND_ITEMS_BY_VENTA =
        "SELECT vi.*, s.nombre AS nombre_servicio, " +
        "(t.nombres || ' ' || t.apellidos) AS nombre_trabajadora " +
        "FROM venta_items vi " +
        "JOIN servicios s ON vi.servicio_id = s.id " +
        "JOIN trabajadoras t ON vi.trabajadora_id = t.id " +
        "WHERE vi.venta_id=?";

    private static final String SQL_FIND_PAGOS_BY_VENTA =
        "SELECT * FROM venta_pagos WHERE venta_id=?";

    private static final String SQL_FIND_PROPINAS_BY_VENTA =
        "SELECT p.*, (t.nombres || ' ' || t.apellidos) AS nombre_trabajadora " +
        "FROM propinas p JOIN trabajadoras t ON p.trabajadora_id = t.id " +
        "WHERE p.venta_id=?";


    @Override
    public void saveCompleteVenta(Venta venta) throws DatabaseException {
        Connection conn = null;
        try {
            conn = DatabaseConnection.connect();
            conn.setAutoCommit(false);

            // 1. Insertar cabecera de venta
            int ventaId;
            try (PreparedStatement pstmt = conn.prepareStatement(SQL_INSERT_VENTA, Statement.RETURN_GENERATED_KEYS)) {
                if (venta.getClienteId() != null) {
                    pstmt.setInt(1, venta.getClienteId());
                } else {
                    pstmt.setNull(1, Types.INTEGER);
                }
                pstmt.setString(2, venta.getFechaVenta().format(DB_DATETIME_FORMAT));
                pstmt.setDouble(3, venta.getSubtotal());
                pstmt.setString(4, venta.getTipoDescuento());
                pstmt.setDouble(5, venta.getMontoDescuento());
                pstmt.setDouble(6, venta.getMontoIva());
                pstmt.setDouble(7, venta.getTotal());
                pstmt.setDouble(8, venta.getTasaBcv());
                pstmt.setString(9, venta.getNumeroCorrelativo());

                pstmt.executeUpdate();

                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        ventaId = rs.getInt(1);
                        venta.setId(ventaId);
                    } else {
                        throw new SQLException("No se generó ID para la venta");
                    }
                }
            }
            logger.debug("Venta insertada (ID={})", ventaId);

            // 2. Insertar ítems de venta
            try (PreparedStatement pstmtItem = conn.prepareStatement(SQL_INSERT_ITEM);
                 PreparedStatement pstmtStock = conn.prepareStatement(SQL_UPDATE_STOCK);
                 PreparedStatement pstmtMov = conn.prepareStatement(SQL_INSERT_MOVIMIENTO)) {

                for (VentaItem item : venta.getItems()) {
                    item.setVentaId(ventaId);

                    pstmtItem.setInt(1, ventaId);
                    pstmtItem.setInt(2, item.getServicioId());
                    pstmtItem.setInt(3, item.getTrabajadoraId());
                    pstmtItem.setDouble(4, item.getPrecioVenta());
                    pstmtItem.setInt(5, item.isClienteTrajoProducto() ? 1 : 0);
                    if (item.getProductoId() != null) {
                        pstmtItem.setInt(6, item.getProductoId());
                    } else {
                        pstmtItem.setNull(6, Types.INTEGER);
                    }
                    pstmtItem.addBatch();

                    // Actualizar stock si el ítem usa un producto de inventario
                    if (item.getProductoId() != null) {
                        pstmtStock.setInt(1, item.getProductoId());
                        pstmtStock.addBatch();

                        pstmtMov.setInt(1, item.getProductoId());
                        pstmtMov.setInt(2, ventaId);
                        pstmtMov.addBatch();
                    }
                }
                pstmtItem.executeBatch();
                pstmtStock.executeBatch();
                pstmtMov.executeBatch();
            }
            logger.debug("Items insertados: {} para venta ID={}", venta.getItems().size(), ventaId);

            // 3. Insertar pagos
            try (PreparedStatement pstmtPago = conn.prepareStatement(SQL_INSERT_PAGO)) {
                for (Pago pago : venta.getPagos()) {
                    pago.setVentaId(ventaId);

                    pstmtPago.setInt(1, ventaId);
                    pstmtPago.setDouble(2, pago.getMonto());
                    pstmtPago.setString(3, pago.getMoneda());
                    pstmtPago.setString(4, pago.getMetodoPago());
                    pstmtPago.setString(5, pago.getDestinoPago());
                    pstmtPago.setString(6, pago.getReferenciaPago());
                    pstmtPago.setDouble(7, pago.getTasaBcvAlPago());
                    pstmtPago.addBatch();
                }
                pstmtPago.executeBatch();
            }
            logger.debug("Pagos insertados: {} para venta ID={}", venta.getPagos().size(), ventaId);

            // 4. Insertar propinas
            if (!venta.getPropinas().isEmpty()) {
                try (PreparedStatement pstmtProp = conn.prepareStatement(SQL_INSERT_PROPINA)) {
                    for (Propina propina : venta.getPropinas()) {
                        propina.setVentaId(ventaId);

                        pstmtProp.setInt(1, ventaId);
                        pstmtProp.setInt(2, propina.getTrabajadoraId());
                        pstmtProp.setDouble(3, propina.getMonto());
                        pstmtProp.addBatch();
                    }
                    pstmtProp.executeBatch();
                }
                logger.debug("Propinas insertadas: {} para venta ID={}", venta.getPropinas().size(), ventaId);
            }

            // 5. Actualizar saldo a favor del cliente si hay vuelto
            if (venta.getClienteId() != null) {
                double vuelto = venta.getVuelto();
                if (vuelto > 0.01) {
                    try (PreparedStatement pstmtSaldo = conn.prepareStatement(SQL_UPDATE_CLIENTE_SALDO)) {
                        pstmtSaldo.setDouble(1, vuelto);
                        pstmtSaldo.setInt(2, venta.getClienteId());
                        pstmtSaldo.executeUpdate();
                    }
                    logger.debug("Saldo a favor actualizado: +${} para cliente ID={}", vuelto, venta.getClienteId());
                }
            }

            // COMMIT
            conn.commit();
            logger.info("✓ Venta completa guardada exitosamente (ID={}, Items={}, Pagos={}, Propinas={})",
                    ventaId, venta.getItems().size(), venta.getPagos().size(), venta.getPropinas().size());

        } catch (SQLException e) {
            // ROLLBACK
            if (conn != null) {
                try {
                    conn.rollback();
                    logger.warn("ROLLBACK ejecutado para venta fallida");
                } catch (SQLException rbEx) {
                    logger.error("Error durante ROLLBACK", rbEx);
                }
            }
            throw DatabaseException.queryFailed("SAVE_COMPLETE_VENTA", e);
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                } catch (SQLException e) {
                    logger.error("Error restaurando auto-commit", e);
                }
            }
        }
    }

    @Override
    public void delete(int id) throws DatabaseException {
        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(SQL_DELETE)) {

            pstmt.setInt(1, id);
            pstmt.executeUpdate();
            logger.debug("Venta eliminada (ID={})", id);
        } catch (SQLException e) {
            throw DatabaseException.queryFailed("DELETE VENTA", e);
        }
    }

    @Override
    public Venta findById(int id) throws DatabaseException {
        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(SQL_FIND_BY_ID)) {

            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Venta venta = mapResultSetToVenta(rs);
                    cargarColeccionesHijas(conn, venta);
                    return venta;
                }
            }
        } catch (SQLException e) {
            throw DatabaseException.queryFailed("FIND_BY_ID VENTA", e);
        }
        return null;
    }

    @Override
    public List<Venta> findByRangoFechas(LocalDate inicio, LocalDate fin) throws DatabaseException {
        List<Venta> lista = new ArrayList<>();
        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(SQL_FIND_BY_RANGO)) {

            pstmt.setString(1, inicio.toString());
            pstmt.setString(2, fin.toString());

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapResultSetToVenta(rs));
                }
            }
        } catch (SQLException e) {
            throw DatabaseException.queryFailed("FIND_BY_RANGO VENTAS", e);
        }
        return lista;
    }

    @Override
    public List<Venta> findByClienteId(int clienteId) throws DatabaseException {
        List<Venta> lista = new ArrayList<>();
        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(SQL_FIND_BY_CLIENTE)) {

            pstmt.setInt(1, clienteId);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapResultSetToVenta(rs));
                }
            }
        } catch (SQLException e) {
            throw DatabaseException.queryFailed("FIND_BY_CLIENTE VENTAS", e);
        }
        return lista;
    }

    @Override
    public List<Venta> findRecientes(int limit) throws DatabaseException {
        List<Venta> lista = new ArrayList<>();
        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(SQL_FIND_RECIENTES)) {

            pstmt.setInt(1, limit);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapResultSetToVenta(rs));
                }
            }
        } catch (SQLException e) {
            throw DatabaseException.queryFailed("FIND_RECIENTES VENTAS", e);
        }
        return lista;
    }

    @Override
    public int count() throws DatabaseException {
        try (Connection conn = DatabaseConnection.connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(SQL_COUNT)) {

            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw DatabaseException.queryFailed("COUNT VENTAS", e);
        }
        return 0;
    }

    @Override
    public double sumTotalByRangoFechas(LocalDate inicio, LocalDate fin) throws DatabaseException {
        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(SQL_SUM_TOTAL_RANGO)) {

            pstmt.setString(1, inicio.toString());
            pstmt.setString(2, fin.toString());

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble(1);
                }
            }
        } catch (SQLException e) {
            throw DatabaseException.queryFailed("SUM_TOTAL_RANGO VENTAS", e);
        }
        return 0;
    }

    // ===== Mappers privados =====

    private Venta mapResultSetToVenta(ResultSet rs) throws SQLException {
        Venta venta = new Venta();
        venta.setId(rs.getInt("id"));

        int clienteId = rs.getInt("cliente_id");
        venta.setClienteId(rs.wasNull() ? null : clienteId);

        String fechaStr = rs.getString("fecha_venta");
        if (fechaStr != null) {
            try {
                venta.setFechaVenta(LocalDateTime.parse(fechaStr, DB_DATETIME_FORMAT));
            } catch (Exception e) {
                // Intentar formato ISO como fallback
                venta.setFechaVenta(LocalDateTime.parse(fechaStr));
            }
        }

        venta.setSubtotal(rs.getDouble("subtotal"));
        venta.setTipoDescuento(rs.getString("tipo_descuento"));
        venta.setMontoDescuento(rs.getDouble("monto_descuento"));
        venta.setMontoIva(rs.getDouble("monto_iva"));
        venta.setTotal(rs.getDouble("total"));
        venta.setTasaBcv(rs.getDouble("tasa_bcv"));
        venta.setNumeroCorrelativo(rs.getString("numero_correlativo"));

        try {
            venta.setNombreCliente(rs.getString("nombre_cliente"));
        } catch (SQLException ignored) {
            // nombre_cliente no está en todos los queries
        }

        return venta;
    }

    private void cargarColeccionesHijas(Connection conn, Venta venta) throws SQLException {
        // Cargar items
        try (PreparedStatement pstmt = conn.prepareStatement(SQL_FIND_ITEMS_BY_VENTA)) {
            pstmt.setInt(1, venta.getId());
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    VentaItem item = new VentaItem();
                    item.setId(rs.getInt("id"));
                    item.setVentaId(rs.getInt("venta_id"));
                    item.setServicioId(rs.getInt("servicio_id"));
                    item.setTrabajadoraId(rs.getInt("trabajadora_id"));
                    item.setPrecioVenta(rs.getDouble("precio_venta"));
                    item.setClienteTrajoProducto(rs.getInt("cliente_trajo_producto") == 1);
                    int prodId = rs.getInt("producto_id");
                    item.setProductoId(rs.wasNull() ? null : prodId);
                    item.setNombreServicio(rs.getString("nombre_servicio"));
                    item.setNombreTrabajadora(rs.getString("nombre_trabajadora"));
                    venta.getItems().add(item);
                }
            }
        }

        // Cargar pagos
        try (PreparedStatement pstmt = conn.prepareStatement(SQL_FIND_PAGOS_BY_VENTA)) {
            pstmt.setInt(1, venta.getId());
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Pago pago = new Pago();
                    pago.setId(rs.getInt("id"));
                    pago.setVentaId(rs.getInt("venta_id"));
                    pago.setMonto(rs.getDouble("monto"));
                    pago.setMoneda(rs.getString("moneda"));
                    pago.setMetodoPago(rs.getString("metodo_pago"));
                    pago.setDestinoPago(rs.getString("destino_pago"));
                    pago.setReferenciaPago(rs.getString("referencia_pago"));
                    pago.setTasaBcvAlPago(rs.getDouble("tasa_bcv_al_pago"));
                    venta.getPagos().add(pago);
                }
            }
        }

        // Cargar propinas
        try (PreparedStatement pstmt = conn.prepareStatement(SQL_FIND_PROPINAS_BY_VENTA)) {
            pstmt.setInt(1, venta.getId());
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Propina propina = new Propina();
                    propina.setId(rs.getInt("id"));
                    propina.setVentaId(rs.getInt("venta_id"));
                    propina.setTrabajadoraId(rs.getInt("trabajadora_id"));
                    propina.setMonto(rs.getDouble("monto"));
                    propina.setNombreTrabajadora(rs.getString("nombre_trabajadora"));
                    venta.getPropinas().add(propina);
                }
            }
        }
    }
}
