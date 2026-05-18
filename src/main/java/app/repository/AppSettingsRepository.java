package app.repository;

import app.db.DatabaseConnection;
import app.exception.DatabaseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;

/**
 * CORRECCIÓN #14: Repositorio centralizado para app_settings.
 * Reemplaza el SQL directo que existía en VentaService y BCVService.
 *
 * Tabla: app_settings(setting_key TEXT PK, setting_value TEXT)
 */
public class AppSettingsRepository {

    private static final Logger logger = LoggerFactory.getLogger(AppSettingsRepository.class);

    private static final String SQL_GET =
        "SELECT setting_value FROM app_settings WHERE setting_key = ?";

    private static final String SQL_SET =
        "UPDATE app_settings SET setting_value = ? WHERE setting_key = ?";

    private static final String SQL_INSERT =
        "INSERT OR IGNORE INTO app_settings (setting_key, setting_value) VALUES (?, ?)";

    /**
     * Obtiene un valor de configuración por su clave.
     *
     * @param key Clave de la configuración
     * @return Valor como String, o null si no existe
     */
    public String getSetting(String key) throws DatabaseException {
        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(SQL_GET)) {

            pstmt.setString(1, key);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("setting_value");
                }
            }
        } catch (SQLException e) {
            logger.error("Error al leer setting: {}", key, e);
            throw DatabaseException.queryFailed("GET_SETTING:" + key, e);
        }
        return null;
    }

    /**
     * Establece un valor de configuración. Si la clave no existe, la crea.
     *
     * @param key   Clave de la configuración
     * @param value Valor a establecer
     */
    public void setSetting(String key, String value) throws DatabaseException {
        try (Connection conn = DatabaseConnection.connect()) {
            // Intentar INSERT OR IGNORE primero (para crear si no existe)
            try (PreparedStatement pstmt = conn.prepareStatement(SQL_INSERT)) {
                pstmt.setString(1, key);
                pstmt.setString(2, value);
                pstmt.executeUpdate();
            }
            // Luego UPDATE para asegurar el valor correcto
            try (PreparedStatement pstmt = conn.prepareStatement(SQL_SET)) {
                pstmt.setString(1, value);
                pstmt.setString(2, key);
                pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            logger.error("Error al guardar setting: {}={}", key, value, e);
            throw DatabaseException.queryFailed("SET_SETTING:" + key, e);
        }
    }

    /**
     * Obtiene un valor entero de configuración con valor por defecto.
     *
     * @param key          Clave de la configuración
     * @param defaultValue Valor por defecto si no existe o no es parseable
     * @return Valor entero
     */
    public int getSettingAsInt(String key, int defaultValue) throws DatabaseException {
        String val = getSetting(key);
        if (val == null) return defaultValue;
        try {
            return Integer.parseInt(val);
        } catch (NumberFormatException e) {
            logger.warn("Setting '{}' no es un entero válido: '{}'. Usando default: {}", key, val, defaultValue);
            return defaultValue;
        }
    }

    /**
     * Obtiene un valor double de configuración con valor por defecto.
     *
     * @param key          Clave de la configuración
     * @param defaultValue Valor por defecto si no existe o no es parseable
     * @return Valor double
     */
    public double getSettingAsDouble(String key, double defaultValue) throws DatabaseException {
        String val = getSetting(key);
        if (val == null) return defaultValue;
        try {
            return Double.parseDouble(val);
        } catch (NumberFormatException e) {
            logger.warn("Setting '{}' no es un double válido: '{}'. Usando default: {}", key, val, defaultValue);
            return defaultValue;
        }
    }
}
