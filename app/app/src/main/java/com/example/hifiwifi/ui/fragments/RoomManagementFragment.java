package com.example.hifiwifi.ui.fragments;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hifiwifi.R;
import com.example.hifiwifi.repository.RoomRepository;
import com.example.hifiwifi.ui.adapters.RoomAdapter;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;

/**
 * Fragment for managing rooms (add, edit, delete)
 */
public class RoomManagementFragment extends Fragment {
    
    private RecyclerView recyclerViewRooms;
    private FloatingActionButton fabAddRoom;
    private TextView textEmptyState;
    
    private RoomRepository roomRepository;
    private RoomAdapter roomAdapter;
    private List<RoomRepository.Room> rooms;
    
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
        
        // Initialize repository
        roomRepository = new RoomRepository(requireContext());
        
        // Setup RecyclerView
        setupRecyclerView();
        
        // Setup FAB click listener
        fabAddRoom.setOnClickListener(v -> showAddRoomDialog());
        
        // Load rooms
        loadRooms();
    }
    
    private void setupRecyclerView() {
        roomAdapter = new RoomAdapter(rooms, new RoomAdapter.OnRoomClickListener() {
            @Override
            public void onRoomClick(RoomRepository.Room room) {
                // TODO: Navigate to room details or edit room
                Toast.makeText(requireContext(), "Room clicked: " + room.getName(), Toast.LENGTH_SHORT).show();
            }
            
            @Override
            public void onDeleteClick(RoomRepository.Room room) {
                showDeleteRoomDialog(room);
            }
        });
        
        recyclerViewRooms.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerViewRooms.setAdapter(roomAdapter);
    }
    
    private void loadRooms() {
        rooms = roomRepository.getRooms();
        roomAdapter.updateRooms(rooms);
        
        // Show/hide empty state
        if (rooms.isEmpty()) {
            textEmptyState.setVisibility(View.VISIBLE);
            recyclerViewRooms.setVisibility(View.GONE);
        } else {
            textEmptyState.setVisibility(View.GONE);
            recyclerViewRooms.setVisibility(View.VISIBLE);
        }
    }
    
    private void showAddRoomDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Add New Room");
        
        final EditText input = new EditText(requireContext());
        input.setHint("Enter room name");
        builder.setView(input);
        
        builder.setPositiveButton("Add", (dialog, which) -> {
            String roomName = input.getText().toString().trim();
            if (!roomName.isEmpty()) {
                if (roomRepository.roomNameExists(roomName)) {
                    Toast.makeText(requireContext(), "Room name already exists", Toast.LENGTH_SHORT).show();
                } else {
                    roomRepository.saveRoom(roomName);
                    loadRooms();
                    Toast.makeText(requireContext(), "Room added successfully", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(requireContext(), "Please enter a room name", Toast.LENGTH_SHORT).show();
            }
        });
        
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }
    
    private void showDeleteRoomDialog(RoomRepository.Room room) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Delete Room");
        builder.setMessage("Are you sure you want to delete '" + room.getName() + "'?");
        
        builder.setPositiveButton("Delete", (dialog, which) -> {
            if (roomRepository.deleteRoom(room.getId())) {
                loadRooms();
                Toast.makeText(requireContext(), "Room deleted successfully", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(requireContext(), "Failed to delete room", Toast.LENGTH_SHORT).show();
            }
        });
        
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }
}
