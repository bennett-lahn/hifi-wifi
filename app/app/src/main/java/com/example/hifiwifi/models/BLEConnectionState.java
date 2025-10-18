package com.example.hifiwifi.models;

/**
 * Data model representing the current BLE connection state
 */
public class BLEConnectionState {
    private boolean isConnected;
    private boolean isConnecting;
    private String deviceName;
    private String errorMessage;

    public BLEConnectionState() {
        this.isConnected = false;
        this.isConnecting = false;
        this.deviceName = "";
        this.errorMessage = "";
    }

    public BLEConnectionState(boolean isConnected, boolean isConnecting, 
                             String deviceName, String errorMessage) {
        this.isConnected = isConnected;
        this.isConnecting = isConnecting;
        this.deviceName = deviceName;
        this.errorMessage = errorMessage;
    }

    // Getters and Setters
    public boolean isConnected() {
        return isConnected;
    }

    public void setConnected(boolean connected) {
        isConnected = connected;
    }

    public boolean isConnecting() {
        return isConnecting;
    }

    public void setConnecting(boolean connecting) {
        isConnecting = connecting;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
