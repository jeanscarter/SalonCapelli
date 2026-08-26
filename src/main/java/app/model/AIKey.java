package app.model;

import java.time.LocalDateTime;

/**
 * Representa una API Key de Google Gemini configurada en el sistema.
 */
public class AIKey {

    private int id;
    private String label;
    private String apiKey;
    private boolean active;
    private int requestsToday;
    private int totalRequests;
    private String lastResetDate;
    private String lastStatus;
    private String lastError;
    private LocalDateTime lastUsedAt;
    private LocalDateTime createdAt;

    public AIKey() {
        this.active = true;
        this.requestsToday = 0;
        this.totalRequests = 0;
        this.lastStatus = "PENDIENTE";
    }

    public AIKey(String label, String apiKey) {
        this();
        this.label = label;
        this.apiKey = apiKey;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    /**
     * Retorna la clave enmascarada para mostrar en UI (ej: AIzaSy...9x1A).
     */
    public String getMaskedKey() {
        if (apiKey == null || apiKey.length() < 10) {
            return "••••••••••";
        }
        return apiKey.substring(0, 6) + "••••••••" + apiKey.substring(apiKey.length() - 4);
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public int getRequestsToday() {
        return requestsToday;
    }

    public void setRequestsToday(int requestsToday) {
        this.requestsToday = requestsToday;
    }

    public int getTotalRequests() {
        return totalRequests;
    }

    public void setTotalRequests(int totalRequests) {
        this.totalRequests = totalRequests;
    }

    public String getLastResetDate() {
        return lastResetDate;
    }

    public void setLastResetDate(String lastResetDate) {
        this.lastResetDate = lastResetDate;
    }

    public String getLastStatus() {
        return lastStatus;
    }

    public void setLastStatus(String lastStatus) {
        this.lastStatus = lastStatus;
    }

    public String getLastError() {
        return lastError;
    }

    public void setLastError(String lastError) {
        this.lastError = lastError;
    }

    public LocalDateTime getLastUsedAt() {
        return lastUsedAt;
    }

    public void setLastUsedAt(LocalDateTime lastUsedAt) {
        this.lastUsedAt = lastUsedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return label != null && !label.isBlank() ? label : getMaskedKey();
    }
}
