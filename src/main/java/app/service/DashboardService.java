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
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Servicio de métricas y estadísticas para el Home/Dashboard.
 */
public class DashboardService {

    private static final Logger logger = LoggerFactory.getLogger(DashboardService.class);

    /**
     * Retorna la tasa BCV del día.
     */
    public double getTasaBCV() {
        return BCVService.getBCVRateSafe();
    }

    /**
     * Retorna el top 5 de servicios más vendidos en un rango de fechas.
     * Retorna un mapa: "Nombre del Servicio" -> Cantidad Vendida
     */
    public Map<String, Integer> getTopServicios(LocalDate desde, LocalDate hasta) throws DatabaseException {
        String sql = """
            SELECT s.nombre as servicio, COUNT(vi.id) as cantidad
            FROM venta_items vi
            JOIN ventas v ON vi.venta_id = v.id
            JOIN servicios s ON vi.servicio_id = s.id
            WHERE date(v.fecha_venta) BETWEEN date(?) AND date(?)
            GROUP BY s.id
            ORDER BY cantidad DESC
            LIMIT 5
            """;
            
        Map<String, Integer> top = new LinkedHashMap<>();
        
        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, desde.toString());
            pstmt.setString(2, hasta.toString());
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    top.put(rs.getString("servicio"), rs.getInt("cantidad"));
                }
            }
        } catch (SQLException e) {
            logger.error("Error al obtener Top 5 Servicios", e);
            throw DatabaseException.queryFailed("GET_TOP_SERVICIOS", e);
        }
        
        return top;
    }

    /**
     * Retorna la producción bruta (suma de precios de venta) agrupada por trabajadora
     * en un rango de fechas.
     */
    public Map<String, Double> getProduccionPorTrabajadora(LocalDate desde, LocalDate hasta) throws DatabaseException {
        String sql = """
            SELECT (t.nombres || ' ' || t.apellidos) as trabajadora, SUM(vi.precio_venta) as total_produccion
            FROM venta_items vi
            JOIN ventas v ON vi.venta_id = v.id
            JOIN trabajadoras t ON vi.trabajadora_id = t.id
            WHERE date(v.fecha_venta) BETWEEN date(?) AND date(?)
            GROUP BY t.id
            ORDER BY total_produccion DESC
            """;
            
        Map<String, Double> produccion = new LinkedHashMap<>();
        
        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, desde.toString());
            pstmt.setString(2, hasta.toString());
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    produccion.put(rs.getString("trabajadora"), rs.getDouble("total_produccion"));
                }
            }
        } catch (SQLException e) {
            logger.error("Error al obtener producción por trabajadora", e);
            throw DatabaseException.queryFailed("GET_PRODUCCION_TRABAJADORA", e);
        }
        
        return produccion;
    }

    /**
     * Retorna el ingreso total de la empresa en un rango de fechas
     * (Suma de la tabla pagos, convertido a USD).
     */
    public double getIngresoTotal(LocalDate desde, LocalDate hasta) throws DatabaseException {
        String sql = """
            SELECT SUM(
                CASE 
                    WHEN p.moneda = 'Bs' THEN p.monto / NULLIF(p.tasa_bcv_al_pago, 0)
                    ELSE p.monto 
                END
            ) as total_usd
            FROM venta_pagos p
            JOIN ventas v ON p.venta_id = v.id
            WHERE date(v.fecha_venta) BETWEEN date(?) AND date(?)
            """;
            
        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, desde.toString());
            pstmt.setString(2, hasta.toString());
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble("total_usd");
                }
            }
        } catch (SQLException e) {
            logger.error("Error al obtener Ingreso Total", e);
            throw DatabaseException.queryFailed("GET_INGRESO_TOTAL", e);
        }
        
        return 0.0;
    }
}
