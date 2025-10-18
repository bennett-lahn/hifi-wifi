package com.example.hifiwifi.ui.fragments;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hifiwifi.R;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

/**
 * Fragment for managing rooms (add, edit, delete) - UI Skeleton Only
 * No services, no ViewModels, no data connections
 */
public class RoomManagementFragment extends Fragment {
    
    private RecyclerView recyclerViewRooms;
    private FloatingActionButton fabAddRoom;
    private TextView textEmptyState;
    
    // Mock data for UI development
    private List<String> mockRooms;
    
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_room_management, container, false);
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Initialize views
        recyclerViewRooms = view.findViewById(R.id.recycler_view_rooms);
        fabAddRoom = view.findViewById(R.id.fab_add_room);
        textEmptyState = view.findViewById(R.id.text_empty_state);
        
        // Initialize mock data
        mockRooms = new ArrayList<>();
        mockRooms.add("Living Room");
        mockRooms.add("Bedroom");
        mockRooms.add("Kitchen");
        
        // Setup RecyclerView with simple adapter
        setupRecyclerView();
        
        // Setup FAB click listener
        fabAddRoom.setOnClickListener(v -> showAddRoomDialog());
        
        // Update UI
        updateEmptyState();
    }
    
    private void setupRecyclerView() {
        // Simple adapter for UI development
        recyclerViewRooms.setLayoutManager(new LinearLayoutManager(requireContext()));
        // For now, just show empty - you can add a simple adapter later
        updateEmptyState();
    }
    
    private void showAddRoomDialog() {
        EditText editText = new EditText(requireContext());
        editText.setHint("Enter room name");
        
        new AlertDialog.Builder(requireContext())
                .setTitle("Add New Room")
                .setView(editText)
                .setPositiveButton("Add", (dialog, which) -> {
                    String roomName = editText.getText().toString().trim();
                    if (!roomName.isEmpty()) {
                        mockRooms.add(roomName);
                        updateEmptyState();
                        Toast.makeText(requireContext(), "Room added (UI only)", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(requireContext(), "Please enter a room name", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
    
    private void updateEmptyState() {
        if (mockRooms.isEmpty()) {
            textEmptyState.setVisibility(View.VISIBLE);
            recyclerViewRooms.setVisibility(View.GONE);
        } else {
            textEmptyState.setVisibility(View.GONE);
            recyclerViewRooms.setVisibility(View.VISIBLE);
            // Show mock data count
            textEmptyState.setText("Mock rooms: " + mockRooms.size() + " (UI only)");
        }
    }
}