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

    // API pública para tasa Euro Oficial (prioritaria) y Dólar BCV (dolarapi.com)
    private static final String EURO_API_URL = "https://ve.dolarapi.com/v1/euros/oficial";
    private static final String EURO_HISTORICO_URL_PREFIX = "https://ve.dolarapi.com/v1/historicos/euros/oficial/";
    private static final String BCV_API_URL = "https://ve.dolarapi.com/v1/dolares/oficial";
    private static final int TIMEOUT_MS = 4_000;
    private static final double DEFAULT_RATE = 916.00;

    private static final String SQL_GET_SETTING = "SELECT setting_value FROM app_settings WHERE setting_key = ?";
    private static final String SQL_UPSERT_SETTING = "INSERT OR REPLACE INTO app_settings (setting_key, setting_value) VALUES (?, ?)";

    /**
     * Obtiene la tasa del Euro Oficial actual o histórica según la fecha proporcionada.
     * Si no se especifica fecha o es hoy, consulta el endpoint actual del Euro.
     * Si la fecha es histórica, consulta el endpoint de históricos de dolarapi.com.
     *
     * @param fechaStr Fecha en formatos: "YYYY-MM-DD", "DD-MM-YYYY", "DD/MM/YYYY", "DD-MM-YY", "DD/MM/YY"
     * @return Tasa del Euro Oficial o fallback a tasa cacheada/BCV
     */
    public static double getEuroOficialRate(String fechaStr) {
        String formattedDate = normalizeDateForEuroApi(fechaStr);
        String targetUrl;

        if (formattedDate == null || formattedDate.isBlank()) {
            targetUrl = EURO_API_URL;
        } else {
            targetUrl = EURO_HISTORICO_URL_PREFIX + formattedDate;
        }

        try {
            double rate = fetchRateFromUrl(targetUrl);
            logger.info("Tasa Euro Oficial obtenida ({}) de API: {}", targetUrl, rate);
            return rate;
        } catch (Exception e) {
            logger.warn("No se pudo obtener tasa Euro de {}: {}. Intentando Euro actual o BCV...", targetUrl, e.getMessage());
            try {
                if (!targetUrl.equals(EURO_API_URL)) {
                    return fetchRateFromUrl(EURO_API_URL);
                }
            } catch (Exception ex) {
                logger.warn("Fallo también Euro actual: {}", ex.getMessage());
            }
            return getBCVRateSafe();
        }
    }

    /**
     * Normaliza cualquier formato de fecha a 'YYYY/MM/DD' para el endpoint de dolarapi.
     */
    public static String normalizeDateForEuroApi(String rawDate) {
        if (rawDate == null || rawDate.isBlank()) return null;
        String clean = rawDate.trim().replace(".", "-").replace("/", "-");
        String[] parts = clean.split("-");

        if (parts.length != 3) return null;

        try {
            int p1 = Integer.parseInt(parts[0].trim());
            int p2 = Integer.parseInt(parts[1].trim());
            int p3 = Integer.parseInt(parts[2].trim());

            int year, month, day;

            if (p1 > 1000) {
                // Formato YYYY-MM-DD
                year = p1;
                month = p2;
                day = p3;
            } else {
                // Formato DD-MM-YY o DD-MM-YYYY
                day = p1;
                month = p2;
                year = p3;
                if (year < 100) {
                    // Año de 2 dígitos (ej: 24 -> 2024)
                    year = 2000 + year;
                }
            }

            // Validar límites básicos
            if (month < 1 || month > 12 || day < 1 || day > 31 || year < 2000 || year > 2100) {
                return null;
            }

            return String.format("%04d/%02d/%02d", year, month, day);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Obtiene la tasa del Euro Oficial actual.
     * Intenta la API del Euro primero; si falla, usa la última tasa guardada en la DB.
     */
    public static double getBCVRate() {
        try {
            double rate = fetchFromAPI();
            // Persistir la tasa obtenida en app_settings para uso futuro
            saveSetting("tasa_euro_oficial", String.valueOf(rate));
            saveSetting("tasa_bcv", String.valueOf(rate));
            logger.info("Tasa Euro Oficial obtenida de API: {}", rate);
            return rate;
        } catch (Exception e) {
            logger.warn("No se pudo obtener tasa Euro Oficial de API, usando tasa guardada: {}", e.getMessage());
            return getCachedRate();
        }
    }

    /**
     * Obtiene la tasa del Euro Oficial de forma segura (nunca lanza excepción).
     */
    public static double getBCVRateSafe() {
        try {
            return getBCVRate();
        } catch (Exception e) {
            logger.error("Error inesperado al obtener tasa Euro Oficial", e);
            return DEFAULT_RATE;
        }
    }

    /**
     * Retorna la última tasa del Euro Oficial guardada en la DB o la tasa por defecto.
     */
    public static double getCachedRate() {
        try {
            String cachedEuro = getSetting("tasa_euro_oficial");
            if (cachedEuro != null && !cachedEuro.equals("0.0")) {
                double rate = Double.parseDouble(cachedEuro);
                logger.debug("Tasa Euro Oficial cacheada: {}", rate);
                return rate;
            }

            String cached = getSetting("tasa_bcv");
            if (cached != null && !cached.equals("0.0")) {
                double rate = Double.parseDouble(cached);
                logger.debug("Tasa de respaldo cacheada: {}", rate);
                return rate;
            }
        } catch (Exception e) {
            logger.warn("Error leyendo tasa cacheada", e);
        }
        return DEFAULT_RATE;
    }

    /**
     * Consulta la API de tasa Euro Oficial (con fallback a Dólar BCV si es necesario).
     */
    private static double fetchFromAPI() throws IOException {
        try {
            return fetchRateFromUrl(EURO_API_URL);
        } catch (Exception e) {
            logger.warn("Fallo endpoint Euro ({}), recurriendo a endpoint Dólar...", e.getMessage());
            return fetchRateFromUrl(BCV_API_URL);
        }
    }

    /**
     * Realiza la petición HTTP y extrae el valor 'promedio' de la respuesta JSON.
     */
    private static double fetchRateFromUrl(String urlString) throws IOException {
        logger.debug("Consultando API Tasa: {}", urlString);

        HttpURLConnection conn = (HttpURLConnection) URI.create(urlString).toURL().openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(TIMEOUT_MS);
        conn.setReadTimeout(TIMEOUT_MS);
        conn.setRequestProperty("User-Agent", "Mozilla/5.0");
        conn.connect();

        int responseCode = conn.getResponseCode();
        if (responseCode != 200) {
            throw new IOException("API Tasa (" + urlString + ") respondió con código: " + responseCode);
        }

        StringBuilder response = new StringBuilder();
        try (Scanner scanner = new Scanner(conn.getInputStream())) {
            while (scanner.hasNext()) {
                response.append(scanner.nextLine());
            }
        }

        JSONObject data = new JSONObject(response.toString());
        if (data.has("promedio") && !data.isNull("promedio")) {
            return data.getDouble("promedio");
        } else if (data.has("venta") && !data.isNull("venta")) {
            return data.getDouble("venta");
        } else if (data.has("compra") && !data.isNull("compra")) {
            return data.getDouble("compra");
        }

        throw new IOException("JSON de respuesta no contiene campo de tasa válido: " + response);
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
