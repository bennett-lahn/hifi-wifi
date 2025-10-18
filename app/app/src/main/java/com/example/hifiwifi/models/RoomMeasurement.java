package com.example.hifiwifi.models;

import java.util.Date;

/**
 * Data model representing a single WiFi measurement for a specific room
 */
public class RoomMeasurement {
    private String roomId;
    private String roomName;
    private long timestamp;
    private int signalStrengthDbm;
    private int latencyMs;
    private double bandwidthMbps;
    private String activityType; // "gaming", "streaming", "video_call", "general"

    public RoomMeasurement() {
        // Default constructor
    }

    public RoomMeasurement(String roomId, String roomName, int signalStrengthDbm, 
                          int latencyMs, double bandwidthMbps, String activityType) {
        this.roomId = roomId;
        this.roomName = roomName;
        this.timestamp = System.currentTimeMillis();
        this.signalStrengthDbm = signalStrengthDbm;
        this.latencyMs = latencyMs;
        this.bandwidthMbps = bandwidthMbps;
        this.activityType = activityType;
    }

    // Getters and Setters
    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public String getRoomName() {
        return roomName;
    }

    public void setRoomName(String roomName) {
        this.roomName = roomName;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public int getSignalStrengthDbm() {
        return signalStrengthDbm;
    }

    public void setSignalStrengthDbm(int signalStrengthDbm) {
        this.signalStrengthDbm = signalStrengthDbm;
    }

    public int getLatencyMs() {
        return latencyMs;
    }

    public void setLatencyMs(int latencyMs) {
        this.latencyMs = latencyMs;
    }

    public double getBandwidthMbps() {
        return bandwidthMbps;
    }

    public void setBandwidthMbps(double bandwidthMbps) {
        this.bandwidthMbps = bandwidthMbps;
    }

    public String getActivityType() {
        return activityType;
    }

    public void setActivityType(String activityType) {
        this.activityType = activityType;
    }
}
