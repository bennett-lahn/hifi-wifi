package com.example.hifiwifi.models;

/**
 * Data model representing a chat message in the SLM conversation
 */
public class ChatMessage {
    private String messageId;
    private String messageText;
    private boolean isFromUser;
    private long timestamp;
    private String roomContext; // Room name for context

    public ChatMessage() {
        // Default constructor
    }

    public ChatMessage(String messageText, boolean isFromUser, String roomContext) {
        this.messageId = String.valueOf(System.currentTimeMillis());
        this.messageText = messageText;
        this.isFromUser = isFromUser;
        this.timestamp = System.currentTimeMillis();
        this.roomContext = roomContext;
    }

    // Getters and Setters
    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getMessageText() {
        return messageText;
    }

    public void setMessageText(String messageText) {
        this.messageText = messageText;
    }

    public boolean isFromUser() {
        return isFromUser;
    }

    public void setFromUser(boolean fromUser) {
        isFromUser = fromUser;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getRoomContext() {
        return roomContext;
    }

    public void setRoomContext(String roomContext) {
        this.roomContext = roomContext;
    }
}
