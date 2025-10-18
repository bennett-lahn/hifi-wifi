package com.example.hifiwifi.models;

/**
 * Data model representing the classification result for a room's WiFi quality
 */
public class ClassificationResult {
    private String roomId;
    private String roomName;
    private int gamingScore; // 0-100
    private int streamingScore; // 0-100
    private int videoCallScore; // 0-100
    private String overallGrade; // "A", "B", "C", "D", "F"
    private String zone; // "good", "medium", "poor"

    public ClassificationResult() {
        // Default constructor
    }

    public ClassificationResult(String roomId, String roomName, int gamingScore, 
                               int streamingScore, int videoCallScore, 
                               String overallGrade, String zone) {
        this.roomId = roomId;
        this.roomName = roomName;
        this.gamingScore = gamingScore;
        this.streamingScore = streamingScore;
        this.videoCallScore = videoCallScore;
        this.overallGrade = overallGrade;
        this.zone = zone;
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

    public int getGamingScore() {
        return gamingScore;
    }

    public void setGamingScore(int gamingScore) {
        this.gamingScore = gamingScore;
    }

    public int getStreamingScore() {
        return streamingScore;
    }

    public void setStreamingScore(int streamingScore) {
        this.streamingScore = streamingScore;
    }

    public int getVideoCallScore() {
        return videoCallScore;
    }

    public void setVideoCallScore(int videoCallScore) {
        this.videoCallScore = videoCallScore;
    }

    public String getOverallGrade() {
        return overallGrade;
    }

    public void setOverallGrade(String overallGrade) {
        this.overallGrade = overallGrade;
    }

    public String getZone() {
        return zone;
    }

    public void setZone(String zone) {
        this.zone = zone;
    }
}
