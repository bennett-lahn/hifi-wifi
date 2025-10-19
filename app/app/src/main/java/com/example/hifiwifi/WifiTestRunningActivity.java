package com.example.hifiwifi;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class WifiTestRunningActivity extends AppCompatActivity {

    private TextView roomNameDisplay, speedText, latencyText, jitterText, packetLossText, strengthText, instructionText;
    private Button cancelButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wifi_test_running);

        roomNameDisplay = findViewById(R.id.roomNameDisplay);
        instructionText = findViewById(R.id.instructionText);
        speedText = findViewById(R.id.speedText);
        latencyText = findViewById(R.id.latencyText);
        jitterText = findViewById(R.id.jitterText);
        packetLossText = findViewById(R.id.packetLossText);
        strengthText = findViewById(R.id.strengthText);
        cancelButton = findViewById(R.id.cancelButton);

        // Get the passed room name from intent
        String roomName = getIntent().getStringExtra("ROOM_NAME");

        // Handle null safety (default to Office)
        if (roomName == null || roomName.isEmpty()) {
            roomName = "Office";
        }

        // Dynamically display the correct text
        roomNameDisplay.setText("Testing Wi-Fi in " + roomName);

        instructionText.setText("Please stand in the center of the room while we measure your connection...");

        // Placeholder values
        speedText.setText("Speed: 0 Mbps");
        latencyText.setText("Latency: 0 ms");
        jitterText.setText("Jitter: 0 ms");
        packetLossText.setText("Packet Loss: 0%");
        strengthText.setText("Wi-Fi Strength: 0 dBm");

        cancelButton.setOnClickListener(v -> finish());
    }
}
