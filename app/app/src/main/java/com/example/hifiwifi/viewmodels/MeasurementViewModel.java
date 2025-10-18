package com.example.hifiwifi.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.hifiwifi.models.ClassificationResult;
import com.example.hifiwifi.models.NetworkMetrics;
import com.example.hifiwifi.models.RoomMeasurement;
import com.example.hifiwifi.repository.MeasurementRepository;
import com.example.hifiwifi.services.WiFiMeasurementService;

import java.util.List;

/**
 * ViewModel for managing WiFi measurements and classifications
 * Exposes LiveData for UI observation
 */
public class MeasurementViewModel extends AndroidViewModel {
    
    private MeasurementRepository measurementRepository;
    private WiFiMeasurementService wifiMeasurementService;
    
    // LiveData for UI observation
    private MutableLiveData<NetworkMetrics> currentMetrics;
    private MutableLiveData<List<RoomMeasurement>> allMeasurements;
    private MutableLiveData<List<ClassificationResult>> classifications;
    private MutableLiveData<Boolean> isMeasuring;
    private MutableLiveData<String> errorMessage;
    
    public MeasurementViewModel(@NonNull Application application) {
        super(application);
        
        // Initialize repositories and services
        measurementRepository = new MeasurementRepository();
        wifiMeasurementService = new WiFiMeasurementService(application);
        
        // Initialize LiveData
        currentMetrics = new MutableLiveData<>();
        allMeasurements = new MutableLiveData<>();
        classifications = new MutableLiveData<>();
        isMeasuring = new MutableLiveData<>(false);
        errorMessage = new MutableLiveData<>();
        
        // Setup service callback
        wifiMeasurementService.setCallback(new WiFiMeasurementService.MeasurementCallback() {
            @Override
            public void onMeasurementUpdate(NetworkMetrics metrics) {
                currentMetrics.postValue(metrics);
            }
            
            @Override
            public void onMeasurementComplete(RoomMeasurement measurement) {
                // Add measurement to repository
                measurementRepository.addMeasurement(measurement);
                
                // Update LiveData
                allMeasurements.postValue(measurementRepository.getMeasurements());
                classifications.postValue(measurementRepository.getAllClassifications());
            }
            
            @Override
            public void onError(String error) {
                errorMessage.postValue(error);
            }
        });
        
        // Initialize with empty data
        allMeasurements.setValue(measurementRepository.getMeasurements());
        classifications.setValue(measurementRepository.getAllClassifications());
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
     * Start measurement for a specific room
     */
    public void startMeasurement(String roomId, String roomName, String activityType) {
        if (isMeasuring.getValue() != null && isMeasuring.getValue()) {
            stopMeasurement();
        }
        
        isMeasuring.setValue(true);
        wifiMeasurementService.startMeasurement(roomName);
    }
    
    /**
     * Stop current measurement
     */
    public void stopMeasurement() {
        isMeasuring.setValue(false);
        wifiMeasurementService.stopMeasurement();
    }
    
    /**
     * Add a manual measurement
     */
    public void addMeasurement(String roomId, String roomName, String activityType) {
        RoomMeasurement measurement = wifiMeasurementService.createRoomMeasurement(
            roomId, roomName, activityType
        );
        measurementRepository.addMeasurement(measurement);
        
        // Update LiveData
        allMeasurements.setValue(measurementRepository.getMeasurements());
        classifications.setValue(measurementRepository.getAllClassifications());
    }
    
    /**
     * Get measurements for a specific room
     */
    public List<RoomMeasurement> getMeasurementsForRoom(String roomId) {
        return measurementRepository.getMeasurementsForRoom(roomId);
    }
    
    /**
     * Get classification for a specific room
     */
    public ClassificationResult getClassificationForRoom(String roomId) {
        return measurementRepository.getClassificationForRoom(roomId);
    }
    
    /**
     * Export measurements to JSON
     */
    public String exportMeasurementsToJSON() {
        // TODO: Implement JSON export using Gson
        List<RoomMeasurement> measurements = measurementRepository.getMeasurements();
        // For now, return a simple string representation
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
        measurementRepository.clearAll();
        allMeasurements.setValue(measurementRepository.getMeasurements());
        classifications.setValue(measurementRepository.getAllClassifications());
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
        wifiMeasurementService.cleanup();
    }
}
