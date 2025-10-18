package com.example.hifiwifi.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.hifiwifi.models.ChatMessage;
import com.example.hifiwifi.repository.ChatRepository;
import com.example.hifiwifi.viewmodels.BLEViewModel;

import java.util.List;

/**
 * ViewModel for managing chat functionality with SLM
 * Exposes LiveData for UI observation
 */
public class ChatViewModel extends AndroidViewModel {
    
    private ChatRepository chatRepository;
    private BLEViewModel bleViewModel;
    
    // LiveData for UI observation
    private MutableLiveData<List<ChatMessage>> messages;
    private MutableLiveData<Boolean> isWaitingForResponse;
    private MutableLiveData<String> errorMessage;
    
    public ChatViewModel(@NonNull Application application) {
        super(application);
        
        // Initialize repository
        chatRepository = new ChatRepository();
        
        // Initialize LiveData
        messages = new MutableLiveData<>();
        isWaitingForResponse = new MutableLiveData<>(false);
        errorMessage = new MutableLiveData<>();
        
        // Initialize with empty data
        messages.setValue(chatRepository.getMessages());
    }
    
    /**
     * Set BLE ViewModel for communication
     */
    public void setBLEViewModel(BLEViewModel bleViewModel) {
        this.bleViewModel = bleViewModel;
    }
    
    // LiveData getters
    public LiveData<List<ChatMessage>> getMessages() {
        return messages;
    }
    
    public LiveData<Boolean> getIsWaitingForResponse() {
        return isWaitingForResponse;
    }
    
    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }
    
    /**
     * Send a message to the SLM
     */
    public void sendMessage(String messageText, String roomContext) {
        if (messageText == null || messageText.trim().isEmpty()) {
            errorMessage.setValue("Message cannot be empty");
            return;
        }
        
        // Add user message to repository
        chatRepository.addUserMessage(messageText.trim(), roomContext);
        messages.setValue(chatRepository.getMessages());
        
        // Send to Pi via BLE if connected
        if (bleViewModel != null && bleViewModel.isConnected()) {
            isWaitingForResponse.setValue(true);
            bleViewModel.sendChatMessage(messageText.trim());
        } else {
            // TODO: Handle offline mode or show error
            errorMessage.setValue("Not connected to Raspberry Pi");
            isWaitingForResponse.setValue(false);
        }
    }
    
    /**
     * Add SLM response message
     */
    public void addSLMResponse(String responseText, String roomContext) {
        chatRepository.addSLMMessage(responseText, roomContext);
        messages.setValue(chatRepository.getMessages());
        isWaitingForResponse.setValue(false);
    }
    
    /**
     * Clear all chat messages
     */
    public void clearChat() {
        chatRepository.clearMessages();
        messages.setValue(chatRepository.getMessages());
        isWaitingForResponse.setValue(false);
    }
    
    /**
     * Get messages for a specific room
     */
    public List<ChatMessage> getMessagesForRoom(String roomContext) {
        return chatRepository.getMessagesForRoom(roomContext);
    }
    
    /**
     * Get the last message
     */
    public ChatMessage getLastMessage() {
        return chatRepository.getLastMessage();
    }
    
    /**
     * Check if there are any messages
     */
    public boolean hasMessages() {
        return chatRepository.hasMessages();
    }
    
    /**
     * Get message count
     */
    public int getMessageCount() {
        return chatRepository.getMessageCount();
    }
    
    /**
     * Clear error message
     */
    public void clearError() {
        errorMessage.setValue(null);
    }
    
    /**
     * Set waiting state (for external use)
     */
    public void setWaitingForResponse(boolean waiting) {
        isWaitingForResponse.setValue(waiting);
    }
}
