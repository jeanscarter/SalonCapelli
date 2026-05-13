package app.service;

import app.db.DatabaseConnection;
import app.exception.DatabaseException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Scanner;

/**
 * Servicio para obtener y gestionar la tasa de cambio BCV (Banco Central de Venezuela).
 * Migrado de LEGACY (CapelliSalesWindow) con las siguientes mejoras:
 * - Persistencia en app_settings para cachear la última tasa conocida
 * - Logging con SLF4J en lugar de java.util.logging
 * - Configuración de API interna (sin AppConfig externo)
 */
public class BCVService {

    private static final Logger logger = LoggerFactory.getLogger(BCVService.class);

    // API pública para tasa BCV (misma que LEGACY)
    private static final String BCV_API_URL = "https://ve.dolarapi.com/v1/dolares/oficial";
    private static final int TIMEOUT_MS = 10_000;
    private static final double DEFAULT_RATE = 508.60;

    private static final String SQL_GET_SETTING = "SELECT setting_value FROM app_settings WHERE setting_key = ?";
    private static final String SQL_UPSERT_SETTING = "INSERT OR REPLACE INTO app_settings (setting_key, setting_value) VALUES (?, ?)";

    /**
     * Obtiene la tasa BCV actual.
     * Intenta la API primero; si falla, usa la última tasa guardada en la DB.
     */
    public static double getBCVRate() {
        try {
            double rate = fetchFromAPI();
            // Persistir la tasa obtenida en app_settings para uso futuro
            saveSetting("tasa_bcv", String.valueOf(rate));
            logger.info("Tasa BCV obtenida de API: {}", rate);
            return rate;
        } catch (Exception e) {
            logger.warn("No se pudo obtener tasa BCV de API, usando tasa guardada: {}", e.getMessage());
            return getCachedRate();
        }
    }

    /**
     * Obtiene la tasa BCV de forma segura (nunca lanza excepción)
     */
    public static double getBCVRateSafe() {
        try {
            return getBCVRate();
        } catch (Exception e) {
            logger.error("Error inesperado al obtener tasa BCV", e);
            return DEFAULT_RATE;
        }
    }

    /**
     * Retorna la última tasa guardada en la DB o la tasa por defecto
     */
    public static double getCachedRate() {
        try {
            String cached = getSetting("tasa_bcv");
            if (cached != null && !cached.equals("0.0")) {
                double rate = Double.parseDouble(cached);
                logger.debug("Tasa BCV cacheada: {}", rate);
                return rate;
            }
        } catch (Exception e) {
            logger.warn("Error leyendo tasa cacheada", e);
        }
        return DEFAULT_RATE;
    }

    /**
     * Consulta la API de tasa BCV
     */
    private static double fetchFromAPI() throws IOException {
        logger.debug("Consultando API BCV: {}", BCV_API_URL);

        HttpURLConnection conn = (HttpURLConnection) URI.create(BCV_API_URL).toURL().openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(TIMEOUT_MS);
        conn.setReadTimeout(TIMEOUT_MS);
        conn.setRequestProperty("User-Agent", "Mozilla/5.0");
        conn.connect();

        int responseCode = conn.getResponseCode();
        if (responseCode != 200) {
            throw new IOException("API BCV respondió con código: " + responseCode);
        }

        StringBuilder response = new StringBuilder();
        try (Scanner scanner = new Scanner(conn.getInputStream())) {
            while (scanner.hasNext()) {
                response.append(scanner.nextLine());
            }
        }

        JSONObject data = new JSONObject(response.toString());
        return data.getDouble("promedio");
    }

    // ===== Settings helpers =====

    private static String getSetting(String key) {
        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(SQL_GET_SETTING)) {
            pstmt.setString(1, key);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("setting_value");
                }
            }
        } catch (SQLException | DatabaseException e) {
            logger.warn("Error leyendo setting '{}': {}", key, e.getMessage());
        }
        return null;
    }

    private static void saveSetting(String key, String value) {
        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(SQL_UPSERT_SETTING)) {
            pstmt.setString(1, key);
            pstmt.setString(2, value);
            pstmt.executeUpdate();
        } catch (SQLException | DatabaseException e) {
            logger.warn("Error guardando setting '{}': {}", key, e.getMessage());
        }
    }
}
