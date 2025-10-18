package com.example.hifiwifi.ui.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.hifiwifi.R;

/**
 * Fragment for SLM chat functionality - UI Skeleton Only
 * No services, no ViewModels, no data connections
 */
public class ChatFragment extends Fragment {
    
    private TextView textTitle;
    private EditText editTextMessage;
    private Button buttonSend;
    private TextView textEmptyState;
    
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_chat, container, false);
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Initialize views
        textTitle = view.findViewById(R.id.text_title);
        editTextMessage = view.findViewById(R.id.edit_text_message);
        buttonSend = view.findViewById(R.id.button_send);
        textEmptyState = view.findViewById(R.id.text_empty_state);
        
        // Setup UI
        setupUI();
    }
    
    private void setupUI() {
        textTitle.setText("SLM Chat");
        textEmptyState.setText("SLM: Hello! I'm your Smart Location Manager. How can I help you optimize your WiFi today?\n\n" +
                "User: What's the WiFi signal like in the living room?\n\n" +
                "SLM: Based on your recent measurements, the living room has excellent WiFi coverage with -45 dBm signal strength. Perfect for gaming and streaming!\n\n" +
                "(This is UI skeleton only - no real chat functionality)");
        
        buttonSend.setOnClickListener(v -> {
            String message = editTextMessage.getText().toString().trim();
            if (!message.isEmpty()) {
                // Mock chat response
                textEmptyState.append("\n\nUser: " + message);
                textEmptyState.append("\n\nSLM: Mock response: " + message + " (UI only)");
                editTextMessage.setText("");
            }
        });
    }
}