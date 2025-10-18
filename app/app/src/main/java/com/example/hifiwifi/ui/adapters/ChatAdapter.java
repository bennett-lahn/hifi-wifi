package com.example.hifiwifi.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hifiwifi.R;
import com.example.hifiwifi.models.ChatMessage;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * RecyclerView adapter for chat messages
 * Supports two view types: user messages and SLM messages
 */
public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    
    private static final int VIEW_TYPE_USER = 1;
    private static final int VIEW_TYPE_SLM = 2;
    
    private List<ChatMessage> messages;
    private SimpleDateFormat timeFormat;
    
    public ChatAdapter(List<ChatMessage> messages) {
        this.messages = messages != null ? new ArrayList<>(messages) : new ArrayList<>();
        this.timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
    }
    
    @Override
    public int getItemViewType(int position) {
        ChatMessage message = messages.get(position);
        return message.isFromUser() ? VIEW_TYPE_USER : VIEW_TYPE_SLM;
    }
    
    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        
        if (viewType == VIEW_TYPE_USER) {
            View view = inflater.inflate(R.layout.item_chat_user, parent, false);
            return new UserMessageViewHolder(view);
        } else {
            View view = inflater.inflate(R.layout.item_chat_slm, parent, false);
            return new SLMMessageViewHolder(view);
        }
    }
    
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatMessage message = messages.get(position);
        
        if (holder instanceof UserMessageViewHolder) {
            ((UserMessageViewHolder) holder).bind(message);
        } else if (holder instanceof SLMMessageViewHolder) {
            ((SLMMessageViewHolder) holder).bind(message);
        }
    }
    
    @Override
    public int getItemCount() {
        return messages.size();
    }
    
    public void updateMessages(List<ChatMessage> newMessages) {
        this.messages = newMessages != null ? new ArrayList<>(newMessages) : new ArrayList<>();
        notifyDataSetChanged();
    }
    
    static class UserMessageViewHolder extends RecyclerView.ViewHolder {
        private TextView textMessage;
        
        public UserMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            textMessage = itemView.findViewById(R.id.text_message);
        }
        
        public void bind(ChatMessage message) {
            textMessage.setText(message.getMessageText());
        }
    }
    
    static class SLMMessageViewHolder extends RecyclerView.ViewHolder {
        private TextView textMessage;
        
        public SLMMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            textMessage = itemView.findViewById(R.id.text_message);
        }
        
        public void bind(ChatMessage message) {
            textMessage.setText(message.getMessageText());
        }
    }
}
