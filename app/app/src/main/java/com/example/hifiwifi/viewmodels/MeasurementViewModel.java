package com.example.hifiwifi.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.hifiwifi.models.ClassificationResult;
import com.example.hifiwifi.models.NetworkMetrics;
import com.example.hifiwifi.models.RoomMeasurement;

import java.util.ArrayList;
import java.util.List;

/**
 * ViewModel for managing WiFi measurements and classifications
 * Exposes LiveData for UI observation
 * MOCK VERSION - Disconnected from actual services for UI development
 */
public class MeasurementViewModel extends AndroidViewModel {
    
    // LiveData for UI observation
    private MutableLiveData<NetworkMetrics> currentMetrics;
    private MutableLiveData<List<RoomMeasurement>> allMeasurements;
    private MutableLiveData<List<ClassificationResult>> classifications;
    private MutableLiveData<Boolean> isMeasuring;
    private MutableLiveData<String> errorMessage;
    
    // Mock data storage
    private List<RoomMeasurement> mockMeasurements;
    private List<ClassificationResult> mockClassifications;
    
    public MeasurementViewModel(@NonNull Application application) {
        super(application);
        
        // Initialize LiveData
        currentMetrics = new MutableLiveData<>();
        allMeasurements = new MutableLiveData<>();
        classifications = new MutableLiveData<>();
        isMeasuring = new MutableLiveData<>(false);
        errorMessage = new MutableLiveData<>();
        
        // Initialize mock data
        mockMeasurements = new ArrayList<>();
        mockClassifications = new ArrayList<>();
        
        // Initialize with mock data for UI development
        initializeMockData();
        
        allMeasurements.setValue(mockMeasurements);
        classifications.setValue(mockClassifications);
    }
    
    /**
     * Initialize mock data for UI development
     */
    private void initializeMockData() {
        // Create mock measurements
        mockMeasurements.add(new RoomMeasurement(
            "room1", "Living Room", -45, 25, 85.5, "gaming"
        ));
        mockMeasurements.add(new RoomMeasurement(
            "room2", "Bedroom", -65, 45, 45.2, "streaming"
        ));
        mockMeasurements.add(new RoomMeasurement(
            "room3", "Kitchen", -55, 35, 65.8, "video_call"
        ));
        
        // Create mock classifications
        mockClassifications.add(new ClassificationResult(
            "room1", "Living Room", 95, 88, 92, "A", "Excellent"
        ));
        mockClassifications.add(new ClassificationResult(
            "room2", "Bedroom", 75, 82, 78, "B", "Good"
        ));
        mockClassifications.add(new ClassificationResult(
            "room3", "Kitchen", 65, 70, 68, "C", "Fair"
        ));
    }
    
    // LiveData getters
    public LiveData<NetworkMetrics> getCurrentMetrics() {
        return currentMetrics;
    }
    
    public LiveData<List<RoomMeasurement>> getAllMeasurements() {
        return allMeasurements;
    }
    
    public LiveData<List<ClassificationResult>> getClassifications() {
        return classifications;
    }
    
    public LiveData<Boolean> getIsMeasuring() {
        return isMeasuring;
    }
    
    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }
    
    /**
     * Start measurement for a specific room (MOCK VERSION)
     */
    public void startMeasurement(String roomId, String roomName, String activityType) {
        if (isMeasuring.getValue() != null && isMeasuring.getValue()) {
            stopMeasurement();
        }
        
        isMeasuring.setValue(true);
        
        // Simulate measurement with mock data
        simulateMeasurement(roomName);
    }
    
    /**
     * Stop current measurement (MOCK VERSION)
     */
    public void stopMeasurement() {
        isMeasuring.setValue(false);
    }
    
    /**
     * Simulate measurement with mock data
     */
    private void simulateMeasurement(String roomName) {
        // Create mock metrics that change over time
        NetworkMetrics mockMetrics = new NetworkMetrics(
            -50 + (int)(Math.random() * 30), // Random signal strength between -50 and -80
            20 + (int)(Math.random() * 40),  // Random latency between 20 and 60ms
            50 + Math.random() * 50,         // Random bandwidth between 50 and 100 Mbps
            true,
            roomName
        );
        currentMetrics.setValue(mockMetrics);
    }
    
    /**
     * Add a manual measurement (MOCK VERSION)
     */
    public void addMeasurement(String roomId, String roomName, String activityType) {
        RoomMeasurement measurement = new RoomMeasurement(
            roomId, roomName, 
            -50 + (int)(Math.random() * 30), // Random signal strength
            20 + (int)(Math.random() * 40),  // Random latency
            50 + Math.random() * 50,         // Random bandwidth
            activityType
        );
        
        mockMeasurements.add(measurement);
        allMeasurements.setValue(new ArrayList<>(mockMeasurements));
    }
    
    /**
     * Get measurements for a specific room (MOCK VERSION)
     */
    public List<RoomMeasurement> getMeasurementsForRoom(String roomId) {
        List<RoomMeasurement> roomMeasurements = new ArrayList<>();
        for (RoomMeasurement measurement : mockMeasurements) {
            if (measurement.getRoomId().equals(roomId)) {
                roomMeasurements.add(measurement);
            }
        }
        return roomMeasurements;
    }
    
    /**
     * Get classification for a specific room (MOCK VERSION)
     */
    public ClassificationResult getClassificationForRoom(String roomId) {
        for (ClassificationResult classification : mockClassifications) {
            if (classification.getRoomId().equals(roomId)) {
                return classification;
            }
        }
        return null;
    }
    
    /**
     * Export measurements to JSON (MOCK VERSION)
     */
    public String exportMeasurementsToJSON() {
        StringBuilder json = new StringBuilder();
        json.append("{\"measurements\":[");
        for (int i = 0; i < mockMeasurements.size(); i++) {
            if (i > 0) json.append(",");
            RoomMeasurement m = mockMeasurements.get(i);
            json.append("{");
            json.append("\"roomId\":\"").append(m.getRoomId()).append("\",");
            json.append("\"roomName\":\"").append(m.getRoomName()).append("\",");
            json.append("\"timestamp\":").append(m.getTimestamp()).append(",");
            json.append("\"signalStrengthDbm\":").append(m.getSignalStrengthDbm()).append(",");
            json.append("\"latencyMs\":").append(m.getLatencyMs()).append(",");
            json.append("\"bandwidthMbps\":").append(m.getBandwidthMbps()).append(",");
            json.append("\"activityType\":\"").append(m.getActivityType()).append("\"");
            json.append("}");
        }
        json.append("]}");
        return json.toString();
    }
    
    /**
     * Clear all measurements (MOCK VERSION)
     */
    public void clearAllMeasurements() {
        mockMeasurements.clear();
        mockClassifications.clear();
        allMeasurements.setValue(new ArrayList<>(mockMeasurements));
        classifications.setValue(new ArrayList<>(mockClassifications));
    }
    
    /**
     * Clear error message
     */
    public void clearError() {
        errorMessage.setValue(null);
    }
    
    @Override
    protected void onCleared() {
        super.onCleared();
        // No cleanup needed for mock version
    }
}
