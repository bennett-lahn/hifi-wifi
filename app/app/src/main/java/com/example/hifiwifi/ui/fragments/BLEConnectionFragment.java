package com.example.hifiwifi.ui.fragments;

import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hifiwifi.R;
import com.example.hifiwifi.models.BLEConnectionState;
import com.example.hifiwifi.ui.adapters.BLEDeviceAdapter;
import com.example.hifiwifi.viewmodels.BLEViewModel;

import java.util.List;

/**
 * Fragment for BLE connection management
 */
public class BLEConnectionFragment extends Fragment {
    
    private TextView textConnectionStatus;
    private TextView textDeviceName;
    private Button buttonScan;
    private Button buttonDisconnect;
    private RecyclerView recyclerViewDevices;
    private TextView textScanStatus;
    
    private BLEViewModel bleViewModel;
    private BLEDeviceAdapter deviceAdapter;
    private List<BluetoothDevice> availableDevices;
    
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_ble_connection, container, false);
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Initialize views
        textConnectionStatus = view.findViewById(R.id.text_connection_status);
        textDeviceName = view.findViewById(R.id.text_device_name);
        buttonScan = view.findViewById(R.id.button_scan);
        buttonDisconnect = view.findViewById(R.id.button_disconnect);
        recyclerViewDevices = view.findViewById(R.id.recycler_view_devices);
        textScanStatus = view.findViewById(R.id.text_scan_status);
        
        // Initialize ViewModel
        bleViewModel = new ViewModelProvider(this).get(BLEViewModel.class);
        
        // Setup UI
        setupRecyclerView();
        setupButtons();
        setupObservers();
    }
    
    private void setupRecyclerView() {
        deviceAdapter = new BLEDeviceAdapter(availableDevices, new BLEDeviceAdapter.OnDeviceClickListener() {
            @Override
            public void onDeviceClick(BluetoothDevice device) {
                bleViewModel.connect(device);
            }
        });
        
        recyclerViewDevices.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerViewDevices.setAdapter(deviceAdapter);
    }
    
    private void setupButtons() {
        buttonScan.setOnClickListener(v -> {
            if (bleViewModel.isScanning()) {
                bleViewModel.stopScan();
            } else {
                bleViewModel.startScan();
            }
        });
        
        buttonDisconnect.setOnClickListener(v -> {
            bleViewModel.disconnect();
        });
    }
    
    private void setupObservers() {
        // Observe connection state
        bleViewModel.getConnectionState().observe(getViewLifecycleOwner(), connectionState -> {
            if (connectionState != null) {
                updateConnectionStatus(connectionState);
            }
        });
        
        // Observe available devices
        bleViewModel.getAvailableDevices().observe(getViewLifecycleOwner(), devices -> {
            if (devices != null) {
                updateAvailableDevices(devices);
            }
        });
        
        // Observe error messages
        bleViewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                textScanStatus.setText("Error: " + error);
                bleViewModel.clearError();
            }
        });
    }
    
    private void updateConnectionStatus(BLEConnectionState connectionState) {
        if (connectionState.isConnected()) {
            textConnectionStatus.setText("Connected");
            textDeviceName.setText("Device: " + connectionState.getDeviceName());
            buttonDisconnect.setEnabled(true);
            buttonScan.setEnabled(false);
        } else if (connectionState.isConnecting()) {
            textConnectionStatus.setText("Connecting...");
            textDeviceName.setText("Device: " + connectionState.getDeviceName());
            buttonDisconnect.setEnabled(false);
            buttonScan.setEnabled(false);
        } else {
            textConnectionStatus.setText("Disconnected");
            textDeviceName.setText("Device: None");
            buttonDisconnect.setEnabled(false);
            buttonScan.setEnabled(true);
        }
        
        if (!connectionState.getErrorMessage().isEmpty()) {
            textScanStatus.setText("Error: " + connectionState.getErrorMessage());
        }
    }
    
    private void updateAvailableDevices(List<BluetoothDevice> devices) {
        this.availableDevices = devices;
        deviceAdapter.updateDevices(devices);
        
        if (bleViewModel.isScanning()) {
            textScanStatus.setText("Scanning... Found " + devices.size() + " devices");
            buttonScan.setText("Stop Scan");
        } else {
            textScanStatus.setText("Tap 'Scan for Devices' to start");
            buttonScan.setText("Scan for Devices");
        }
    }
}
