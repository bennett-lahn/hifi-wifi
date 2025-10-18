package com.example.hifiwifi.ui.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hifiwifi.R;
import com.example.hifiwifi.models.ChatMessage;
import com.example.hifiwifi.ui.adapters.ChatAdapter;
import com.example.hifiwifi.viewmodels.BLEViewModel;
import com.example.hifiwifi.viewmodels.ChatViewModel;

import java.util.List;

/**
 * Fragment for SLM chat functionality
 */
public class ChatFragment extends Fragment {
    
    private RecyclerView recyclerViewMessages;
    private EditText editTextMessage;
    private Button buttonSend;
    private TextView textEmptyState;
    private ProgressBar progressWaiting;
    
    private ChatViewModel chatViewModel;
    private BLEViewModel bleViewModel;
    private ChatAdapter chatAdapter;
    private List<ChatMessage> messages;
    
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_chat, container, false);
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Initialize views
        recyclerViewMessages = view.findViewById(R.id.recycler_view_messages);
        editTextMessage = view.findViewById(R.id.edit_text_message);
        buttonSend = view.findViewById(R.id.button_send);
        textEmptyState = view.findViewById(R.id.text_empty_state);
        progressWaiting = view.findViewById(R.id.progress_waiting);
        
        // Initialize ViewModels
        chatViewModel = new ViewModelProvider(this).get(ChatViewModel.class);
        bleViewModel = new ViewModelProvider(requireActivity()).get(BLEViewModel.class);
        
        // Connect ViewModels
        chatViewModel.setBLEViewModel(bleViewModel);
        
        // Setup UI
        setupRecyclerView();
        setupButton();
        setupObservers();
    }
    
    private void setupRecyclerView() {
        chatAdapter = new ChatAdapter(messages);
        recyclerViewMessages.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerViewMessages.setAdapter(chatAdapter);
    }
    
    private void setupButton() {
        buttonSend.setOnClickListener(v -> {
            String message = editTextMessage.getText().toString().trim();
            if (!message.isEmpty()) {
                // TODO: Get current room context
                String roomContext = "General";
                chatViewModel.sendMessage(message, roomContext);
                editTextMessage.setText("");
            }
        });
    }
    
    private void setupObservers() {
        // Observe messages
        chatViewModel.getMessages().observe(getViewLifecycleOwner(), messages -> {
            if (messages != null) {
                updateMessages(messages);
            }
        });
        
        // Observe waiting state
        chatViewModel.getIsWaitingForResponse().observe(getViewLifecycleOwner(), isWaiting -> {
            if (isWaiting != null) {
                updateWaitingState(isWaiting);
            }
        });
        
        // Observe error messages
        chatViewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                // TODO: Show error message to user
                chatViewModel.clearError();
            }
        });
        
        // Observe BLE data for SLM responses
        bleViewModel.getSlmRecommendation().observe(getViewLifecycleOwner(), response -> {
            if (response != null && !response.isEmpty()) {
                // TODO: Parse response and add as SLM message
                String roomContext = "General"; // TODO: Get actual room context
                chatViewModel.addSLMResponse(response, roomContext);
            }
        });
    }
    
    private void updateMessages(List<ChatMessage> messages) {
        this.messages = messages;
        chatAdapter.updateMessages(messages);
        
        // Scroll to bottom
        if (!messages.isEmpty()) {
            recyclerViewMessages.smoothScrollToPosition(messages.size() - 1);
        }
        
        // Show/hide empty state
        if (messages.isEmpty()) {
            textEmptyState.setVisibility(View.VISIBLE);
            recyclerViewMessages.setVisibility(View.GONE);
        } else {
            textEmptyState.setVisibility(View.GONE);
            recyclerViewMessages.setVisibility(View.VISIBLE);
        }
    }
    
    private void updateWaitingState(boolean isWaiting) {
        if (isWaiting) {
            progressWaiting.setVisibility(View.VISIBLE);
            buttonSend.setEnabled(false);
            editTextMessage.setEnabled(false);
        } else {
            progressWaiting.setVisibility(View.GONE);
            buttonSend.setEnabled(true);
            editTextMessage.setEnabled(true);
        }
    }
}
