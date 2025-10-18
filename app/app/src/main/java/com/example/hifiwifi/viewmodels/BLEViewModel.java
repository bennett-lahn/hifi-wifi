package com.example.hifiwifi.viewmodels;

import android.app.Application;
import android.bluetooth.BluetoothDevice;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.hifiwifi.models.BLEConnectionState;
import com.example.hifiwifi.services.BLEService;

import java.util.ArrayList;
import java.util.List;

/**
 * ViewModel for managing BLE communication with Raspberry Pi
 * Exposes LiveData for UI observation
 */
public class BLEViewModel extends AndroidViewModel {
    
    private BLEService bleService;
    
    // LiveData for UI observation
    private MutableLiveData<BLEConnectionState> connectionState;
    private MutableLiveData<List<BluetoothDevice>> availableDevices;
    private MutableLiveData<String> slmRecommendation;
    private MutableLiveData<String> errorMessage;
    
    public BLEViewModel(@NonNull Application application) {
        super(application);
        
        // Initialize BLE service
        bleService = new BLEService(application);
        
        // Initialize LiveData
        connectionState = new MutableLiveData<>(new BLEConnectionState());
        availableDevices = new MutableLiveData<>(new ArrayList<>());
        slmRecommendation = new MutableLiveData<>("");
        errorMessage = new MutableLiveData<>();
        
        // Setup service callback
        bleService.setCallback(new BLEService.BLECallback() {
            @Override
            public void onDeviceFound(BluetoothDevice device) {
                List<BluetoothDevice> devices = availableDevices.getValue();
                if (devices != null && !devices.contains(device)) {
                    devices.add(device);
                    availableDevices.postValue(new ArrayList<>(devices));
                }
            }
            
            @Override
            public void onConnectionStateChanged(boolean connected, String deviceName) {
                BLEConnectionState state = new BLEConnectionState();
                state.setConnected(connected);
                state.setConnecting(false);
                state.setDeviceName(deviceName);
                state.setErrorMessage("");
                connectionState.postValue(state);
            }
            
            @Override
            public void onDataReceived(String data) {
                // TODO: Parse JSON response from Pi and extract SLM recommendation
                slmRecommendation.postValue(data);
            }
            
            @Override
            public void onError(String error) {
                errorMessage.postValue(error);
                
                // Update connection state with error
                BLEConnectionState state = connectionState.getValue();
                if (state != null) {
                    state.setConnecting(false);
                    state.setErrorMessage(error);
                    connectionState.postValue(state);
                }
            }
        });
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
     * Start scanning for BLE devices
     */
    public void startScan() {
        BLEConnectionState state = connectionState.getValue();
        if (state != null) {
            state.setConnecting(true);
            state.setErrorMessage("");
            connectionState.setValue(state);
        }
        
        bleService.startScan();
    }
    
    /**
     * Stop scanning for BLE devices
     */
    public void stopScan() {
        bleService.stopScan();
    }
    
    /**
     * Connect to a specific device
     */
    public void connect(BluetoothDevice device) {
        BLEConnectionState state = connectionState.getValue();
        if (state != null) {
            state.setConnecting(true);
            state.setErrorMessage("");
            connectionState.setValue(state);
        }
        
        bleService.connectToDevice(device);
    }
    
    /**
     * Disconnect from current device
     */
    public void disconnect() {
        bleService.disconnect();
    }
    
    /**
     * Send measurement data to Pi
     */
    public void sendDataToPi(String jsonData) {
        if (bleService.isConnected()) {
            bleService.sendMeasurementData(jsonData);
        } else {
            errorMessage.setValue("Not connected to device");
        }
    }
    
    /**
     * Send chat message to Pi
     */
    public void sendChatMessage(String message) {
        if (bleService.isConnected()) {
            bleService.sendChatMessage(message);
        } else {
            errorMessage.setValue("Not connected to device");
        }
    }
    
    /**
     * Check if currently connected
     */
    public boolean isConnected() {
        return bleService.isConnected();
    }
    
    /**
     * Check if currently scanning
     */
    public boolean isScanning() {
        return bleService.isScanning();
    }
    
    /**
     * Get connected device name
     */
    public String getConnectedDeviceName() {
        return bleService.getConnectedDeviceName();
    }
    
    /**
     * Clear available devices list
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
