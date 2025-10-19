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
                
                // Log comprehensive classification details
                logClassificationDetails(classificationResult);
                
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
        
        if (wifiMeasurementService == null) {
            Log.e(TAG, "WiFiMeasurementService is null, cannot start measurement");
            errorMessage.setValue("Service not available");
            return;
        }
        
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
        
        if (wifiMeasurementService == null) {
            Log.e(TAG, "WiFiMeasurementService is null, cannot start speed test");
            errorMessage.setValue("Service not available");
            return;
        }
        
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
        if (wifiMeasurementService != null) {
            wifiMeasurementService.stopMeasurement();
        }
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
        if (wifiMeasurementService == null) {
            Log.e(TAG, "WiFiMeasurementService is null, cannot add measurement");
            return;
        }
        
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
    
    /**
     * Log comprehensive classification details to logcat for examination
     */
    private void logClassificationDetails(ClassificationResult result) {
        Log.i(TAG, "========================================");
        Log.i(TAG, "WiFi Classification Results");
        Log.i(TAG, "========================================");
        Log.i(TAG, "Room: " + result.getRoomName());
        Log.i(TAG, "Activity Type: " + result.getActivityType());
        Log.i(TAG, "Timestamp: " + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date(result.getTimestamp())));
        Log.i(TAG, "");
        
        // Overall classification
        Log.i(TAG, "OVERALL CLASSIFICATION: " + result.getOverallClassification().getDisplayName() + 
                   " (Score: " + result.getOverallClassification().getScore() + "/5)");
        Log.i(TAG, "");
        
        // Individual metric classifications
        Log.i(TAG, "--- Individual Metric Classifications ---");
        if (result.getMetricClassification() != null) {
            Log.i(TAG, "Signal Strength: " + result.getMetricClassification().getSignalStrengthClassification().getDisplayName() + 
                       " (Score: " + result.getMetricClassification().getSignalStrengthClassification().getScore() + "/5)");
            Log.i(TAG, "Latency: " + result.getMetricClassification().getLatencyClassification().getDisplayName() + 
                       " (Score: " + result.getMetricClassification().getLatencyClassification().getScore() + "/5)");
            Log.i(TAG, "Bandwidth: " + result.getMetricClassification().getBandwidthClassification().getDisplayName() + 
                       " (Score: " + result.getMetricClassification().getBandwidthClassification().getScore() + "/5)");
            Log.i(TAG, "Jitter: " + result.getMetricClassification().getJitterClassification().getDisplayName() + 
                       " (Score: " + result.getMetricClassification().getJitterClassification().getScore() + "/5)");
            Log.i(TAG, "Packet Loss: " + result.getMetricClassification().getPacketLossClassification().getDisplayName() + 
                       " (Score: " + result.getMetricClassification().getPacketLossClassification().getScore() + "/5)");
        }
        Log.i(TAG, "");
        
        // Activity importance weights
        Log.i(TAG, "--- Activity Importance Weights ---");
        if (result.getActivityImportance() != null) {
            Log.i(TAG, "Signal Strength Weight: " + String.format("%.2f", result.getActivityImportance().getSignalStrengthWeight()));
            Log.i(TAG, "Latency Weight: " + String.format("%.2f", result.getActivityImportance().getLatencyWeight()));
            Log.i(TAG, "Bandwidth Weight: " + String.format("%.2f", result.getActivityImportance().getBandwidthWeight()));
            Log.i(TAG, "Jitter Weight: " + String.format("%.2f", result.getActivityImportance().getJitterWeight()));
            Log.i(TAG, "Packet Loss Weight: " + String.format("%.2f", result.getActivityImportance().getPacketLossWeight()));
        }
        Log.i(TAG, "");
        
        // Most critical metric
        if (result.getMostCriticalMetric() != null) {
            Log.i(TAG, "Most Critical Metric: " + result.getMostCriticalMetric());
        }
        Log.i(TAG, "");
        
        // Reasoning
        if (result.getReasoning() != null) {
            Log.i(TAG, "Reasoning: " + result.getReasoning());
        }
        Log.i(TAG, "");
        
        // Well performing metrics
        String[] wellPerforming = result.getWellPerformingMetrics();
        if (wellPerforming.length > 0) {
            Log.i(TAG, "Well Performing Metrics:");
            for (String metric : wellPerforming) {
                Log.i(TAG, "  - " + metric.replace("_", " "));
            }
        } else {
            Log.i(TAG, "Well Performing Metrics: None");
        }
        Log.i(TAG, "");
        
        // Poorly performing metrics
        String[] poorlyPerforming = result.getPoorlyPerformingMetrics();
        if (poorlyPerforming.length > 0) {
            Log.i(TAG, "Poorly Performing Metrics:");
            for (String metric : poorlyPerforming) {
                Log.i(TAG, "  - " + metric.replace("_", " "));
            }
        } else {
            Log.i(TAG, "Poorly Performing Metrics: None");
        }
        Log.i(TAG, "");
        
        // Most important poor metric
        String mostImportantPoor = result.getMostImportantPoorMetric();
        if (mostImportantPoor != null) {
            Log.i(TAG, "Most Important Poor Metric: " + mostImportantPoor.replace("_", " "));
        }
        Log.i(TAG, "");
        
        // Recommendations
        if (result.getRecommendations() != null && !result.getRecommendations().isEmpty()) {
            Log.i(TAG, "Recommendations:");
            for (String recommendation : result.getRecommendations()) {
                Log.i(TAG, "  * " + recommendation);
            }
        } else {
            Log.i(TAG, "Recommendations: None");
        }
        Log.i(TAG, "");
        
        // Acceptability for activity
        boolean acceptable = result.isAcceptableForActivity();
        Log.i(TAG, "Acceptable for Activity: " + (acceptable ? "YES" : "NO"));
        Log.i(TAG, "");
        
        // Summary
        Log.i(TAG, "Summary: " + result.getSummary());
        Log.i(TAG, "========================================");
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
