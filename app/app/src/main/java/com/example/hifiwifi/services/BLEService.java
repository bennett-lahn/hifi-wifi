package com.example.hifiwifi.services;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Service for BLE communication with Raspberry Pi
 * Handles scanning, connection, and data exchange
 */
public class BLEService {
    
    // UUIDs for communication with Raspberry Pi
    // TODO: Replace with actual UUIDs from Pi implementation
    private static final String SERVICE_UUID = "12345678-1234-1234-1234-123456789ABC";
    private static final String MEASUREMENT_CHARACTERISTIC_UUID = "12345678-1234-1234-1234-123456789ABD";
    private static final String CHAT_CHARACTERISTIC_UUID = "12345678-1234-1234-1234-123456789ABE";
    private static final String RESPONSE_CHARACTERISTIC_UUID = "12345678-1234-1234-1234-123456789ABF";
    
    private Context context;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeScanner bluetoothLeScanner;
    private BluetoothGatt bluetoothGatt;
    private Handler mainHandler;
    private Gson gson;
    
    // Callback interfaces
    public interface BLECallback {
        void onDeviceFound(BluetoothDevice device);
        void onConnectionStateChanged(boolean connected, String deviceName);
        void onDataReceived(String data);
        void onError(String error);
    }
    
    private BLECallback callback;
    private List<BluetoothDevice> discoveredDevices;
    private boolean isScanning = false;
    private boolean isConnected = false;
    private String connectedDeviceName = "";
    
    public BLEService(Context context) {
        this.context = context;
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.gson = new Gson();
        this.discoveredDevices = new ArrayList<>();
        
        BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        this.bluetoothAdapter = bluetoothManager.getAdapter();
        this.bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
    }
    
    public void setCallback(BLECallback callback) {
        this.callback = callback;
    }
    
    /**
     * Start scanning for BLE devices
     */
    public void startScan() {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            notifyError("Bluetooth is not enabled");
            return;
        }
        
        if (isScanning) {
            stopScan();
        }
        
        discoveredDevices.clear();
        isScanning = true;
        
        // TODO: Add proper scan filters for Raspberry Pi devices
        bluetoothLeScanner.startScan(scanCallback);
        
        // Stop scanning after 10 seconds
        mainHandler.postDelayed(this::stopScan, 10000);
    }
    
    /**
     * Stop scanning for BLE devices
     */
    public void stopScan() {
        if (isScanning) {
            bluetoothLeScanner.stopScan(scanCallback);
            isScanning = false;
        }
    }
    
    /**
     * Connect to a specific BLE device
     */
    public void connectToDevice(BluetoothDevice device) {
        if (bluetoothGatt != null) {
            bluetoothGatt.close();
        }
        
        bluetoothGatt = device.connectGatt(context, false, gattCallback);
        connectedDeviceName = device.getName() != null ? device.getName() : "Unknown Device";
    }
    
    /**
     * Disconnect from current device
     */
    public void disconnect() {
        if (bluetoothGatt != null) {
            bluetoothGatt.disconnect();
            bluetoothGatt.close();
            bluetoothGatt = null;
        }
        isConnected = false;
        connectedDeviceName = "";
    }
    
    /**
     * Send measurement data to Pi
     */
    public void sendMeasurementData(Object data) {
        if (!isConnected || bluetoothGatt == null) {
            notifyError("Not connected to device");
            return;
        }
        
        // TODO: Implement data sending via GATT characteristics
        // Convert data to JSON and send in chunks if needed
        String jsonData = gson.toJson(data);
        sendDataToCharacteristic(jsonData, MEASUREMENT_CHARACTERISTIC_UUID);
    }
    
    /**
     * Send chat message to Pi
     */
    public void sendChatMessage(String message) {
        if (!isConnected || bluetoothGatt == null) {
            notifyError("Not connected to device");
            return;
        }
        
        // TODO: Implement chat message sending
        sendDataToCharacteristic(message, CHAT_CHARACTERISTIC_UUID);
    }
    
    /**
     * Send data to a specific GATT characteristic
     */
    private void sendDataToCharacteristic(String data, String characteristicUuid) {
        // TODO: Implement GATT characteristic writing
        // Handle MTU limitations with chunked transmission
        BluetoothGattService service = bluetoothGatt.getService(UUID.fromString(SERVICE_UUID));
        if (service != null) {
            BluetoothGattCharacteristic characteristic = service.getCharacteristic(UUID.fromString(characteristicUuid));
            if (characteristic != null) {
                // TODO: Split data into chunks if larger than MTU
                characteristic.setValue(data.getBytes());
                bluetoothGatt.writeCharacteristic(characteristic);
            }
        }
    }
    
    /**
     * Setup notifications for incoming data
     */
    private void setupNotifications() {
        // TODO: Enable notifications for response characteristic
        BluetoothGattService service = bluetoothGatt.getService(UUID.fromString(SERVICE_UUID));
        if (service != null) {
            BluetoothGattCharacteristic characteristic = service.getCharacteristic(UUID.fromString(RESPONSE_CHARACTERISTIC_UUID));
            if (characteristic != null) {
                bluetoothGatt.setCharacteristicNotification(characteristic, true);
            }
        }
    }
    
    // GATT Callback
    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                isConnected = true;
                gatt.discoverServices();
                mainHandler.post(() -> {
                    if (callback != null) {
                        callback.onConnectionStateChanged(true, connectedDeviceName);
                    }
                });
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                isConnected = false;
                mainHandler.post(() -> {
                    if (callback != null) {
                        callback.onConnectionStateChanged(false, connectedDeviceName);
                    }
                });
            }
        }
        
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                setupNotifications();
            }
        }
        
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            // Handle incoming data from Pi
            byte[] data = characteristic.getValue();
            String receivedData = new String(data);
            mainHandler.post(() -> {
                if (callback != null) {
                    callback.onDataReceived(receivedData);
                }
            });
        }
    };
    
    // Scan Callback
    private final ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            BluetoothDevice device = result.getDevice();
            if (!discoveredDevices.contains(device)) {
                discoveredDevices.add(device);
                mainHandler.post(() -> {
                    if (callback != null) {
                        callback.onDeviceFound(device);
                    }
                });
            }
        }
        
        @Override
        public void onScanFailed(int errorCode) {
            mainHandler.post(() -> {
                if (callback != null) {
                    callback.onError("Scan failed with error: " + errorCode);
                }
            });
        }
    };
    
    private void notifyError(String error) {
        mainHandler.post(() -> {
            if (callback != null) {
                callback.onError(error);
            }
        });
    }
    
    // Getters
    public boolean isConnected() {
        return isConnected;
    }
    
    public boolean isScanning() {
        return isScanning;
    }
    
    public String getConnectedDeviceName() {
        return connectedDeviceName;
    }
    
    public List<BluetoothDevice> getDiscoveredDevices() {
        return new ArrayList<>(discoveredDevices);
    }
}
