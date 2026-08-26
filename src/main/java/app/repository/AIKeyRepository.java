package app.repository;

import app.db.DatabaseConnection;
import app.exception.DatabaseException;
import app.model.AIKey;
import app.model.AIStatsDTO;
import app.model.AIUsageLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Repositorio para la gestión del pool de API Keys de Google Gemini y registro de métricas de uso.
 */
public class AIKeyRepository {

    private static final Logger logger = LoggerFactory.getLogger(AIKeyRepository.class);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final AppSettingsRepository settingsRepo = new AppSettingsRepository();

    public AIKeyRepository() {
        checkAndResetDailyCounters();
    }

    /**
     * Verifica y reinicia los contadores diarios si la fecha cambió.
     */
    public synchronized void checkAndResetDailyCounters() {
        String today = LocalDate.now().format(DATE_FMT);
        String sql = "UPDATE ai_api_keys SET requests_today = 0, last_reset_date = ? WHERE last_reset_date IS NULL OR last_reset_date != ?";
        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, today);
            pstmt.setString(2, today);
            int updated = pstmt.executeUpdate();
            if (updated > 0) {
                logger.info("Contadores diarios de IA reiniciados para {} ({} claves actualizadas)", today, updated);
            }
        } catch (Exception e) {
            logger.warn("No se pudo verificar el reinicio diario de contadores de IA: {}", e.getMessage());
        }
    }

    /**
     * Obtiene todas las claves configuradas.
     */
    public List<AIKey> findAll() throws DatabaseException {
        checkAndResetDailyCounters();
        List<AIKey> list = new ArrayList<>();
        String sql = "SELECT * FROM ai_api_keys ORDER BY is_active DESC, id ASC";

        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            logger.error("Error al listar API Keys", e);
            throw DatabaseException.queryFailed("FIND_ALL_AI_KEYS", e);
        }
        return list;
    }

    /**
     * Obtiene solo las claves activas.
     */
    public List<AIKey> findActive() throws DatabaseException {
        checkAndResetDailyCounters();
        List<AIKey> list = new ArrayList<>();
        String sql = "SELECT * FROM ai_api_keys WHERE is_active = 1 ORDER BY requests_today ASC, id ASC";

        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            logger.error("Error al listar API Keys activas", e);
            throw DatabaseException.queryFailed("FIND_ACTIVE_AI_KEYS", e);
        }
        return list;
    }

    /**
     * Guarda una clave nueva o actualiza una existente.
     */
    public void save(AIKey key) throws DatabaseException {
        if (key.getId() <= 0) {
            // Insertar
            String sql = """
                INSERT INTO ai_api_keys (label, api_key, is_active, requests_today, total_requests, last_reset_date, last_status, last_error)
                VALUES (?, ?, ?, 0, 0, ?, 'PENDIENTE', NULL)
            """;
            String encryptedKey = app.util.CryptoUtil.encrypt(key.getApiKey());
            try (Connection conn = DatabaseConnection.connect();
                 PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

                pstmt.setString(1, key.getLabel());
                pstmt.setString(2, encryptedKey);
                pstmt.setInt(3, key.isActive() ? 1 : 0);
                pstmt.setString(4, LocalDate.now().format(DATE_FMT));

                pstmt.executeUpdate();
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        key.setId(generatedKeys.getInt(1));
                    }
                }
            } catch (SQLException e) {
                logger.error("Error al insertar API Key: {}", key.getLabel(), e);
                throw DatabaseException.queryFailed("INSERT_AI_KEY", e);
            }
        } else {
            // Actualizar
            String sql = """
                UPDATE ai_api_keys
                SET label = ?, api_key = ?, is_active = ?
                WHERE id = ?
            """;
            String encryptedKey = app.util.CryptoUtil.encrypt(key.getApiKey());
            try (Connection conn = DatabaseConnection.connect();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {

                pstmt.setString(1, key.getLabel());
                pstmt.setString(2, encryptedKey);
                pstmt.setInt(3, key.isActive() ? 1 : 0);
                pstmt.setInt(4, key.getId());

                pstmt.executeUpdate();
            } catch (SQLException e) {
                logger.error("Error al actualizar API Key ID: {}", key.getId(), e);
                throw DatabaseException.queryFailed("UPDATE_AI_KEY", e);
            }
        }
    }

    /**
     * Elimina una clave por su ID.
     */
    public void delete(int id) throws DatabaseException {
        String sql = "DELETE FROM ai_api_keys WHERE id = ?";
        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error al eliminar API Key ID: {}", id, e);
            throw DatabaseException.queryFailed("DELETE_AI_KEY", e);
        }
    }

    /**
     * Activa o desactiva una clave.
     */
    public void toggleActive(int id, boolean active) throws DatabaseException {
        String sql = "UPDATE ai_api_keys SET is_active = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, active ? 1 : 0);
            pstmt.setInt(2, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error al alternar estado de API Key ID: {}", id, e);
            throw DatabaseException.queryFailed("TOGGLE_ACTIVE_AI_KEY", e);
        }
    }

    /**
     * Actualiza el estado y mensaje de error de una clave específica.
     */
    public void updateKeyStatus(int id, String status, String error) {
        String sql = "UPDATE ai_api_keys SET last_status = ?, last_error = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, status);
            pstmt.setString(2, error);
            pstmt.setInt(3, id);
            pstmt.executeUpdate();
        } catch (Exception e) {
            logger.warn("No se pudo actualizar estado de API Key {}: {}", id, e.getMessage());
        }
    }

    /**
     * Registra un escaneo o llamada a la API (incrementa contadores y guarda en log).
     */
    public void recordUsage(Integer keyId, String keyLabel, String model, long durationMs, String status, String error) {
        String nowStr = LocalDateTime.now().format(DATETIME_FMT);
        String today = LocalDate.now().format(DATE_FMT);

        // 1. Registrar en ai_usage_logs
        String sqlLog = """
            INSERT INTO ai_usage_logs (key_id, key_label, model_used, duration_ms, status, error_message, timestamp)
            VALUES (?, ?, ?, ?, ?, ?, ?)
        """;

        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(sqlLog)) {

            if (keyId != null && keyId > 0) {
                pstmt.setInt(1, keyId);
            } else {
                pstmt.setNull(1, Types.INTEGER);
            }
            pstmt.setString(2, keyLabel);
            pstmt.setString(3, model);
            pstmt.setLong(4, durationMs);
            pstmt.setString(5, status);
            pstmt.setString(6, error);
            pstmt.setString(7, nowStr);

            pstmt.executeUpdate();
        } catch (Exception e) {
            logger.warn("Error al registrar log de uso de IA: {}", e.getMessage());
        }

        // 2. Incrementar contadores en ai_api_keys
        if (keyId != null && keyId > 0) {
            String sqlKey = """
                UPDATE ai_api_keys
                SET requests_today = requests_today + 1,
                    total_requests = total_requests + 1,
                    last_used_at = ?,
                    last_status = ?,
                    last_error = ?,
                    last_reset_date = ?
                WHERE id = ?
            """;

            try (Connection conn = DatabaseConnection.connect();
                 PreparedStatement pstmt = conn.prepareStatement(sqlKey)) {
                pstmt.setString(1, nowStr);
                pstmt.setString(2, status);
                pstmt.setString(3, error);
                pstmt.setString(4, today);
                pstmt.setInt(5, keyId);
                pstmt.executeUpdate();
            } catch (Exception e) {
                logger.warn("Error al actualizar contadores de API Key {}: {}", keyId, e.getMessage());
            }
        }
    }

    /**
     * Obtiene el DTO de métricas y estadísticas consolidadas.
     */
    public AIStatsDTO getUsageStats() {
        checkAndResetDailyCounters();
        AIStatsDTO stats = new AIStatsDTO();

        try {
            List<AIKey> activeKeys = findActive();
            stats.setActiveKeys(activeKeys);
            stats.setDailyCapacity(activeKeys.size() * 1500);

            int totalToday = 0;
            for (AIKey k : activeKeys) {
                totalToday += k.getRequestsToday();
            }
            stats.setRequestsToday(totalToday);

            // Modelo actualmente seleccionado
            String currentModel = settingsRepo.getSetting("gemini_selected_model");
            stats.setCurrentSelectedModel(currentModel != null && !currentModel.isBlank() ? currentModel : "Automático (gemini-3.6-flash / 2.5-flash)");

            // Consultar agregaciones históricas de logs
            String sqlMetrics = """
                SELECT 
                    COUNT(*) as total,
                    SUM(CASE WHEN status = 'SUCCESS' THEN 1 ELSE 0 END) as success_cnt,
                    SUM(CASE WHEN status != 'SUCCESS' THEN 1 ELSE 0 END) as err_cnt,
                    AVG(duration_ms) as avg_duration,
                    MAX(timestamp) as last_time
                FROM ai_usage_logs
            """;

            try (Connection conn = DatabaseConnection.connect();
                 PreparedStatement pstmt = conn.prepareStatement(sqlMetrics);
                 ResultSet rs = pstmt.executeQuery()) {

                if (rs.next()) {
                    int total = rs.getInt("total");
                    int success = rs.getInt("success_cnt");
                    int err = rs.getInt("err_cnt");
                    double avgMs = rs.getDouble("avg_duration");
                    String lastTime = rs.getString("last_time");

                    stats.setTotalHistoricalRequests(total);
                    stats.setSuccessCount(success);
                    stats.setErrorCount(err);
                    stats.setAvgDurationMs(avgMs);
                    stats.setLastUsedTimestamp(lastTime != null ? lastTime : "Ninguno aún");
                    stats.setSuccessRate(total > 0 ? ((double) success / total) * 100.0 : 100.0);
                }
            }

            // Últimos 20 logs
            List<AIUsageLog> logs = new ArrayList<>();
            String sqlLogs = "SELECT * FROM ai_usage_logs ORDER BY id DESC LIMIT 20";
            try (Connection conn = DatabaseConnection.connect();
                 PreparedStatement pstmt = conn.prepareStatement(sqlLogs);
                 ResultSet rs = pstmt.executeQuery()) {

                while (rs.next()) {
                    AIUsageLog log = new AIUsageLog();
                    log.setId(rs.getInt("id"));
                    int kId = rs.getInt("key_id");
                    log.setKeyId(rs.wasNull() ? null : kId);
                    log.setKeyLabel(rs.getString("key_label"));
                    log.setModelUsed(rs.getString("model_used"));
                    log.setDurationMs(rs.getLong("duration_ms"));
                    log.setStatus(rs.getString("status"));
                    log.setErrorMessage(rs.getString("error_message"));
                    
                    String ts = rs.getString("timestamp");
                    if (ts != null) {
                        try {
                            log.setTimestamp(LocalDateTime.parse(ts.replace(" ", "T")));
                        } catch (Exception ignored) {}
                    }
                    logs.add(log);
                }
            }
            stats.setRecentLogs(logs);

        } catch (Exception e) {
            logger.error("Error al calcular estadísticas de IA", e);
        }

        return stats;
    }

    private AIKey mapRow(ResultSet rs) throws SQLException {
        AIKey k = new AIKey();
        k.setId(rs.getInt("id"));
        k.setLabel(rs.getString("label"));
        
        String rawKey = rs.getString("api_key");
        String decryptedKey = app.util.CryptoUtil.decrypt(rawKey);
        k.setApiKey(decryptedKey);

        // Auto-migración a cifrado si estaba en texto plano
        if (rawKey != null && !rawKey.startsWith("ENC:") && !rawKey.isBlank()) {
            try {
                String encrypted = app.util.CryptoUtil.encrypt(rawKey);
                try (Connection conn2 = DatabaseConnection.connect();
                     PreparedStatement pstmt2 = conn2.prepareStatement("UPDATE ai_api_keys SET api_key = ? WHERE id = ?")) {
                    pstmt2.setString(1, encrypted);
                    pstmt2.setInt(2, k.getId());
                    pstmt2.executeUpdate();
                }
            } catch (Exception ignored) {}
        }

        k.setActive(rs.getInt("is_active") == 1);
        k.setRequestsToday(rs.getInt("requests_today"));
        k.setTotalRequests(rs.getInt("total_requests"));
        k.setLastResetDate(rs.getString("last_reset_date"));
        k.setLastStatus(rs.getString("last_status"));
        k.setLastError(rs.getString("last_error"));

        String lastUsed = rs.getString("last_used_at");
        if (lastUsed != null && !lastUsed.isBlank()) {
            try {
                k.setLastUsedAt(LocalDateTime.parse(lastUsed.replace(" ", "T")));
            } catch (Exception ignored) {}
        }

        String created = rs.getString("created_at");
        if (created != null && !created.isBlank()) {
            try {
                k.setCreatedAt(LocalDateTime.parse(created.replace(" ", "T")));
            } catch (Exception ignored) {}
        }
        return k;
    }
}
