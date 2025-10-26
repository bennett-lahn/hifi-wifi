package com.example.hifiwifi.ui.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.hifiwifi.R;

/**
 * Fragment for WiFi measurement functionality - UI Skeleton Only
 * No services, no ViewModels, no data connections
 */
public class MeasurementFragment extends Fragment {
    
    private Spinner spinnerRooms;
    private Spinner spinnerActivityType;
    private Button buttonStartMeasurement;
    private TextView textSignalStrength;
    private TextView textLatency;
    private TextView textBandwidth;
    private TextView textCurrentRoom;
    private TextView textMeasurementStatus;
    
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_measurement, container, false);
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Initialize views
        spinnerRooms = view.findViewById(R.id.spinner_rooms);
        spinnerActivityType = view.findViewById(R.id.spinner_activity_type);
        buttonStartMeasurement = view.findViewById(R.id.button_start_measurement);
        textSignalStrength = view.findViewById(R.id.text_signal_strength);
        textLatency = view.findViewById(R.id.text_latency);
        textBandwidth = view.findViewById(R.id.text_bandwidth);
        textCurrentRoom = view.findViewById(R.id.text_current_room);
        textMeasurementStatus = view.findViewById(R.id.text_measurement_status);
        
        // Setup UI
        setupSpinners();
        setupButton();
        updateDisplay();
    }
    
    private void setupSpinners() {
        // Setup activity type spinner
        String[] activityTypes = {"gaming", "streaming", "video_call", "general"};
        ArrayAdapter<String> activityAdapter = new ArrayAdapter<>(requireContext(), 
                android.R.layout.simple_spinner_item, activityTypes);
        activityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerActivityType.setAdapter(activityAdapter);
        
        // Setup rooms spinner with mock data
        String[] mockRooms = {"Living Room", "Bedroom", "Kitchen", "Office"};
        ArrayAdapter<String> roomAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, mockRooms);
        roomAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRooms.setAdapter(roomAdapter);
    }
    
    private void setupButton() {
        buttonStartMeasurement.setOnClickListener(v -> {
            // Mock measurement toggle
            String currentText = buttonStartMeasurement.getText().toString();
            if (currentText.equals("Start Measurement")) {
                buttonStartMeasurement.setText("Stop Measurement");
                textMeasurementStatus.setText("Status: Measuring... (Mock)");
                updateMockMetrics();
            } else {
                buttonStartMeasurement.setText("Start Measurement");
                textMeasurementStatus.setText("Status: Stopped");
            }
        });
    }
    
    private void updateMockMetrics() {
        // Mock data for UI development
        textSignalStrength.setText("-45 dBm");
        textLatency.setText("25 ms");
        textBandwidth.setText("85.5 Mbps");
        textCurrentRoom.setText("Current Room: " + spinnerRooms.getSelectedItem().toString());
    }
    
    private void updateDisplay() {
        // Initialize with mock data
        textSignalStrength.setText("-100 dBm");
        textLatency.setText("0 ms");
        textBandwidth.setText("0.0 Mbps");
        textCurrentRoom.setText("Current Room: None");
        textMeasurementStatus.setText("Status: Stopped");
    }
}