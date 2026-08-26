package app.model;

import java.util.List;

/**
 * DTO con métricas agregadas de uso y rendimiento de la IA.
 */
public class AIStatsDTO {

    private int requestsToday;
    private int dailyCapacity; // e.g. activeKeys * 1500
    private int totalHistoricalRequests;
    private int successCount;
    private int errorCount;
    private double successRate;
    private double avgDurationMs;
    private String lastUsedTimestamp;
    private String currentSelectedModel;
    private List<AIKey> activeKeys;
    private List<AIUsageLog> recentLogs;

    public int getRequestsToday() {
        return requestsToday;
    }

    public void setRequestsToday(int requestsToday) {
        this.requestsToday = requestsToday;
    }

    public int getDailyCapacity() {
        return dailyCapacity;
    }

    public void setDailyCapacity(int dailyCapacity) {
        this.dailyCapacity = dailyCapacity;
    }

    public int getTotalHistoricalRequests() {
        return totalHistoricalRequests;
    }

    public void setTotalHistoricalRequests(int totalHistoricalRequests) {
        this.totalHistoricalRequests = totalHistoricalRequests;
    }

    public int getSuccessCount() {
        return successCount;
    }

    public void setSuccessCount(int successCount) {
        this.successCount = successCount;
    }

    public int getErrorCount() {
        return errorCount;
    }

    public void setErrorCount(int errorCount) {
        this.errorCount = errorCount;
    }

    public double getSuccessRate() {
        return successRate;
    }

    public void setSuccessRate(double successRate) {
        this.successRate = successRate;
    }

    public double getAvgDurationMs() {
        return avgDurationMs;
    }

    public void setAvgDurationMs(double avgDurationMs) {
        this.avgDurationMs = avgDurationMs;
    }

    public String getLastUsedTimestamp() {
        return lastUsedTimestamp;
    }

    public void setLastUsedTimestamp(String lastUsedTimestamp) {
        this.lastUsedTimestamp = lastUsedTimestamp;
    }

    public String getCurrentSelectedModel() {
        return currentSelectedModel;
    }

    public void setCurrentSelectedModel(String currentSelectedModel) {
        this.currentSelectedModel = currentSelectedModel;
    }

    public List<AIKey> getActiveKeys() {
        return activeKeys;
    }

    public void setActiveKeys(List<AIKey> activeKeys) {
        this.activeKeys = activeKeys;
    }

    public List<AIUsageLog> getRecentLogs() {
        return recentLogs;
    }

    public void setRecentLogs(List<AIUsageLog> recentLogs) {
        this.recentLogs = recentLogs;
    }
}
