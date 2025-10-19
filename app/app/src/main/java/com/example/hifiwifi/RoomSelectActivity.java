package com.example.hifiwifi;

import android.content.Intent;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class RoomSelectActivity extends AppCompatActivity {

    private ImageView spinnerArrow;
    private boolean isSpinnerOpen = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_room_select);

        TextView titleText = findViewById(R.id.titleText);
        Spinner roomSpinner = findViewById(R.id.roomSpinner);
        spinnerArrow = findViewById(R.id.spinnerArrow);
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

        setupSpinnerArrowAnimation(roomSpinner, spinnerArrow);

        // Apply scale animation only to buttons
        applyButtonAnimation(continueButton);
        applyButtonAnimation(cancelButton);

        // Continue → go to ChatActivity
        continueButton.setOnClickListener(v -> {
            String selectedRoom = roomSpinner.getSelectedItem().toString();
            Intent intent = new Intent(RoomSelectActivity.this, ChatActivity.class);
            intent.putExtra("ROOM_NAME", selectedRoom);
            startActivity(intent);
        });

        // Cancel → go back
        cancelButton.setOnClickListener(v -> finish());
    }

    // ✅ Spinner arrow animation linked to dropdown state
    private void setupSpinnerArrowAnimation(Spinner spinner, ImageView arrow) {
        Animation rotateUp = AnimationUtils.loadAnimation(this, R.anim.arrow_rotate_up);
        Animation rotateDown = AnimationUtils.loadAnimation(this, R.anim.arrow_rotate_down);
        
        final boolean[] isFirstSelection = {true}; // Track first automatic selection

        // Rotate arrow up when spinner is touched (dropdown will open)
        spinner.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                if (!isSpinnerOpen) {
                    arrow.startAnimation(rotateUp);
                    isSpinnerOpen = true;
                }
            }
            return false;
        });

        // Listen for item selection or dismissal (dropdown closes)
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // Skip the first automatic selection when spinner initializes
                if (isFirstSelection[0]) {
                    isFirstSelection[0] = false;
                    return;
                }
                
                // User selected an item - dropdown has closed, rotate arrow down
                if (isSpinnerOpen) {
                    arrow.startAnimation(rotateDown);
                    isSpinnerOpen = false;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Dropdown closed without selection
                if (isSpinnerOpen) {
                    arrow.startAnimation(rotateDown);
                    isSpinnerOpen = false;
                }
            }
        });

        // Monitor window focus to detect when dropdown closes (e.g., clicking outside)
        spinner.getViewTreeObserver().addOnWindowFocusChangeListener(hasFocus -> {
            if (!hasFocus && isSpinnerOpen) {
                // Window lost focus, likely because dropdown opened
                // Do nothing here, arrow already rotated up
            } else if (hasFocus && isSpinnerOpen) {
                // Window regained focus, dropdown likely closed
                arrow.post(() -> {
                    if (isSpinnerOpen) {
                        arrow.startAnimation(rotateDown);
                        isSpinnerOpen = false;
                    }
                });
            }
        });
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
