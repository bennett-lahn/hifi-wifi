package com.example.hifiwifi.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.hifiwifi.models.ChatMessage;
import com.example.hifiwifi.repository.ClassificationRepository;
import com.example.hifiwifi.services.HTTPService;

import java.util.ArrayList;
import java.util.List;

/**
 * ViewModel for managing chat functionality with SLM
 * Exposes LiveData for UI observation
 * MOCK VERSION - Disconnected from actual services for UI development
 */
public class ChatViewModel extends AndroidViewModel {
    
    // LiveData for UI observation
    private MutableLiveData<List<ChatMessage>> messages;
    private MutableLiveData<Boolean> isWaitingForResponse;
    private MutableLiveData<String> errorMessage;
    
    // Mock data storage
    private List<ChatMessage> mockMessages;
    
    // Classification repository for context
    private ClassificationRepository classificationRepository;
    
    public ChatViewModel(@NonNull Application application) {
        super(application);
        
        // Initialize LiveData
        messages = new MutableLiveData<>();
        isWaitingForResponse = new MutableLiveData<>(false);
        errorMessage = new MutableLiveData<>();
        
        // Initialize classification repository
        classificationRepository = ClassificationRepository.getInstance();
        
        // Initialize mock data
        mockMessages = new ArrayList<>();
        initializeMockMessages();
        
        messages.setValue(mockMessages);
    }
    
    /**
     * Initialize mock chat messages for UI development
     */
    private void initializeMockMessages() {
        mockMessages.add(new ChatMessage(
            "Hello! I'm your Smart Location Manager. How can I help you optimize your WiFi today?",
            false, // isFromUser = false (SLM message)
            "system"
        ));
        
        mockMessages.add(new ChatMessage(
            "What's the WiFi signal like in the living room?",
            true, // isFromUser = true (User message)
            "living_room"
        ));
        
        mockMessages.add(new ChatMessage(
            "Based on your recent measurements, the living room has excellent WiFi coverage with -45 dBm signal strength. Perfect for gaming and streaming!",
            false, // isFromUser = false (SLM message)
            "living_room"
        ));
    }
    
    /**
     * Set BLE ViewModel for communication (MOCK VERSION - no-op)
     */
    public void setBLEViewModel(BLEViewModel bleViewModel) {
        // In mock version, we don't need to store the BLE ViewModel
        // This method exists for compatibility with the UI
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
     * Send a message to the SLM (MOCK VERSION)
     */
    public void sendMessage(String messageText, String roomContext) {
        if (messageText == null || messageText.trim().isEmpty()) {
            errorMessage.setValue("Message cannot be empty");
            return;
        }
        
        // Add user message to mock data
        ChatMessage userMessage = new ChatMessage(
            messageText.trim(),
            true, // isFromUser = true
            roomContext
        );
        mockMessages.add(userMessage);
        messages.setValue(new ArrayList<>(mockMessages));
        
        // Simulate waiting for response
        isWaitingForResponse.setValue(true);
        
        // Simulate SLM response after a delay
        simulateSLMResponse(messageText.trim(), roomContext);
    }
    
    /**
     * Simulate SLM response with mock data
     */
    private void simulateSLMResponse(String userMessage, String roomContext) {
        // Simulate response delay
        String response = generateMockResponse(userMessage, roomContext);
        
        ChatMessage slmMessage = new ChatMessage(
            response,
            false, // isFromUser = false (SLM message)
            roomContext
        );
        mockMessages.add(slmMessage);
        messages.setValue(new ArrayList<>(mockMessages));
        isWaitingForResponse.setValue(false);
    }
    
    /**
     * Generate mock SLM responses based on user input
     */
    private String generateMockResponse(String userMessage, String roomContext) {
        String lowerMessage = userMessage.toLowerCase();
        
        if (lowerMessage.contains("signal") || lowerMessage.contains("wifi")) {
            return "Based on your recent measurements, the " + roomContext + " has good WiFi coverage. " +
                   "Signal strength is around -50 dBm, which is suitable for most activities.";
        } else if (lowerMessage.contains("gaming")) {
            return "For gaming in the " + roomContext + ", I recommend checking your latency. " +
                   "Your current setup should work well for most games.";
        } else if (lowerMessage.contains("streaming")) {
            return "The " + roomContext + " has adequate bandwidth for streaming. " +
                   "You should be able to stream in HD without issues.";
        } else if (lowerMessage.contains("help")) {
            return "I can help you optimize your WiFi! Try asking about signal strength, " +
                   "gaming performance, or streaming quality in specific rooms.";
        } else {
            return "That's an interesting question about the " + roomContext + ". " +
                   "Let me analyze your WiFi data and get back to you with recommendations.";
        }
    }
    
    /**
     * Add SLM response message (MOCK VERSION)
     */
    public void addSLMResponse(String responseText, String roomContext) {
        ChatMessage slmMessage = new ChatMessage(
            responseText,
            false, // isFromUser = false (SLM message)
            roomContext
        );
        mockMessages.add(slmMessage);
        messages.setValue(new ArrayList<>(mockMessages));
        isWaitingForResponse.setValue(false);
    }
    
    /**
     * Clear all chat messages (MOCK VERSION)
     */
    public void clearChat() {
        mockMessages.clear();
        messages.setValue(new ArrayList<>(mockMessages));
        isWaitingForResponse.setValue(false);
    }
    
    /**
     * Get messages for a specific room (MOCK VERSION)
     */
    public List<ChatMessage> getMessagesForRoom(String roomContext) {
        List<ChatMessage> roomMessages = new ArrayList<>();
        for (ChatMessage message : mockMessages) {
            if (message.getRoomContext().equals(roomContext)) {
                roomMessages.add(message);
            }
        }
        return roomMessages;
    }
    
    /**
     * Get the last message (MOCK VERSION)
     */
    public ChatMessage getLastMessage() {
        if (mockMessages.isEmpty()) {
            return null;
        }
        return mockMessages.get(mockMessages.size() - 1);
    }
    
    /**
     * Check if there are any messages (MOCK VERSION)
     */
    public boolean hasMessages() {
        return !mockMessages.isEmpty();
    }
    
    /**
     * Get message count (MOCK VERSION)
     */
    public int getMessageCount() {
        return mockMessages.size();
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
    
    /**
     * Get classification context as JSON for SLM prompt
     * This provides the SLM with WiFi measurement and classification data
     */
    public String getClassificationContext() {
        return classificationRepository.toJson();
    }
    
    /**
     * Get classification context as pretty-printed JSON
     */
    public String getClassificationContextPretty() {
        return classificationRepository.toJsonPretty();
    }
    
    /**
     * Get the classification repository instance
     */
    public ClassificationRepository getClassificationRepository() {
        return classificationRepository;
    }
    
    /**
     * Check if there is any classification data available for context
     */
    public boolean hasClassificationContext() {
        return classificationRepository.getCount() > 0;
    }
    
    /**
     * Send message with classification context to SLM via HTTPService
     * This is the production version that sends to actual Raspberry Pi
     * 
     * @param messageText User's message
     * @param roomContext Current room context
     * @param httpService HTTPService instance for communication
     * @param responseCallback Callback to receive SLM response
     */
    public void sendMessageToSLM(
            String messageText, 
            String roomContext, 
            HTTPService httpService,
            SLMResponseCallback responseCallback
    ) {
        if (messageText == null || messageText.trim().isEmpty()) {
            errorMessage.setValue("Message cannot be empty");
            if (responseCallback != null) {
                responseCallback.onError("Message cannot be empty");
            }
            return;
        }
        
        // Set waiting state
        isWaitingForResponse.setValue(true);
        
        // Note: Classification context is not sent with chat queries in current Flask API version
        // The Flask /chat endpoint only accepts "query" and optional "format_json"
        // To include context, Flask API would need to be enhanced
        
        // Send chat query to Pi via HTTPService
        httpService.sendChatQuery(
            messageText.trim(),
            new HTTPService.HTTPCallback() {
                @Override
                public void onExplanationReceived(String explanation) {
                    // Response received from SLM
                    isWaitingForResponse.setValue(false);
                    if (responseCallback != null) {
                        responseCallback.onResponse(explanation);
                    }
                }
                
                @Override
                public void onHealthCheckSuccess() {
                    // Not used for chat queries
                }
                
                @Override
                public void onError(String error) {
                    // Error occurred
                    isWaitingForResponse.setValue(false);
                    errorMessage.setValue("Chat error: " + error);
                    if (responseCallback != null) {
                        responseCallback.onError(error);
                    }
                }
            }
        );
    }
    
    /**
     * Callback interface for SLM responses
     */
    public interface SLMResponseCallback {
        void onResponse(String response);
        void onError(String error);
    }
    
    /**
     * Send message with classification context to SLM (MOCK VERSION)
     * In production, this would send both the user message and classification context to the SLM
     */
    public void sendMessageWithContext(String messageText, String roomContext) {
        if (messageText == null || messageText.trim().isEmpty()) {
            errorMessage.setValue("Message cannot be empty");
            return;
        }
        
        // Add user message to mock data
        ChatMessage userMessage = new ChatMessage(
            messageText.trim(),
            true, // isFromUser = true
            roomContext
        );
        mockMessages.add(userMessage);
        messages.setValue(new ArrayList<>(mockMessages));
        
        // Simulate waiting for response
        isWaitingForResponse.setValue(true);
        
        // In production version, you would send this to the SLM:
        // String context = getClassificationContext();
        // String fullPrompt = "Context: " + context + "\nUser Query: " + messageText;
        // sendToSLM(fullPrompt);
        
        // For now, simulate SLM response with mock data
        simulateSLMResponse(messageText.trim(), roomContext);
    }
}
