package app.service;

import app.db.DatabaseConnection;
import app.exception.DatabaseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Motor de consultas para reportes financieros.
 * Migrado desde: DailyReportWindow.java y WeeklyReportWindow.java del proyecto CapelliSalesWindow.
 *
 * Realiza consultas SQL complejas con GROUP BY y SUM para calcular:
 * - Ingresos totales del día/semana
 * - Ingresos agrupados por método de pago (Efectivo, Punto, Zelle, etc.)
 * - Ingresos agrupados por cuenta receptora (Capelli vs. Rosa vs. Efectivo)
 * - Cuentas por cobrar (deuda real = total venta - total pagado)
 * - IVA recaudado
 */
public class ReporteService {

    private static final Logger logger = LoggerFactory.getLogger(ReporteService.class);

    // ================================================================
    // DTOs internos para resultados de reportes
    // ================================================================

    /**
     * Estadísticas financieras de un solo día.
     */
    public record DailyStats(
        double tasaUsada,
        double efectivoUsd,
        double totalBsCapelli,     // Monto en Bs que entró a Cuenta Capelli (PdV, PM, Transferencia)
        double totalBsRosa,        // Monto en Bs que entró a Cuenta Rosa (PM Rosa, Transferencia Rosa)
        double zelleUsd,           // Monto en USD por Zelle
        double cuentasPorCobrar,   // Deuda real (Total Venta - Total Pagado)
        double otrosUsd,           // Otros métodos en USD
        double totalIva            // IVA recaudado en USD
    ) {
        /**
         * Convierte Bs a USD usando la tasa del día y suma todo.
         */
        public double getTotalDiaUsd() {
            double capelliUsd = (tasaUsada > 0) ? totalBsCapelli / tasaUsada : 0;
            double rosaUsd = (tasaUsada > 0) ? totalBsRosa / tasaUsada : 0;
            return efectivoUsd + zelleUsd + otrosUsd + capelliUsd + rosaUsd + cuentasPorCobrar;
        }

        public double getCapelliConvertidoUsd() {
            return (tasaUsada > 0) ? totalBsCapelli / tasaUsada : 0;
        }

        public double getRosaConvertidoUsd() {
            return (tasaUsada > 0) ? totalBsRosa / tasaUsada : 0;
        }
    }

    /**
     * Datos de un día para el reporte semanal (incluye fecha formateada).
     */
    public record DailyRow(
        LocalDate fecha,
        double efectivoUsd,
        double capelliConvertidoUsd,
        double zelleUsd,
        double cxcUsd,
        double rosaConvertidoUsd,
        double otrosUsd
    ) {
        public double getTotalDia() {
            return efectivoUsd + capelliConvertidoUsd + zelleUsd + cxcUsd + rosaConvertidoUsd + otrosUsd;
        }
    }

    /**
     * Resultado agrupado por cuenta receptora (nombre_cuenta).
     */
    public record ResumenPorCuenta(String nombreCuenta, double totalUsd, double totalBs) {}

    // ================================================================
    // REPORTE DIARIO
    // ================================================================

    /**
     * Calcula las estadísticas financieras de un solo día.
     * Migrado desde: DailyReportWindow.loadReportData()
     *
     * NOTA: Cada consulta usa su propia conexión para evitar el error
     * "database connection closed" del driver SQLite al reutilizar
     * una Connection con múltiples PreparedStatements secuenciales.
     */
    public DailyStats calcularEstadisticasDia(LocalDate fecha) throws DatabaseException {
        String dateStr = fecha.toString();
        double tasaUsada = 0;
        double efectivoUsd = 0;
        double totalBsCapelli = 0;
        double totalBsRosa = 0;
        double zelleUsd = 0;
        double cxcUsd = 0;
        double otrosUsd = 0;
        double totalIva = 0;

        try {
            // 1. Obtener la tasa BCV de referencia del día
            String sqlRate = "SELECT tasa_bcv FROM ventas WHERE DATE(fecha_venta) = ? AND tasa_bcv > 0 ORDER BY fecha_venta ASC LIMIT 1";
            try (Connection conn = DatabaseConnection.connect();
                 PreparedStatement pstmt = conn.prepareStatement(sqlRate)) {
                pstmt.setString(1, dateStr);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        tasaUsada = rs.getDouble(1);
                    }
                }
            }
            if (tasaUsada <= 0) tasaUsada = BCVService.getCachedRate();

            // 2. IVA total del día
            String sqlIva = "SELECT COALESCE(SUM(monto_iva), 0.0) FROM ventas WHERE DATE(fecha_venta) = ?";
            try (Connection conn = DatabaseConnection.connect();
                 PreparedStatement pstmt = conn.prepareStatement(sqlIva)) {
                pstmt.setString(1, dateStr);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) totalIva = rs.getDouble(1);
                }
            }

            // 3. Pagos agrupados por método, moneda y destino
            String sqlPayments = """
                SELECT p.metodo_pago, p.moneda, p.monto, p.destino_pago
                FROM venta_pagos p
                JOIN ventas v ON p.venta_id = v.id
                WHERE DATE(v.fecha_venta) = ?
                """;
            try (Connection conn = DatabaseConnection.connect();
                 PreparedStatement pstmt = conn.prepareStatement(sqlPayments)) {
                pstmt.setString(1, dateStr);
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        String metodo = rs.getString("metodo_pago");
                        String moneda = rs.getString("moneda");
                        double monto = rs.getDouble("monto");
                        String destino = rs.getString("destino_pago");

                        if ("$".equals(moneda)) {
                            if (esEfectivo(metodo)) {
                                efectivoUsd += monto;
                            } else if (esZelleOTransferenciaUsd(metodo)) {
                                zelleUsd += monto;
                            } else {
                                otrosUsd += monto;
                            }
                        } else if ("Bs".equalsIgnoreCase(moneda)) {
                            // Clasificar si es Cuenta Rosa o Capelli basándose en destino_pago
                            if (destino != null && destino.toLowerCase().contains("rosa")) {
                                totalBsRosa += monto;
                            } else {
                                totalBsCapelli += monto;
                            }
                        }
                    }
                }
            }

            // 4. Cuentas por cobrar: deuda real = total venta - total pagado (para ventas PENDIENTE/PARCIAL)
            String sqlCxC = """
                SELECT
                    v.total,
                    (SELECT COALESCE(SUM(
                        CASE WHEN sp.moneda = 'Bs' THEN sp.monto / NULLIF(sp.tasa_bcv_al_pago, 0) ELSE sp.monto END
                    ), 0) FROM venta_pagos sp WHERE sp.venta_id = v.id) as pagado_usd
                FROM ventas v
                WHERE DATE(v.fecha_venta) = ? AND v.estatus IN ('PENDIENTE', 'PARCIAL')
                """;
            try (Connection conn = DatabaseConnection.connect();
                 PreparedStatement pstmt = conn.prepareStatement(sqlCxC)) {
                pstmt.setString(1, dateStr);
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        double totalVenta = rs.getDouble("total");
                        double pagadoUsd = rs.getDouble("pagado_usd");
                        double deuda = totalVenta - pagadoUsd;
                        if (deuda > 0.01) {
                            cxcUsd += deuda;
                        }
                    }
                }
            }

        } catch (SQLException e) {
            logger.error("Error al calcular estadísticas del día: {}", dateStr, e);
            throw DatabaseException.queryFailed("REPORTE_DIARIO", e);
        }

        return new DailyStats(tasaUsada, efectivoUsd, totalBsCapelli, totalBsRosa, zelleUsd, cxcUsd, otrosUsd, totalIva);
    }

    /**
     * Determina si un método de pago es "Efectivo" con coincidencia flexible.
     * Soporta: "Efectivo", "Efectivo $", "EFECTIVO", "efectivo", etc.
     */
    private boolean esEfectivo(String metodo) {
        if (metodo == null) return false;
        String m = metodo.trim().toLowerCase();
        return m.equals("efectivo") || m.startsWith("efectivo ");
    }

    /**
     * Determina si un método es Zelle o Transferencia en USD.
     * Soporta variantes como "Zelle", "ZELLE", "Transferencia", etc.
     */
    private boolean esZelleOTransferenciaUsd(String metodo) {
        if (metodo == null) return false;
        String m = metodo.trim().toLowerCase();
        return m.equals("zelle") || m.startsWith("zelle ") ||
               m.equals("transferencia") || m.startsWith("transferencia ");
    }

    // ================================================================
    // REPORTE SEMANAL / POR RANGO
    // ================================================================

    /**
     * Genera los datos día-a-día de un rango de fechas para el reporte semanal.
     * Migrado desde: WeeklyReportWindow.calcularDatosDia()
     */
    public List<DailyRow> calcularReporteRango(LocalDate desde, LocalDate hasta) throws DatabaseException {
        List<DailyRow> rows = new ArrayList<>();
        LocalDate current = desde;

        while (!current.isAfter(hasta)) {
            DailyStats stats = calcularEstadisticasDia(current);

            rows.add(new DailyRow(
                current,
                stats.efectivoUsd(),
                stats.getCapelliConvertidoUsd(),
                stats.zelleUsd(),
                stats.cuentasPorCobrar(),
                stats.getRosaConvertidoUsd(),
                stats.otrosUsd()
            ));

            current = current.plusDays(1);
        }

        return rows;
    }

    // ================================================================
    // RESÚMENES AGRUPADOS POR CUENTA RECEPTORA
    // ================================================================

    /**
     * Agrupa pagos por cuenta receptora (nombre_cuenta) en un rango de fechas.
     * Esto permite ver: "Dinero en Cuenta Capelli", "Dinero en Cuenta Rosa", "Efectivo Caja"
     */
    public Map<String, Double> getIngresoPorCuentaReceptora(LocalDate desde, LocalDate hasta) throws DatabaseException {
        // Clasificar pagos por destino_pago → nombre_cuenta
        // Usa LIKE con comodines para matching flexible de "Efectivo" y variantes
        String sql = """
            SELECT
                CASE
                    WHEN p.destino_pago LIKE '%Rosa%' THEN 'Cuenta Rosa'
                    WHEN p.metodo_pago LIKE 'Efectivo%' THEN 'Efectivo Caja'
                    ELSE 'Cuenta Capelli'
                END as cuenta,
                SUM(
                    CASE
                        WHEN p.moneda = 'Bs' THEN p.monto / NULLIF(p.tasa_bcv_al_pago, 0)
                        ELSE p.monto
                    END
                ) as total_usd
            FROM venta_pagos p
            JOIN ventas v ON p.venta_id = v.id
            WHERE DATE(v.fecha_venta) BETWEEN DATE(?) AND DATE(?)
            GROUP BY cuenta
            ORDER BY total_usd DESC
            """;

        Map<String, Double> resultado = new LinkedHashMap<>();

        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, desde.toString());
            pstmt.setString(2, hasta.toString());

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    resultado.put(rs.getString("cuenta"), rs.getDouble("total_usd"));
                }
            }
        } catch (SQLException e) {
            logger.error("Error al obtener ingreso por cuenta receptora", e);
            throw DatabaseException.queryFailed("INGRESO_POR_CUENTA", e);
        }

        return resultado;
    }

    /**
     * Agrupa pagos por método de pago en un rango de fechas.
     * Resultado: "Efectivo $" → $X, "Zelle" → $Y, "Punto de Venta" → $Z, etc.
     */
    public Map<String, Double> getIngresoPorMetodoPago(LocalDate desde, LocalDate hasta) throws DatabaseException {
        String sql = """
            SELECT
                p.metodo_pago,
                SUM(
                    CASE
                        WHEN p.moneda = 'Bs' THEN p.monto / NULLIF(p.tasa_bcv_al_pago, 0)
                        ELSE p.monto
                    END
                ) as total_usd
            FROM venta_pagos p
            JOIN ventas v ON p.venta_id = v.id
            WHERE DATE(v.fecha_venta) BETWEEN DATE(?) AND DATE(?)
            GROUP BY p.metodo_pago
            ORDER BY total_usd DESC
            """;

        Map<String, Double> resultado = new LinkedHashMap<>();

        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, desde.toString());
            pstmt.setString(2, hasta.toString());

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    resultado.put(rs.getString("metodo_pago"), rs.getDouble("total_usd"));
                }
            }
        } catch (SQLException e) {
            logger.error("Error al obtener ingreso por método de pago", e);
            throw DatabaseException.queryFailed("INGRESO_POR_METODO", e);
        }

        return resultado;
    }

    /**
     * Lista las ventas de un rango de fechas con datos de cabecera para la tabla detallada.
     * Retorna: [{correlativo, fecha, cliente, total, estatus, metodos_pago}]
     */
    public List<Map<String, Object>> getDetalleVentasRango(LocalDate desde, LocalDate hasta) throws DatabaseException {
        String sql = """
            SELECT
                v.id, v.numero_correlativo, v.fecha_venta, v.subtotal, v.monto_descuento,
                v.monto_iva, v.total, v.tasa_bcv, v.estatus,
                COALESCE(c.nombre_completo, 'Cliente Casual') as cliente,
                GROUP_CONCAT(DISTINCT p.metodo_pago) as metodos_pago
            FROM ventas v
            LEFT JOIN clientes c ON v.cliente_id = c.id
            LEFT JOIN venta_pagos p ON p.venta_id = v.id
            WHERE DATE(v.fecha_venta) BETWEEN DATE(?) AND DATE(?)
            GROUP BY v.id
            ORDER BY v.fecha_venta DESC
            """;

        List<Map<String, Object>> result = new ArrayList<>();

        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, desde.toString());
            pstmt.setString(2, hasta.toString());

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("id", rs.getInt("id"));
                    row.put("correlativo", rs.getString("numero_correlativo"));
                    row.put("fecha", rs.getString("fecha_venta"));
                    row.put("cliente", rs.getString("cliente"));
                    row.put("subtotal", rs.getDouble("subtotal"));
                    row.put("descuento", rs.getDouble("monto_descuento"));
                    row.put("iva", rs.getDouble("monto_iva"));
                    row.put("total", rs.getDouble("total"));
                    row.put("tasa_bcv", rs.getDouble("tasa_bcv"));
                    row.put("estatus", rs.getString("estatus"));
                    row.put("metodos_pago", rs.getString("metodos_pago"));
                    result.add(row);
                }
            }
        } catch (SQLException e) {
            logger.error("Error al obtener detalle de ventas", e);
            throw DatabaseException.queryFailed("DETALLE_VENTAS_RANGO", e);
        }

        return result;
    }

}
