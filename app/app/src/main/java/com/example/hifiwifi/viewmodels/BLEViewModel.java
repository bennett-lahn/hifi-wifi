package com.example.hifiwifi.viewmodels;

import android.app.Application;
import android.bluetooth.BluetoothDevice;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.hifiwifi.models.BLEConnectionState;

import java.util.ArrayList;
import java.util.List;

/**
 * ViewModel for managing BLE communication with Raspberry Pi
 * Exposes LiveData for UI observation
 * MOCK VERSION - Disconnected from actual services for UI development
 */
public class BLEViewModel extends AndroidViewModel {
    
    // LiveData for UI observation
    private MutableLiveData<BLEConnectionState> connectionState;
    private MutableLiveData<List<BluetoothDevice>> availableDevices;
    private MutableLiveData<String> slmRecommendation;
    private MutableLiveData<String> errorMessage;
    
    // Mock state
    private boolean isConnected = false;
    private boolean isScanning = false;
    private String connectedDeviceName = "";
    
    public BLEViewModel(@NonNull Application application) {
        super(application);
        
        // Initialize LiveData
        connectionState = new MutableLiveData<>(new BLEConnectionState());
        availableDevices = new MutableLiveData<>(new ArrayList<>());
        slmRecommendation = new MutableLiveData<>("");
        errorMessage = new MutableLiveData<>();
        
        // Initialize with mock data
        initializeMockDevices();
    }
    
    /**
     * Initialize mock BLE devices for UI development
     */
    private void initializeMockDevices() {
        // Note: In a real implementation, this would be populated by actual BLE scanning
        // For UI development, we'll leave this empty and let the UI show "No devices found"
        availableDevices.setValue(new ArrayList<>());
    }
    
    // LiveData getters
    public LiveData<BLEConnectionState> getConnectionState() {
        return connectionState;
    }
    
    public LiveData<List<BluetoothDevice>> getAvailableDevices() {
        return availableDevices;
    }
    
    public LiveData<String> getSlmRecommendation() {
        return slmRecommendation;
    }
    
    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }
    
    /**
     * Start scanning for BLE devices (MOCK VERSION)
     */
    public void startScan() {
        isScanning = true;
        BLEConnectionState state = new BLEConnectionState();
        state.setConnecting(true);
        state.setConnected(false);
        state.setErrorMessage("");
        state.setDeviceName("");
        connectionState.setValue(state);
        
        // Simulate scanning delay
        // In real implementation, this would be handled by the BLE service
    }
    
    /**
     * Stop scanning for BLE devices (MOCK VERSION)
     */
    public void stopScan() {
        isScanning = false;
        BLEConnectionState state = new BLEConnectionState();
        state.setConnecting(false);
        state.setConnected(false);
        state.setErrorMessage("");
        state.setDeviceName("");
        connectionState.setValue(state);
    }
    
    /**
     * Connect to a specific device (MOCK VERSION)
     */
    public void connect(BluetoothDevice device) {
        BLEConnectionState state = new BLEConnectionState();
        state.setConnecting(true);
        state.setConnected(false);
        state.setErrorMessage("");
        state.setDeviceName("");
        connectionState.setValue(state);
        
        // Simulate connection delay
        // In real implementation, this would be handled by the BLE service
        // For mock, we'll just set connected to true after a brief delay
        isConnected = true;
        connectedDeviceName = device.getName() != null ? device.getName() : "Mock Device";
        
        BLEConnectionState connectedState = new BLEConnectionState();
        connectedState.setConnecting(false);
        connectedState.setConnected(true);
        connectedState.setErrorMessage("");
        connectedState.setDeviceName(connectedDeviceName);
        connectionState.setValue(connectedState);
    }
    
    /**
     * Disconnect from current device (MOCK VERSION)
     */
    public void disconnect() {
        isConnected = false;
        connectedDeviceName = "";
        
        BLEConnectionState state = new BLEConnectionState();
        state.setConnecting(false);
        state.setConnected(false);
        state.setErrorMessage("");
        state.setDeviceName("");
        connectionState.setValue(state);
    }
    
    /**
     * Send measurement data to Pi (MOCK VERSION)
     */
    public void sendDataToPi(String jsonData) {
        if (isConnected) {
            // Simulate sending data
            slmRecommendation.setValue("Mock recommendation: Your WiFi signal is good for gaming!");
        } else {
            errorMessage.setValue("Not connected to device");
        }
    }
    
    /**
     * Send chat message to Pi (MOCK VERSION)
     */
    public void sendChatMessage(String message) {
        if (isConnected) {
            // Simulate sending chat message
            slmRecommendation.setValue("Mock response: " + message);
        } else {
            errorMessage.setValue("Not connected to device");
        }
    }
    
    /**
     * Check if currently connected (MOCK VERSION)
     */
    public boolean isConnected() {
        return isConnected;
    }
    
    /**
     * Check if currently scanning (MOCK VERSION)
     */
    public boolean isScanning() {
        return isScanning;
    }
    
    /**
     * Get connected device name (MOCK VERSION)
     */
    public String getConnectedDeviceName() {
        return connectedDeviceName;
    }
    
    /**
     * Clear available devices list (MOCK VERSION)
     */
    public void clearAvailableDevices() {
        availableDevices.setValue(new ArrayList<>());
    }
    
    /**
     * Clear error message
     */
    public void clearError() {
        errorMessage.setValue(null);
    }
    
    /**
     * Clear SLM recommendation
     */
    public void clearRecommendation() {
        slmRecommendation.setValue("");
    }
}
