package com.example.hifiwifi;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class WifiTestActivity extends AppCompatActivity {

    private TextView roomNameText;
    private Button startTestButton, cancelButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wifi_test);

        roomNameText = findViewById(R.id.roomNameText);
        startTestButton = findViewById(R.id.startTestButton);
        cancelButton = findViewById(R.id.cancelButton);

        // Get room name from Intent
        String roomName = getIntent().getStringExtra("ROOM_NAME");
        if (roomName != null) {
            roomNameText.setText("Start Wi-Fi Test for " + roomName + "?");
        }

//        startTestButton.setOnClickListener(v -> {
//            // TODO: add your real Wi-Fi test logic here
//            roomNameText.setText("Testing Wi-Fi...");
//        });

        cancelButton.setOnClickListener(v -> finish());
    }
}
