package com.example.hifiwifi.ui.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.hifiwifi.R;
import com.example.hifiwifi.viewmodels.BLEViewModel;

/**
 * Fragment for displaying SLM recommendations
 */
public class RecommendationFragment extends Fragment {
    
    private TextView textTitle;
    private TextView textRecommendation;
    private TextView textEmptyState;
    
    private BLEViewModel bleViewModel;
    
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_recommendation, container, false);
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Initialize views
        textTitle = view.findViewById(R.id.text_title);
        textRecommendation = view.findViewById(R.id.text_recommendation);
        textEmptyState = view.findViewById(R.id.text_empty_state);
        
        // Initialize ViewModel
        bleViewModel = new ViewModelProvider(requireActivity()).get(BLEViewModel.class);
        
        // Setup observers
        setupObservers();
    }
    
    private void setupObservers() {
        // Observe SLM recommendations
        bleViewModel.getSlmRecommendation().observe(getViewLifecycleOwner(), recommendation -> {
            if (recommendation != null && !recommendation.isEmpty()) {
                updateRecommendation(recommendation);
            } else {
                showEmptyState();
            }
        });
    }
    
    private void updateRecommendation(String recommendation) {
        textRecommendation.setText(recommendation);
        textRecommendation.setVisibility(View.VISIBLE);
        textEmptyState.setVisibility(View.GONE);
    }
    
    private void showEmptyState() {
        textRecommendation.setVisibility(View.GONE);
        textEmptyState.setVisibility(View.VISIBLE);
    }
}
