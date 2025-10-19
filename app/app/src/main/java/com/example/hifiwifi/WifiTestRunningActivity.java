package com.example.hifiwifi;

import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.hifiwifi.viewmodels.MeasurementViewModel;
import com.example.hifiwifi.viewmodels.WifiTestViewModel;
import com.example.hifiwifi.models.NetworkMetrics;

public class WifiTestRunningActivity extends AppCompatActivity {

    private static final String TAG = "WifiTestRunningActivity";
    
    private TextView roomNameDisplay, speedText, latencyText, jitterText, packetLossText, strengthText, instructionText;
    private Button saveButton, rerunButton, cancelButton;
    private MeasurementViewModel measurementViewModel;
    private WifiTestViewModel wifiTestViewModel;
    private String currentRoomName;
    private String currentActivityType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wifi_test_running);

        // Initialize ViewModels
        measurementViewModel = new ViewModelProvider(this).get(MeasurementViewModel.class);
        wifiTestViewModel = new ViewModelProvider(this).get(WifiTestViewModel.class);

        // Initialize views
        roomNameDisplay = findViewById(R.id.roomNameDisplay);
        instructionText = findViewById(R.id.instructionText);
        speedText = findViewById(R.id.speedText);
        latencyText = findViewById(R.id.latencyText);
        jitterText = findViewById(R.id.jitterText);
        packetLossText = findViewById(R.id.packetLossText);
        strengthText = findViewById(R.id.strengthText);
        saveButton = findViewById(R.id.saveButton);
        rerunButton = findViewById(R.id.rerunButton);
        cancelButton = findViewById(R.id.cancelButton);

        // Get the passed room name and activity type from intent
        currentRoomName = getIntent().getStringExtra("ROOM_NAME");
        currentActivityType = getIntent().getStringExtra("ACTIVITY_TYPE");

        // Handle null safety
        if (currentRoomName == null || currentRoomName.isEmpty()) {
            currentRoomName = "Office";
        }
        
        if (currentActivityType == null || currentActivityType.isEmpty()) {
            Log.w(TAG, "Activity type not provided, defaulting to 'general'");
            currentActivityType = "general";
        }
        
        // Store in ViewModel
        wifiTestViewModel.setRoomName(currentRoomName);
        wifiTestViewModel.setActivityType(currentActivityType);
        
        Log.d(TAG, "Test configuration - " + wifiTestViewModel.getTestConfigurationSummary());

        // Set up UI with activity type display
        String activityDisplay = formatActivityType(currentActivityType);
        roomNameDisplay.setText(String.format("Testing Wi-Fi in %s (%s)", currentRoomName, activityDisplay));
        instructionText.setText("Please stand in the center of the room while we measure your connection...");

        // Initialize with placeholder values
        speedText.setText("Speed: 0 Mbps");
        latencyText.setText("Latency: 0 ms");
        jitterText.setText("Jitter: 0 ms");
        packetLossText.setText("Packet Loss: 0%");
        strengthText.setText("Wi-Fi Strength: 0 dBm");

        // Apply grow animations to buttons
        applyButtonAnimation(saveButton);
        applyButtonAnimation(rerunButton);
        applyButtonAnimation(cancelButton);

        // Set up button listeners
        saveButton.setOnClickListener(v -> saveResults());
        rerunButton.setOnClickListener(v -> rerunTest());
        cancelButton.setOnClickListener(v -> finish());

        // Set up observers
        setupObservers();

        // Start the measurement
        startMeasurement();
    }

    private void applyButtonAnimation(android.view.View view) {
        Animation scaleUp = AnimationUtils.loadAnimation(this, R.anim.button_scale_up);
        Animation scaleDown = AnimationUtils.loadAnimation(this, R.anim.button_scale_down);

        view.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    v.startAnimation(scaleUp);
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    v.startAnimation(scaleDown);
                    break;
            }
            return false;
        });
    }

    private void setupObservers() {
        measurementViewModel.getCurrentMetrics().observe(this, metrics -> {
            if (metrics != null) updateMetricsDisplay(metrics);
        });

        measurementViewModel.getIsMeasuring().observe(this, isMeasuring -> {
            if (isMeasuring != null) updateMeasurementState(isMeasuring);
        });

        measurementViewModel.getIsTestComplete().observe(this, isComplete -> {
            if (isComplete != null && isComplete) {
                instructionText.setText("Test completed! You can save or rerun the test.");
                rerunButton.setEnabled(true);
                saveButton.setEnabled(true);
            }
        });

        measurementViewModel.getErrorMessage().observe(this, error -> {
            if (error != null && !error.isEmpty()) {
                instructionText.setText("Error: " + error);
            }
        });
    }

    private void updateMetricsDisplay(NetworkMetrics metrics) {
        speedText.setText(String.format("Speed: %.1f Mbps", metrics.getCurrentBandwidthMbps()));
        latencyText.setText(String.format("Latency: %d ms", metrics.getCurrentLatencyMs()));
        jitterText.setText(String.format("Jitter: %.1f ms", metrics.getCurrentJitterMs()));
        packetLossText.setText(String.format("Packet Loss: %.1f%%", metrics.getCurrentPacketLossPercent()));
        strengthText.setText(String.format("Wi-Fi Strength: %d dBm", metrics.getCurrentSignalDbm()));
    }

    private void updateMeasurementState(boolean isMeasuring) {
        if (isMeasuring) {
            instructionText.setText("Please stand in the center of the room while we measure your connection...");
            rerunButton.setEnabled(false);
            saveButton.setEnabled(false);
        } else {
            rerunButton.setEnabled(true);
            saveButton.setEnabled(true);
        }
    }

    private void startMeasurement() {
        measurementViewModel.startSingleSpeedTest(currentRoomName, currentActivityType);
    }

    private void rerunTest() {
        measurementViewModel.rerunTest(currentRoomName, currentActivityType);
    }
    
    /**
     * Format activity type for display (convert underscores to spaces, capitalize)
     */
    private String formatActivityType(String activityType) {
        if (activityType == null || activityType.isEmpty()) {
            return "General";
        }
        
        // Replace underscores with spaces and capitalize each word
        String[] words = activityType.replace("_", " ").split(" ");
        StringBuilder formatted = new StringBuilder();
        
        for (String word : words) {
            if (word.length() > 0) {
                formatted.append(Character.toUpperCase(word.charAt(0)));
                if (word.length() > 1) {
                    formatted.append(word.substring(1).toLowerCase());
                }
                formatted.append(" ");
            }
        }
        
        return formatted.toString().trim();
    }

    /**
     * Save the measurement results and return to main activity
     * Classifications are automatically saved to the repository during measurement
     */
    private void saveResults() {
        Log.d(TAG, "Saving results - " + wifiTestViewModel.getTestConfigurationSummary());
        
        // Show confirmation message
        instructionText.setText("Results saved successfully!");
        
        // Give user brief moment to see confirmation, then close activity
        saveButton.postDelayed(() -> {
            Log.d(TAG, "Returning to main activity");
            finish(); // This returns to MainActivity
        }, 500); // 500ms delay so user sees the "saved" message
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Activity destroyed - cleaning up");
        if (measurementViewModel != null && isFinishing()) {
            measurementViewModel.stopMeasurement();
        }
    }
}
