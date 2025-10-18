package com.example.hifiwifi;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.hifiwifi.ui.fragments.BLEConnectionFragment;
import com.example.hifiwifi.ui.fragments.ChatFragment;
import com.example.hifiwifi.ui.fragments.MeasurementFragment;
import com.example.hifiwifi.ui.fragments.ResultsFragment;
import com.example.hifiwifi.ui.fragments.RoomManagementFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

/**
 * MainActivity - UI Skeleton Only
 * No services, no ViewModels, no data connections
 * Just basic UI for development and testing
 */
public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;
    private FragmentManager fragmentManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.nav_host_fragment), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        setupUI();
    }

    private void setupUI() {
        fragmentManager = getSupportFragmentManager();
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        
        // Setup bottom navigation click listener
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            
            if (itemId == R.id.roomManagementFragment) {
                showFragment(new RoomManagementFragment(), "RoomManagement");
                return true;
            } else if (itemId == R.id.measurementFragment) {
                showFragment(new MeasurementFragment(), "Measurement");
                return true;
            } else if (itemId == R.id.resultsFragment) {
                showFragment(new ResultsFragment(), "Results");
                return true;
            } else if (itemId == R.id.bleConnectionFragment) {
                showFragment(new BLEConnectionFragment(), "BLEConnection");
                return true;
            } else if (itemId == R.id.chatFragment) {
                showFragment(new ChatFragment(), "Chat");
                return true;
            }
            
            return false;
        });
        
        // Show default fragment
        showFragment(new RoomManagementFragment(), "RoomManagement");
    }

    private void showFragment(Fragment fragment, String tag) {
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.nav_host_fragment, fragment, tag);
        transaction.commit();
    }
}