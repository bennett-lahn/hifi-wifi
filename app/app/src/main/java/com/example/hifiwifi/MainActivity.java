package com.example.hifiwifi;

import android.os.Bundle;
import android.content.Intent;
import android.view.MotionEvent;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ViewFlipper;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private ViewFlipper cubeFlipper;
    private TextView roomNameText;
    private ImageButton arrowLeft, arrowRight, chatButton;
    private String[] roomNames = {"Living Room", "Office"};
    private int currentIndex = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        cubeFlipper = findViewById(R.id.cubeFlipper);
        roomNameText = findViewById(R.id.roomNameText);
        arrowLeft = findViewById(R.id.arrowLeft);
        arrowRight = findViewById(R.id.arrowRight);
        chatButton = findViewById(R.id.chatButton);

        updateRoom();

        // Apply scaling animation to buttons
        applyButtonAnimation(arrowLeft);
        applyButtonAnimation(arrowRight);
        applyButtonAnimation(chatButton);
        applyButtonAnimation(cubeFlipper);

        arrowRight.setOnClickListener(v -> showNextRoom());
        arrowLeft.setOnClickListener(v -> showPreviousRoom());

        cubeFlipper.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, WifiTestActivity.class);
            intent.putExtra("ROOM_NAME", roomNameText.getText().toString());
            startActivity(intent);
        });

        chatButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, RoomSelectActivity.class);
            startActivity(intent);
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
            return false; // allow click events to still trigger
        });
    }

    private void showNextRoom() {
        cubeFlipper.setInAnimation(this, R.anim.slide_in_right);
        cubeFlipper.setOutAnimation(this, R.anim.slide_out_left);
        cubeFlipper.showNext();
        currentIndex = (currentIndex + 1) % roomNames.length;
        updateRoom();
    }

    private void showPreviousRoom() {
        cubeFlipper.setInAnimation(this, R.anim.slide_in_left);
        cubeFlipper.setOutAnimation(this, R.anim.slide_out_right);
        cubeFlipper.showPrevious();
        currentIndex = (currentIndex - 1 + roomNames.length) % roomNames.length;
        updateRoom();
    }

    private void updateRoom() {
        roomNameText.setText(roomNames[currentIndex]);
    }
}
