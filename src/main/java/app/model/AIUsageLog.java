package app.model;

import java.time.LocalDateTime;

/**
 * Registro de uso y latencia para cada escaneo / llamada a la API de IA.
 */
public class AIUsageLog {

    private int id;
    private Integer keyId;
    private String keyLabel;
    private String modelUsed;
    private long durationMs;
    private String status; // SUCCESS, ERROR, RATE_LIMIT_429
    private String errorMessage;
    private LocalDateTime timestamp;

    public AIUsageLog() {
        this.timestamp = LocalDateTime.now();
    }

    public AIUsageLog(Integer keyId, String keyLabel, String modelUsed, long durationMs, String status, String errorMessage) {
        this();
        this.keyId = keyId;
        this.keyLabel = keyLabel;
        this.modelUsed = modelUsed;
        this.durationMs = durationMs;
        this.status = status;
        this.errorMessage = errorMessage;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Integer getKeyId() {
        return keyId;
    }

    public void setKeyId(Integer keyId) {
        this.keyId = keyId;
    }

    public String getKeyLabel() {
        return keyLabel;
    }

    public void setKeyLabel(String keyLabel) {
        this.keyLabel = keyLabel;
    }

    public String getModelUsed() {
        return modelUsed;
    }

    public void setModelUsed(String modelUsed) {
        this.modelUsed = modelUsed;
    }

    public long getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(long durationMs) {
        this.durationMs = durationMs;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
