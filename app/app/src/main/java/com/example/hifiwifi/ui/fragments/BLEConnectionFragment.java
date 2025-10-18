package com.example.hifiwifi.ui.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.hifiwifi.R;

/**
 * Fragment for BLE connection management - UI Skeleton Only
 * No services, no ViewModels, no data connections
 */
public class BLEConnectionFragment extends Fragment {
    
    private TextView textConnectionStatus;
    private TextView textDeviceName;
    private Button buttonScan;
    private Button buttonDisconnect;
    private TextView textScanStatus;
    
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
        textScanStatus = view.findViewById(R.id.text_scan_status);
        
        // Setup UI
        setupUI();
    }
    
    private void setupUI() {
        textConnectionStatus.setText("Disconnected (Mock)");
        textDeviceName.setText("No device connected");
        textScanStatus.setText("No devices found\n\nThis is UI skeleton only - no real BLE scanning");
        
        buttonScan.setOnClickListener(v -> {
            textScanStatus.setText("Mock devices found:\n• Raspberry Pi 1\n• Raspberry Pi 2\n\n(UI only - no real scanning)");
        });
        
        buttonDisconnect.setOnClickListener(v -> {
            textConnectionStatus.setText("Disconnected (Mock)");
            textDeviceName.setText("No device connected");
        });
    }
}