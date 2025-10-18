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
import androidx.lifecycle.ViewModelProvider;

import com.example.hifiwifi.R;
import com.example.hifiwifi.models.NetworkMetrics;
import com.example.hifiwifi.repository.RoomRepository;
import com.example.hifiwifi.viewmodels.MeasurementViewModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Fragment for WiFi measurement functionality
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
    
    private MeasurementViewModel measurementViewModel;
    private RoomRepository roomRepository;
    private List<RoomRepository.Room> rooms;
    
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
        
        // Initialize ViewModel
        measurementViewModel = new ViewModelProvider(this).get(MeasurementViewModel.class);
        
        // Initialize repository
        roomRepository = new RoomRepository(requireContext());
        
        // Setup UI
        setupSpinners();
        setupButton();
        setupObservers();
        
        // Load rooms
        loadRooms();
    }
    
    private void setupSpinners() {
        // Setup activity type spinner
        String[] activityTypes = {"gaming", "streaming", "video_call", "general"};
        ArrayAdapter<String> activityAdapter = new ArrayAdapter<>(requireContext(), 
                android.R.layout.simple_spinner_item, activityTypes);
        activityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerActivityType.setAdapter(activityAdapter);
        
        // Setup rooms spinner
        ArrayAdapter<RoomRepository.Room> roomAdapter = new ArrayAdapter<RoomRepository.Room>(requireContext(),
                android.R.layout.simple_spinner_item, new ArrayList<>()) {
            @Override
            public String toString() {
                return getItem(0) != null ? getItem(0).getName() : "";
            }
        };
        roomAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRooms.setAdapter(roomAdapter);
    }
    
    private void setupButton() {
        buttonStartMeasurement.setOnClickListener(v -> {
            if (measurementViewModel.getIsMeasuring().getValue() != null && 
                measurementViewModel.getIsMeasuring().getValue()) {
                // Stop measurement
                measurementViewModel.stopMeasurement();
            } else {
                // Start measurement
                RoomRepository.Room selectedRoom = (RoomRepository.Room) spinnerRooms.getSelectedItem();
                String activityType = (String) spinnerActivityType.getSelectedItem();
                
                if (selectedRoom != null) {
                    measurementViewModel.startMeasurement(
                        selectedRoom.getId(), 
                        selectedRoom.getName(), 
                        activityType
                    );
                } else {
                    // TODO: Show error message
                }
            }
        });
    }
    
    private void setupObservers() {
        // Observe current metrics
        measurementViewModel.getCurrentMetrics().observe(getViewLifecycleOwner(), metrics -> {
            if (metrics != null) {
                updateMetricsDisplay(metrics);
            }
        });
        
        // Observe measurement status
        measurementViewModel.getIsMeasuring().observe(getViewLifecycleOwner(), isMeasuring -> {
            if (isMeasuring != null) {
                updateMeasurementStatus(isMeasuring);
            }
        });
        
        // Observe error messages
        measurementViewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                // TODO: Show error message to user
                textMeasurementStatus.setText("Error: " + error);
                measurementViewModel.clearError();
            }
        });
    }
    
    private void loadRooms() {
        rooms = roomRepository.getRooms();
        ArrayAdapter<RoomRepository.Room> adapter = (ArrayAdapter<RoomRepository.Room>) spinnerRooms.getAdapter();
        adapter.clear();
        adapter.addAll(rooms);
        adapter.notifyDataSetChanged();
    }
    
    private void updateMetricsDisplay(NetworkMetrics metrics) {
        textSignalStrength.setText(metrics.getCurrentSignalDbm() + " dBm");
        textLatency.setText(metrics.getCurrentLatencyMs() + " ms");
        textBandwidth.setText(String.format("%.1f Mbps", metrics.getCurrentBandwidthMbps()));
        textCurrentRoom.setText("Current Room: " + metrics.getCurrentRoomName());
    }
    
    private void updateMeasurementStatus(boolean isMeasuring) {
        if (isMeasuring) {
            buttonStartMeasurement.setText("Stop Measurement");
            textMeasurementStatus.setText("Status: Measuring...");
        } else {
            buttonStartMeasurement.setText("Start Measurement");
            textMeasurementStatus.setText("Status: Stopped");
        }
    }
}
