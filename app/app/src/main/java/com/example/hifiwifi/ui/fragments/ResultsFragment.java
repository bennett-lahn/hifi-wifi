package com.example.hifiwifi.ui.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hifiwifi.R;
import com.example.hifiwifi.models.ClassificationResult;
import com.example.hifiwifi.ui.adapters.ClassificationAdapter;
import com.example.hifiwifi.viewmodels.BLEViewModel;
import com.example.hifiwifi.viewmodels.MeasurementViewModel;

import java.util.List;

/**
 * Fragment for displaying classification results and sending data to Pi
 */
public class ResultsFragment extends Fragment {
    
    private RecyclerView recyclerViewResults;
    private Button buttonSendToPi;
    private TextView textEmptyState;
    
    private MeasurementViewModel measurementViewModel;
    private BLEViewModel bleViewModel;
    private ClassificationAdapter classificationAdapter;
    private List<ClassificationResult> classifications;
    
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_results, container, false);
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Initialize views
        recyclerViewResults = view.findViewById(R.id.recycler_view_results);
        buttonSendToPi = view.findViewById(R.id.button_send_to_pi);
        textEmptyState = view.findViewById(R.id.text_empty_state);
        
        // Initialize ViewModels
        measurementViewModel = new ViewModelProvider(this).get(MeasurementViewModel.class);
        bleViewModel = new ViewModelProvider(requireActivity()).get(BLEViewModel.class);
        
        // Setup UI
        setupRecyclerView();
        setupButton();
        setupObservers();
    }
    
    private void setupRecyclerView() {
        classificationAdapter = new ClassificationAdapter(classifications);
        recyclerViewResults.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerViewResults.setAdapter(classificationAdapter);
    }
    
    private void setupButton() {
        buttonSendToPi.setOnClickListener(v -> {
            if (bleViewModel.isConnected()) {
                // Send measurement data to Pi
                String jsonData = measurementViewModel.exportMeasurementsToJSON();
                bleViewModel.sendDataToPi(jsonData);
                Toast.makeText(requireContext(), "Data sent to Raspberry Pi", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(requireContext(), "Not connected to Raspberry Pi", Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void setupObservers() {
        // Observe classifications
        measurementViewModel.getClassifications().observe(getViewLifecycleOwner(), classifications -> {
            if (classifications != null) {
                updateClassifications(classifications);
            }
        });
        
        // Observe BLE connection state
        bleViewModel.getConnectionState().observe(getViewLifecycleOwner(), connectionState -> {
            if (connectionState != null) {
                updateSendButtonState(connectionState.isConnected());
            }
        });
    }
    
    private void updateClassifications(List<ClassificationResult> classifications) {
        this.classifications = classifications;
        classificationAdapter.updateClassifications(classifications);
        
        // Show/hide empty state
        if (classifications.isEmpty()) {
            textEmptyState.setVisibility(View.VISIBLE);
            recyclerViewResults.setVisibility(View.GONE);
        } else {
            textEmptyState.setVisibility(View.GONE);
            recyclerViewResults.setVisibility(View.VISIBLE);
        }
    }
    
    private void updateSendButtonState(boolean isConnected) {
        buttonSendToPi.setEnabled(isConnected);
        if (isConnected) {
            buttonSendToPi.setText("Send to Raspberry Pi");
        } else {
            buttonSendToPi.setText("Connect to Pi First");
        }
    }
}
