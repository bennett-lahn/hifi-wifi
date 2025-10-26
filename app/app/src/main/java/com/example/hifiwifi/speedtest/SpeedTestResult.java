package com.example.hifiwifi.speedtest;

/**
 * Data model for storing speed test results.
 * Used for local storage and data transfer between components.
 */
public class SpeedTestResult {
    
    private long timestamp;
    private double speedMbps;
    private String roomLabel;
    private String testId;
    private String errorMessage;
    private boolean success;
    private long bytesDownloaded;
    private long testDurationMs;
    private int latencyMs;
    private double jitterMs;
    private double packetLossPercent;
    
    /**
     * Constructor for successful test result
     */
    public SpeedTestResult(long timestamp, double speedMbps, String roomLabel, String testId) {
        this.timestamp = timestamp;
        this.speedMbps = speedMbps;
        this.roomLabel = roomLabel;
        this.testId = testId;
        this.success = true;
        this.errorMessage = "";
        this.bytesDownloaded = 0;
        this.testDurationMs = 0;
        this.latencyMs = 0;
        this.jitterMs = 0.0;
        this.packetLossPercent = 0.0;
    }
    
    /**
     * Constructor for failed test result
     */
    public SpeedTestResult(long timestamp, String errorMessage, String roomLabel, String testId) {
        this.timestamp = timestamp;
        this.speedMbps = 0.0;
        this.roomLabel = roomLabel;
        this.testId = testId;
        this.success = false;
        this.errorMessage = errorMessage;
        this.bytesDownloaded = 0;
        this.testDurationMs = 0;
        this.latencyMs = 0;
        this.jitterMs = 0.0;
        this.packetLossPercent = 0.0;
    }
    
    /**
     * Full constructor with all fields
     */
    public SpeedTestResult(long timestamp, double speedMbps, String roomLabel, String testId,
                          boolean success, String errorMessage, long bytesDownloaded, long testDurationMs) {
        this.timestamp = timestamp;
        this.speedMbps = speedMbps;
        this.roomLabel = roomLabel;
        this.testId = testId;
        this.success = success;
        this.errorMessage = errorMessage;
        this.bytesDownloaded = bytesDownloaded;
        this.testDurationMs = testDurationMs;
        this.latencyMs = 0;
        this.jitterMs = 0.0;
        this.packetLossPercent = 0.0;
    }
    
    /**
     * Full constructor with network metrics
     */
    public SpeedTestResult(long timestamp, double speedMbps, String roomLabel, String testId,
                          boolean success, String errorMessage, long bytesDownloaded, long testDurationMs,
                          int latencyMs, double jitterMs, double packetLossPercent) {
        this.timestamp = timestamp;
        this.speedMbps = speedMbps;
        this.roomLabel = roomLabel;
        this.testId = testId;
        this.success = success;
        this.errorMessage = errorMessage;
        this.bytesDownloaded = bytesDownloaded;
        this.testDurationMs = testDurationMs;
        this.latencyMs = latencyMs;
        this.jitterMs = jitterMs;
        this.packetLossPercent = packetLossPercent;
    }
    
    // Getters and setters
    public long getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
    
    public double getSpeedMbps() {
        return speedMbps;
    }
    
    public void setSpeedMbps(double speedMbps) {
        this.speedMbps = speedMbps;
    }
    
    public String getRoomLabel() {
        return roomLabel;
    }
    
    public void setRoomLabel(String roomLabel) {
        this.roomLabel = roomLabel;
    }
    
    public String getTestId() {
        return testId;
    }
    
    public void setTestId(String testId) {
        this.testId = testId;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    public long getBytesDownloaded() {
        return bytesDownloaded;
    }
    
    public void setBytesDownloaded(long bytesDownloaded) {
        this.bytesDownloaded = bytesDownloaded;
    }
    
    public long getTestDurationMs() {
        return testDurationMs;
    }
    
    public void setTestDurationMs(long testDurationMs) {
        this.testDurationMs = testDurationMs;
    }
    
    public int getLatencyMs() {
        return latencyMs;
    }
    
    public void setLatencyMs(int latencyMs) {
        this.latencyMs = latencyMs;
    }
    
    public double getJitterMs() {
        return jitterMs;
    }
    
    public void setJitterMs(double jitterMs) {
        this.jitterMs = jitterMs;
    }
    
    public double getPacketLossPercent() {
        return packetLossPercent;
    }
    
    public void setPacketLossPercent(double packetLossPercent) {
        this.packetLossPercent = packetLossPercent;
    }
    
    @Override
    public String toString() {
        return "SpeedTestResult{" +
                "timestamp=" + timestamp +
                ", speedMbps=" + speedMbps +
                ", roomLabel='" + roomLabel + '\'' +
                ", testId='" + testId + '\'' +
                ", success=" + success +
                ", errorMessage='" + errorMessage + '\'' +
                ", bytesDownloaded=" + bytesDownloaded +
                ", testDurationMs=" + testDurationMs +
                ", latencyMs=" + latencyMs +
                ", jitterMs=" + jitterMs +
                ", packetLossPercent=" + packetLossPercent +
                '}';
    }
}
