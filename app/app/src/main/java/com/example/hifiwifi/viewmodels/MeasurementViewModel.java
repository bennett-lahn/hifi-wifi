package com.example.hifiwifi.viewmodels;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.hifiwifi.classifier.ClassificationResult;
import com.example.hifiwifi.models.NetworkMetrics;
import com.example.hifiwifi.models.RoomMeasurement;
import com.example.hifiwifi.services.WiFiMeasurementService;

import java.util.ArrayList;
import java.util.List;

/**
 * ViewModel for managing WiFi measurements and classifications
 * Exposes LiveData for UI observation
 * Connected to actual WiFiMeasurementService for real-time measurements
 */
public class MeasurementViewModel extends AndroidViewModel {
    
    private static final String TAG = "MeasurementViewModel";
    
    // LiveData for UI observation
    private MutableLiveData<NetworkMetrics> currentMetrics;
    private MutableLiveData<List<RoomMeasurement>> allMeasurements;
    private MutableLiveData<List<ClassificationResult>> classifications;
    private MutableLiveData<Boolean> isMeasuring;
    private MutableLiveData<String> errorMessage;
    private MutableLiveData<Boolean> isTestComplete;
    
    // Service and data storage
    private WiFiMeasurementService wifiMeasurementService;
    private List<RoomMeasurement> measurements;
    private List<ClassificationResult> classificationResults;
    
    public MeasurementViewModel(@NonNull Application application) {
        super(application);
        
        // Initialize LiveData
        currentMetrics = new MutableLiveData<>();
        allMeasurements = new MutableLiveData<>();
        classifications = new MutableLiveData<>();
        isMeasuring = new MutableLiveData<>(false);
        errorMessage = new MutableLiveData<>();
        isTestComplete = new MutableLiveData<>(false);
        
        // Initialize service and data storage
        wifiMeasurementService = new WiFiMeasurementService(application);
        measurements = new ArrayList<>();
        classificationResults = new ArrayList<>();
        
        // Set up service callback
        wifiMeasurementService.setCallback(new WiFiMeasurementService.MeasurementCallback() {
            @Override
            public void onMeasurementUpdate(NetworkMetrics metrics) {
                Log.d(TAG, "Received measurement update: " + metrics.getCurrentBandwidthMbps() + " Mbps");
                currentMetrics.postValue(metrics);
            }
            
            @Override
            public void onMeasurementComplete(RoomMeasurement measurement) {
                Log.d(TAG, "Measurement completed for room: " + measurement.getRoomName() + 
                      ", Speed: " + measurement.getBandwidthMbps() + " Mbps, Latency: " + measurement.getLatencyMs() + "ms");
                measurements.add(measurement);
                allMeasurements.postValue(new ArrayList<>(measurements));
                
                // Update the current metrics with final values
                NetworkMetrics finalMetrics = new NetworkMetrics(
                    measurement.getSignalStrengthDbm(),
                    measurement.getLatencyMs(),
                    measurement.getBandwidthMbps(),
                    measurement.getJitterMs(),
                    measurement.getPacketLossPercent(),
                    true,
                    measurement.getRoomName()
                );
                currentMetrics.postValue(finalMetrics);
                
                // Mark test as complete and stop measuring
                isMeasuring.postValue(false);
                isTestComplete.postValue(true);
                
                Log.d(TAG, "Final metrics posted to UI: " + measurement.getBandwidthMbps() + " Mbps");
            }
            
            @Override
            public void onClassificationComplete(ClassificationResult classificationResult) {
                Log.d(TAG, "Classification completed for room: " + classificationResult.getRoomName());
                // Remove existing classification for this room if any
                classificationResults.removeIf(c -> c.getRoomId().equals(classificationResult.getRoomId()));
                classificationResults.add(classificationResult);
                classifications.postValue(new ArrayList<>(classificationResults));
            }
            
            @Override
            public void onError(String error) {
                Log.e(TAG, "Service error: " + error);
                errorMessage.postValue(error);
            }
        });
        
        // Initialize with empty data
        allMeasurements.setValue(measurements);
        classifications.setValue(classificationResults);
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
    
    public LiveData<Boolean> getIsTestComplete() {
        return isTestComplete;
    }
    
    /**
     * Start measurement for a specific room (continuous mode)
     */
    public void startMeasurement(String roomId, String roomName, String activityType) {
        Log.d(TAG, "Starting continuous measurement for room: " + roomName);
        
        if (isMeasuring.getValue() != null && isMeasuring.getValue()) {
            stopMeasurement();
        }
        
        isMeasuring.setValue(true);
        isTestComplete.setValue(false);
        errorMessage.setValue(null);
        
        // Start the actual WiFi measurement service (continuous mode)
        wifiMeasurementService.startMeasurement(roomName, activityType);
    }
    
    /**
     * Start a single speed test for the current room
     */
    public void startSingleSpeedTest(String roomName, String activityType) {
        Log.d(TAG, "Starting single speed test for room: " + roomName + ", activity: " + activityType);
        
        if (isMeasuring.getValue() != null && isMeasuring.getValue()) {
            Log.w(TAG, "Measurement already in progress, stopping previous measurement");
            stopMeasurement();
        }
        
        isMeasuring.setValue(true);
        isTestComplete.setValue(false);
        errorMessage.setValue(null);
        
        // Start single speed test
        wifiMeasurementService.startSingleSpeedTest(roomName, activityType);
    }
    
    /**
     * Stop current measurement
     */
    public void stopMeasurement() {
        isMeasuring.setValue(false);
        wifiMeasurementService.stopMeasurement();
    }
    
    /**
     * Rerun the test for the current room
     */
    public void rerunTest(String roomName, String activityType) {
        stopMeasurement();
        startSingleSpeedTest(roomName, activityType);
    }
    
    /**
     * Add a manual measurement
     */
    public void addMeasurement(String roomId, String roomName, String activityType) {
        RoomMeasurement measurement = new RoomMeasurement(
            roomId, roomName, 
            wifiMeasurementService.getCurrentSignalStrength(),
            50, // Default latency
            0.0, // Will be updated by service
            activityType
        );
        
        measurements.add(measurement);
        allMeasurements.setValue(new ArrayList<>(measurements));
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
     * Get classification for a specific room
     */
    public ClassificationResult getClassificationForRoom(String roomId) {
        for (ClassificationResult classification : classificationResults) {
            if (classification.getRoomId().equals(roomId)) {
                return classification;
            }
        }
        return null;
    }
    
    /**
     * Export measurements to JSON
     */
    public String exportMeasurementsToJSON() {
        StringBuilder json = new StringBuilder();
        json.append("{\"measurements\":[");
        for (int i = 0; i < measurements.size(); i++) {
            if (i > 0) json.append(",");
            RoomMeasurement m = measurements.get(i);
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
     * Clear all measurements
     */
    public void clearAllMeasurements() {
        measurements.clear();
        classificationResults.clear();
        allMeasurements.setValue(new ArrayList<>(measurements));
        classifications.setValue(new ArrayList<>(classificationResults));
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
        Log.d(TAG, "ViewModel being cleared - cleaning up resources");
        
        // Stop any ongoing measurements
        if (isMeasuring.getValue() != null && isMeasuring.getValue()) {
            Log.d(TAG, "Stopping ongoing measurement before cleanup");
            stopMeasurement();
        }
        
        // Cleanup service resources
        if (wifiMeasurementService != null) {
            Log.d(TAG, "Cleaning up WiFiMeasurementService");
            wifiMeasurementService.cleanup();
            wifiMeasurementService = null;
        }
        
        Log.d(TAG, "ViewModel cleanup completed");
    }
}
