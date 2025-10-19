package com.example.hifiwifi;

import android.content.Intent;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class WifiTestActivity extends AppCompatActivity {

    private TextView roomNameText;
    private Button startButton, cancelButton;
    private Spinner activitySpinner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wifi_test);

        roomNameText = findViewById(R.id.roomNameText);
        startButton = findViewById(R.id.startTestButton);
        cancelButton = findViewById(R.id.cancelButton);
        activitySpinner = findViewById(R.id.activitySpinner);

        // ✅ Get the room name passed from MainActivity
        String roomName = getIntent().getStringExtra("ROOM_NAME");

        // ✅ Set the text dynamically with the room name
        roomNameText.setText("Select an activity before testing Wi-Fi in " + roomName + ":");

        // Populate dropdown (spinner)
        String[] activities = {"Gaming", "Video Call", "Streaming", "General", "Work"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, activities);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        activitySpinner.setAdapter(adapter);

        // Button grow animation
        applyButtonAnimation(startButton);
        applyButtonAnimation(cancelButton);

        startButton.setOnClickListener(v -> {
            String selectedActivity = activitySpinner.getSelectedItem().toString();

            // Pass both room name and activity to next screen
            Intent intent = new Intent(WifiTestActivity.this, WifiTestRunningActivity.class);
            intent.putExtra("ROOM_NAME", roomName);
            intent.putExtra("ACTIVITY_TYPE", selectedActivity.toLowerCase());
            startActivity(intent);
        });

        cancelButton.setOnClickListener(v -> finish());
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
            return false; // allows normal click to trigger
        });
    }
}
