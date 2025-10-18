package com.example.hifiwifi.ui.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.hifiwifi.R;

/**
 * Fragment for displaying measurement results - UI Skeleton Only
 * No services, no ViewModels, no data connections
 */
public class ResultsFragment extends Fragment {
    
    private TextView textTitle;
    private TextView textEmptyState;
    
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_results, container, false);
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Initialize views
        textTitle = view.findViewById(R.id.text_title);
        textEmptyState = view.findViewById(R.id.text_empty_state);
        
        // Setup UI with mock data
        setupUI();
    }
    
    private void setupUI() {
        textTitle.setText("Measurement Results");
        textEmptyState.setText("Mock Results:\n" +
                "• Living Room: A Grade (95%)\n" +
                "• Bedroom: B Grade (78%)\n" +
                "• Kitchen: C Grade (65%)\n\n" +
                "This is UI skeleton only - no real data");
    }
}