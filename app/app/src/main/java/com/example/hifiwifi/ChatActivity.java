package com.example.hifiwifi;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.hifiwifi.classifier.ClassificationResult;
import com.example.hifiwifi.repository.ClassificationRepository;
import com.example.hifiwifi.services.HTTPService;
import com.example.hifiwifi.viewmodels.ChatViewModel;
import com.example.hifiwifi.viewmodels.RoomSelectViewModel;

import java.util.List;

import java.util.ArrayList;
import java.util.List;

public class ChatActivity extends AppCompatActivity {

    private static final String TAG = "ChatActivity";

    private LinearLayout chatContainer;
    private EditText messageInput;
    private Button sendButton;
    private ScrollView chatScroll;
    private ImageButton closeButton;
    private String roomName;

    // Services and ViewModels
    private HTTPService httpService;
    private RoomSelectViewModel roomSelectViewModel;
    private ChatViewModel chatViewModel;
    private ClassificationRepository classificationRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        chatContainer = findViewById(R.id.chatContainer);
        messageInput = findViewById(R.id.messageInput);
        sendButton = findViewById(R.id.sendButton);
        chatScroll = findViewById(R.id.chatScroll);
        closeButton = findViewById(R.id.closeButton);

        // Initialize ViewModels
        roomSelectViewModel = new ViewModelProvider(this).get(RoomSelectViewModel.class);
        chatViewModel = new ViewModelProvider(this).get(ChatViewModel.class);

        // Get classification repository
        classificationRepository = ClassificationRepository.getInstance();

        // Initialize HTTPService
        httpService = new HTTPService();
        Log.d(TAG, "HTTPService initialized with base URL: " + httpService.getBaseUrl());

        // Get room name from intent or ViewModel
        roomName = getIntent().getStringExtra("ROOM_NAME");
        if (roomName == null || roomName.isEmpty()) {
            roomName = roomSelectViewModel.getCurrentSelectedRoom();
            if (roomName == null) {
                roomName = "Office"; // Default fallback
            }
        }

        Log.d(TAG, "ChatActivity opened for room: " + roomName);

        // Welcome message
        addMessage("👋 Hi! You're now viewing data for: " + roomName, false);

        // Start HTTP service and send classification data
        startHTTPServiceAndSendData();

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
                sendMessageToSLM(message);
            }
        });
    }

    /**
     * Start HTTP service and send classification data to Raspberry Pi
     */
    private void startHTTPServiceAndSendData() {
        Log.d(TAG, "Starting HTTP service and preparing to send data");

        // First check if Pi is reachable
        addMessage("🔍 Checking connection to Raspberry Pi...", false);

        httpService.checkHealth(new HTTPService.HTTPCallback() {
            @Override
            public void onHealthCheckSuccess() {
                Log.i(TAG, "Health check successful - Pi is reachable");
                addMessage("✅ Connected to Raspberry Pi!", false);

                // Now send classification data for the selected room
                sendClassificationDataToSLM();
            }

            @Override
            public void onExplanationReceived(String explanation) {
                // Not used for health check
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Health check failed: " + error);
                addMessage("❌ Cannot reach Raspberry Pi. Using local mode.", false);
                Toast.makeText(ChatActivity.this,
                    "Cannot connect to Pi. Check connection.",
                    Toast.LENGTH_LONG).show();
            }
        });
    }

    /**
     * Send classification data for the selected room to the SLM
     */
    private void sendClassificationDataToSLM() {
        Log.d(TAG, "Sending classification data for room: " + roomName);

        // Get classifications for the selected room
        List<ClassificationResult> roomClassifications =
            classificationRepository.getClassificationsForRoom(roomName);

        if (roomClassifications.isEmpty()) {
            Log.w(TAG, "No classification data available for room: " + roomName);
            addMessage("ℹ️ No measurement data available for " + roomName +
                      ". Please run a WiFi test first.", false);
            return;
        }

        Log.d(TAG, "Found " + roomClassifications.size() + " classification(s) for " + roomName);

        // Get the most recent classification
        ClassificationResult latestClassification = roomClassifications.get(roomClassifications.size() - 1);

        // Prepare data for HTTP request
        String activity = latestClassification.getActivityType();
        String overallClass = latestClassification.getOverallClassification().name().toLowerCase();

        // Convert metric classifications to lowercase strings
        String signalClass = latestClassification.getMetricClassification()
            .getSignalStrengthClassification().name().toLowerCase();
        String latencyClass = latestClassification.getMetricClassification()
            .getLatencyClassification().name().toLowerCase();
        String bandwidthClass = latestClassification.getMetricClassification()
            .getBandwidthClassification().name().toLowerCase();

        // Determine recommendation action based on classification
        String action = determineRecommendationAction(latestClassification);
        String targetLocation = action.equals("move_location") ? "closer to router" : null;

        Log.d(TAG, "Sending explanation request - Activity: " + activity +
                   ", Overall: " + overallClass + ", Action: " + action);

        addMessage("📊 Analyzing WiFi data for " + roomName + "...", false);

        // Send explanation request to Pi
        httpService.requestExplanation(
            roomName,
            activity,
            signalClass,
            latencyClass,
            bandwidthClass,
            action,
            targetLocation,
            new HTTPService.HTTPCallback() {
                @Override
                public void onExplanationReceived(String explanation) {
                    Log.i(TAG, "Received explanation from SLM");
                    addMessage(explanation, false);
                }

                @Override
                public void onHealthCheckSuccess() {
                    // Not used for explanation request
                }

                @Override
                public void onError(String error) {
                    Log.e(TAG, "Explanation request failed: " + error);
                    addMessage("⚠️ Error getting AI analysis: " + error, false);
                }
            }
        );
    }

    /**
     * Determine recommendation action based on classification
     */
    private String determineRecommendationAction(ClassificationResult classification) {
        // If acceptable for activity, recommend staying
        if (classification.isAcceptableForActivity()) {
            return "stay_current";
        }

        // If signal strength is poor, recommend moving
        int signalScore = classification.getMetricClassification()
            .getSignalStrengthClassification().getScore();
        if (signalScore <= 2) {
            return "move_location";
        }

        // Otherwise recommend staying but with awareness of issues
        return "stay_current";
    }

    /**
     * Send user message to SLM with classification context
     */
    private void sendMessageToSLM(String message) {
        Log.d(TAG, "User message: " + message);

        // Get classification context
        String context = chatViewModel.getClassificationContext();

        // For now, use mock response
        // TODO: Send actual message with context to SLM
        addMessage("🤔 Thinking...", false);

        // Simulate processing delay
        chatScroll.postDelayed(() -> {
            addMessage("This is a mock AI reply for " + roomName +
                      ". Your message: \"" + message + "\"", false);
        }, 1000);
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up HTTP service
        if (httpService != null) {
            Log.d(TAG, "Shutting down HTTP service");
            httpService.shutdown();
        }
    }
}
