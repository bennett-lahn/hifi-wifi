package com.example.hifiwifi; // use your real package

import android.os.Bundle;
import android.content.Intent;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.ViewFlipper;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private ViewFlipper cubeFlipper;
    private TextView roomNameText;
    private ImageButton arrowLeft, arrowRight;

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

        updateRoom();

        arrowRight.setOnClickListener(v -> showNextRoom());
        arrowLeft.setOnClickListener(v -> showPreviousRoom());

        cubeFlipper.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, WifiTestActivity.class);
            intent.putExtra("ROOM_NAME", roomNameText.getText().toString());
            startActivity(intent);
        });

        ImageButton chatButton = findViewById(R.id.chatButton);

        chatButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, RoomSelectActivity.class);
            startActivity(intent);
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
