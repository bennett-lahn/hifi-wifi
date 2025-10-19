package com.example.hifiwifi;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.hifiwifi.viewmodels.MeasurementViewModel;
import com.example.hifiwifi.models.NetworkMetrics;

public class WifiTestRunningActivity extends AppCompatActivity {

    private TextView roomNameDisplay, speedText, latencyText, jitterText, packetLossText, strengthText, instructionText;
    private Button cancelButton, rerunButton;
    private MeasurementViewModel measurementViewModel;
    private String currentRoomName;
    private String currentActivityType = "general";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wifi_test_running);

        // Initialize views
        roomNameDisplay = findViewById(R.id.roomNameDisplay);
        instructionText = findViewById(R.id.instructionText);
        speedText = findViewById(R.id.speedText);
        latencyText = findViewById(R.id.latencyText);
        jitterText = findViewById(R.id.jitterText);
        packetLossText = findViewById(R.id.packetLossText);
        strengthText = findViewById(R.id.strengthText);
        cancelButton = findViewById(R.id.cancelButton);
        rerunButton = findViewById(R.id.rerunButton);

        // Get the passed room name from intent
        currentRoomName = getIntent().getStringExtra("ROOM_NAME");

        // Handle null safety (default to Office)
        if (currentRoomName == null || currentRoomName.isEmpty()) {
            currentRoomName = "Office";
        }

        // Initialize ViewModel
        measurementViewModel = new ViewModelProvider(this).get(MeasurementViewModel.class);

        // Set up UI
        roomNameDisplay.setText("Testing Wi-Fi in " + currentRoomName);
        instructionText.setText("Please stand in the center of the room while we measure your connection...");

        // Initialize with placeholder values
        speedText.setText("Speed: 0 Mbps");
        latencyText.setText("Latency: 0 ms");
        jitterText.setText("Jitter: 0 ms");
        packetLossText.setText("Packet Loss: 0%");
        strengthText.setText("Wi-Fi Strength: 0 dBm");

        // Set up button listeners
        cancelButton.setOnClickListener(v -> finish());
        rerunButton.setOnClickListener(v -> rerunTest());

        // Set up observers
        setupObservers();

        // Start the measurement
        startMeasurement();
    }

    private void setupObservers() {
        // Observe current metrics for real-time updates
        measurementViewModel.getCurrentMetrics().observe(this, metrics -> {
            if (metrics != null) {
                updateMetricsDisplay(metrics);
            }
        });

        // Observe measurement state
        measurementViewModel.getIsMeasuring().observe(this, isMeasuring -> {
            if (isMeasuring != null) {
                updateMeasurementState(isMeasuring);
            }
        });

        // Observe test completion
        measurementViewModel.getIsTestComplete().observe(this, isComplete -> {
            if (isComplete != null && isComplete) {
                instructionText.setText("Test completed! You can rerun the test or go back.");
                rerunButton.setEnabled(true);
            }
        });

        // Observe errors
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
        } else {
            rerunButton.setEnabled(true);
        }
    }

    private void startMeasurement() {
        measurementViewModel.startSingleSpeedTest(currentRoomName, currentActivityType);
    }

    private void rerunTest() {
        measurementViewModel.rerunTest(currentRoomName, currentActivityType);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d("WifiTestRunningActivity", "Activity destroyed - cleaning up");
        // Only stop measurement when activity is actually destroyed (user navigated away)
        if (measurementViewModel != null && isFinishing()) {
            measurementViewModel.stopMeasurement();
        }
    }
}
