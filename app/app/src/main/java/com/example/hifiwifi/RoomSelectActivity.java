package com.example.hifiwifi;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.hifiwifi.viewmodels.RoomSelectViewModel;

public class RoomSelectActivity extends AppCompatActivity {

    private static final String TAG = "RoomSelectActivity";
    private RoomSelectViewModel roomSelectViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_room_select);

        // Initialize ViewModel
        roomSelectViewModel = new ViewModelProvider(this).get(RoomSelectViewModel.class);

        TextView titleText = findViewById(R.id.titleText);
        Spinner roomSpinner = findViewById(R.id.roomSpinner);
        Button continueButton = findViewById(R.id.continueButton);
        Button cancelButton = findViewById(R.id.cancelButton);

        titleText.setText("Select a Room to View Data From");

        // Fake list of rooms
        String[] rooms = {"Office", "Living Room", "Kitchen", "Bedroom"};

        // Use custom spinner layout to match app theme
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                R.layout.spinner_item, // custom layout for spinner items
                rooms
        );
        adapter.setDropDownViewResource(R.layout.spinner_item);
        roomSpinner.setAdapter(adapter);

        // Apply scale animation only to buttons
        applyButtonAnimation(continueButton);
        applyButtonAnimation(cancelButton);

        // Continue → save selected room to ViewModel and go to ChatActivity
        continueButton.setOnClickListener(v -> {
            String selectedRoom = roomSpinner.getSelectedItem().toString();
            Log.d(TAG, "User selected room: " + selectedRoom);
            
            // Save selection to ViewModel
            roomSelectViewModel.selectRoom(selectedRoom);
            
            // Log available data for selected room
            String dataSummary = roomSelectViewModel.getRoomDataSummary();
            Log.d(TAG, "Room data summary: " + dataSummary);
            
            // Navigate to ChatActivity
            Intent intent = new Intent(RoomSelectActivity.this, ChatActivity.class);
            intent.putExtra("ROOM_NAME", selectedRoom);
            startActivity(intent);
        });

        // Cancel → go back
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
            return false; // Allow normal click handling
        });
    }
}
