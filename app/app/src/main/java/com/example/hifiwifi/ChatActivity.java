package com.example.hifiwifi;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class ChatActivity extends AppCompatActivity {

    private LinearLayout chatContainer;
    private EditText messageInput;
    private Button sendButton;
    private ScrollView chatScroll;
    private ImageButton closeButton;
    private String roomName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        chatContainer = findViewById(R.id.chatContainer);
        messageInput = findViewById(R.id.messageInput);
        sendButton = findViewById(R.id.sendButton);
        chatScroll = findViewById(R.id.chatScroll);
        closeButton = findViewById(R.id.closeButton);

        roomName = getIntent().getStringExtra("ROOM_NAME");
        if (roomName == null) roomName = "Office";

        // Welcome message
        addMessage("👋 Hi! You’re now viewing data for: " + roomName, false);

        // Close button returns to room select screen
        closeButton.setOnClickListener(v -> {
            Intent intent = new Intent(ChatActivity.this, RoomSelectActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        });

        sendButton.setOnClickListener(v -> {
            String message = messageInput.getText().toString().trim();
            if (!message.isEmpty()) {
                addMessage(message, true);
                messageInput.setText("");
                addMessage("This is a placeholder AI reply for " + roomName + ".", false);
            }
        });
    }

    private void addMessage(String message, boolean isUser) {
        TextView msgView = new TextView(this);
        msgView.setText(message);
        msgView.setTextSize(16);
        msgView.setPadding(20, 10, 20, 10);
        msgView.setTextColor(isUser ? 0xFFFFFFFF : 0xFF000000);
        msgView.setBackgroundResource(isUser ? R.drawable.bubble_user : R.drawable.bubble_ai);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(10, 10, 10, 10);
        params.gravity = isUser ? android.view.Gravity.END : android.view.Gravity.START;
        msgView.setLayoutParams(params);

        chatContainer.addView(msgView);

        chatScroll.post(() -> chatScroll.fullScroll(ScrollView.FOCUS_DOWN));
    }
}
