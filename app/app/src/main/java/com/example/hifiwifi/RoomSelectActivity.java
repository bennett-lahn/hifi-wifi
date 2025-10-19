package com.example.hifiwifi;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class RoomSelectActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_room_select);

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

        // When Continue is pressed, send the selected room to ChatActivity
        continueButton.setOnClickListener(v -> {
            String selectedRoom = roomSpinner.getSelectedItem().toString();
            Intent intent = new Intent(RoomSelectActivity.this, ChatActivity.class);
            intent.putExtra("ROOM_NAME", selectedRoom);
            startActivity(intent);
        });

        // Cancel returns to the previous screen
        cancelButton.setOnClickListener(v -> finish());
    }
}
