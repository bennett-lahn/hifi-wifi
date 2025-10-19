package com.example.hifiwifi;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class WifiTestActivity extends AppCompatActivity {

    private TextView roomNameText;
    private Button startButton, cancelButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wifi_test);

        roomNameText = findViewById(R.id.roomNameText);
        startButton = findViewById(R.id.startTestButton);
        cancelButton = findViewById(R.id.cancelButton);

        String roomName = getIntent().getStringExtra("ROOM_NAME");
        roomNameText.setText("Start Wi-Fi Test for " + roomName + "?");

        startButton.setOnClickListener(v -> {
            Intent intent = new Intent(WifiTestActivity.this, WifiTestRunningActivity.class);
            intent.putExtra("ROOM_NAME", roomName);
            startActivity(intent);
        });

        cancelButton.setOnClickListener(v -> finish());
    }
}
