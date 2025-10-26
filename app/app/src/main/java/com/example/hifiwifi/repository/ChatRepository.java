package com.example.hifiwifi.repository;

import com.example.hifiwifi.models.ChatMessage;

import java.util.ArrayList;
import java.util.List;

/**
 * Repository for managing chat messages
 * Stores messages in-memory for current session
 */
public class ChatRepository {
    
    private List<ChatMessage> messages;
    
    public ChatRepository() {
        this.messages = new ArrayList<>();
    }
    
    /**
     * Add a new chat message
     */
    public void addMessage(ChatMessage message) {
        messages.add(message);
    }
    
    /**
     * Add a user message
     */
    public void addUserMessage(String messageText, String roomContext) {
        ChatMessage message = new ChatMessage(messageText, true, roomContext);
        addMessage(message);
    }
    
    /**
     * Add an SLM response message
     */
    public void addSLMMessage(String messageText, String roomContext) {
        ChatMessage message = new ChatMessage(messageText, false, roomContext);
        addMessage(message);
    }
    
    /**
     * Get all messages
     */
    public List<ChatMessage> getMessages() {
        return new ArrayList<>(messages);
    }
    
    /**
     * Get messages for a specific room
     */
    public List<ChatMessage> getMessagesForRoom(String roomContext) {
        List<ChatMessage> roomMessages = new ArrayList<>();
        for (ChatMessage message : messages) {
            if (roomContext.equals(message.getRoomContext())) {
                roomMessages.add(message);
            }
        }
        return roomMessages;
    }
    
    /**
     * Get the last message
     */
    public ChatMessage getLastMessage() {
        if (messages.isEmpty()) {
            return null;
        }
        return messages.get(messages.size() - 1);
    }
    
    /**
     * Clear all messages
     */
    public void clearMessages() {
        messages.clear();
    }
    
    /**
     * Get message count
     */
    public int getMessageCount() {
        return messages.size();
    }
    
    /**
     * Check if there are any messages
     */
    public boolean hasMessages() {
        return !messages.isEmpty();
    }
}
