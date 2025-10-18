package com.example.hifiwifi.models;

/**
 * Data model representing current network metrics being measured
 */
public class NetworkMetrics {
    private int currentSignalDbm;
    private int currentLatencyMs;
    private double currentBandwidthMbps;
    private boolean isCollecting;
    private String currentRoomName;

    public NetworkMetrics() {
        this.currentSignalDbm = 0;
        this.currentLatencyMs = 0;
        this.currentBandwidthMbps = 0.0;
        this.isCollecting = false;
        this.currentRoomName = "";
    }

    public NetworkMetrics(int currentSignalDbm, int currentLatencyMs, 
                         double currentBandwidthMbps, boolean isCollecting, String currentRoomName) {
        this.currentSignalDbm = currentSignalDbm;
        this.currentLatencyMs = currentLatencyMs;
        this.currentBandwidthMbps = currentBandwidthMbps;
        this.isCollecting = isCollecting;
        this.currentRoomName = currentRoomName;
    }

    // Getters and Setters
    public int getCurrentSignalDbm() {
        return currentSignalDbm;
    }

    public void setCurrentSignalDbm(int currentSignalDbm) {
        this.currentSignalDbm = currentSignalDbm;
    }

    public int getCurrentLatencyMs() {
        return currentLatencyMs;
    }

    public void setCurrentLatencyMs(int currentLatencyMs) {
        this.currentLatencyMs = currentLatencyMs;
    }

    public double getCurrentBandwidthMbps() {
        return currentBandwidthMbps;
    }

    public void setCurrentBandwidthMbps(double currentBandwidthMbps) {
        this.currentBandwidthMbps = currentBandwidthMbps;
    }

    public boolean isCollecting() {
        return isCollecting;
    }

    public void setCollecting(boolean collecting) {
        isCollecting = collecting;
    }

    public String getCurrentRoomName() {
        return currentRoomName;
    }

    public void setCurrentRoomName(String currentRoomName) {
        this.currentRoomName = currentRoomName;
    }
}
