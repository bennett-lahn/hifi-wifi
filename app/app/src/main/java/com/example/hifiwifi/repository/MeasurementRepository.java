package com.example.hifiwifi.repository;

import com.example.hifiwifi.models.ClassificationResult;
import com.example.hifiwifi.models.RoomMeasurement;

import java.util.ArrayList;
import java.util.List;

/**
 * Repository for managing WiFi measurements and classifications
 * Stores data in-memory for current session only
 */
public class MeasurementRepository {
    
    private List<RoomMeasurement> measurements;
    private List<ClassificationResult> classifications;
    
    public MeasurementRepository() {
        this.measurements = new ArrayList<>();
        this.classifications = new ArrayList<>();
    }
    
    /**
     * Add a new measurement
     */
    public void addMeasurement(RoomMeasurement measurement) {
        measurements.add(measurement);
        
        // Automatically classify the room after adding measurement
        ClassificationResult classification = classifyRoom(measurement);
        if (classification != null) {
            // Remove existing classification for this room if any
            classifications.removeIf(c -> c.getRoomId().equals(measurement.getRoomId()));
            classifications.add(classification);
        }
    }
    
    /**
     * Get all measurements
     */
    public List<RoomMeasurement> getMeasurements() {
        return new ArrayList<>(measurements);
    }
    
    /**
     * Get measurements for a specific room
     */
    public List<RoomMeasurement> getMeasurementsForRoom(String roomId) {
        List<RoomMeasurement> roomMeasurements = new ArrayList<>();
        for (RoomMeasurement measurement : measurements) {
            if (measurement.getRoomId().equals(roomId)) {
                roomMeasurements.add(measurement);
            }
        }
        return roomMeasurements;
    }
    
    /**
     * Classify a room based on its measurements
     * TODO: Implement proper weighted scoring algorithm
     */
    public ClassificationResult classifyRoom(RoomMeasurement measurement) {
        // TODO: Implement sophisticated classification algorithm
        // For now, return a mock classification based on simple thresholds
        
        int signalStrength = measurement.getSignalStrengthDbm();
        int latency = measurement.getLatencyMs();
        double bandwidth = measurement.getBandwidthMbps();
        
        // Simple scoring based on thresholds
        int gamingScore = calculateGamingScore(signalStrength, latency, bandwidth);
        int streamingScore = calculateStreamingScore(signalStrength, latency, bandwidth);
        int videoCallScore = calculateVideoCallScore(signalStrength, latency, bandwidth);
        
        // Calculate overall grade
        String overallGrade = calculateOverallGrade(gamingScore, streamingScore, videoCallScore);
        
        // Determine zone
        String zone = determineZone(overallGrade);
        
        return new ClassificationResult(
            measurement.getRoomId(),
            measurement.getRoomName(),
            gamingScore,
            streamingScore,
            videoCallScore,
            overallGrade,
            zone
        );
    }
    
    /**
     * Get all classifications
     */
    public List<ClassificationResult> getAllClassifications() {
        return new ArrayList<>(classifications);
    }
    
    /**
     * Get classification for a specific room
     */
    public ClassificationResult getClassificationForRoom(String roomId) {
        for (ClassificationResult classification : classifications) {
            if (classification.getRoomId().equals(roomId)) {
                return classification;
            }
        }
        return null;
    }
    
    /**
     * Clear all measurements and classifications
     */
    public void clearAll() {
        measurements.clear();
        classifications.clear();
    }
    
    // TODO: Implement proper scoring algorithms
    private int calculateGamingScore(int signalStrength, int latency, double bandwidth) {
        // Gaming requires low latency and stable connection
        int score = 0;
        
        // Signal strength scoring (0-40 points)
        if (signalStrength >= -30) score += 40;
        else if (signalStrength >= -50) score += 30;
        else if (signalStrength >= -70) score += 20;
        else if (signalStrength >= -80) score += 10;
        
        // Latency scoring (0-40 points)
        if (latency <= 20) score += 40;
        else if (latency <= 50) score += 30;
        else if (latency <= 100) score += 20;
        else if (latency <= 200) score += 10;
        
        // Bandwidth scoring (0-20 points)
        if (bandwidth >= 25) score += 20;
        else if (bandwidth >= 10) score += 15;
        else if (bandwidth >= 5) score += 10;
        else if (bandwidth >= 1) score += 5;
        
        return Math.min(score, 100);
    }
    
    private int calculateStreamingScore(int signalStrength, int latency, double bandwidth) {
        // Streaming requires good bandwidth and stable connection
        int score = 0;
        
        // Signal strength scoring (0-30 points)
        if (signalStrength >= -30) score += 30;
        else if (signalStrength >= -50) score += 25;
        else if (signalStrength >= -70) score += 15;
        else if (signalStrength >= -80) score += 5;
        
        // Latency scoring (0-20 points)
        if (latency <= 100) score += 20;
        else if (latency <= 200) score += 15;
        else if (latency <= 500) score += 10;
        
        // Bandwidth scoring (0-50 points)
        if (bandwidth >= 25) score += 50;
        else if (bandwidth >= 10) score += 40;
        else if (bandwidth >= 5) score += 30;
        else if (bandwidth >= 1) score += 15;
        
        return Math.min(score, 100);
    }
    
    private int calculateVideoCallScore(int signalStrength, int latency, double bandwidth) {
        // Video calls require balanced performance
        int score = 0;
        
        // Signal strength scoring (0-35 points)
        if (signalStrength >= -30) score += 35;
        else if (signalStrength >= -50) score += 30;
        else if (signalStrength >= -70) score += 20;
        else if (signalStrength >= -80) score += 10;
        
        // Latency scoring (0-35 points)
        if (latency <= 50) score += 35;
        else if (latency <= 100) score += 30;
        else if (latency <= 200) score += 20;
        else if (latency <= 500) score += 10;
        
        // Bandwidth scoring (0-30 points)
        if (bandwidth >= 10) score += 30;
        else if (bandwidth >= 5) score += 25;
        else if (bandwidth >= 2) score += 15;
        else if (bandwidth >= 1) score += 10;
        
        return Math.min(score, 100);
    }
    
    private String calculateOverallGrade(int gamingScore, int streamingScore, int videoCallScore) {
        // Calculate average score
        int averageScore = (gamingScore + streamingScore + videoCallScore) / 3;
        
        if (averageScore >= 90) return "A";
        else if (averageScore >= 80) return "B";
        else if (averageScore >= 70) return "C";
        else if (averageScore >= 60) return "D";
        else return "F";
    }
    
    private String determineZone(String grade) {
        switch (grade) {
            case "A":
            case "B":
                return "good";
            case "C":
                return "medium";
            case "D":
            case "F":
            default:
                return "poor";
        }
    }
}
