package com.example.hifiwifi.viewmodels;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.hifiwifi.repository.ClassificationRepository;

/**
 * ViewModel for managing room selection for chat context
 * Stores the selected room and provides it to ChatViewModel and ChatActivity
 */
public class RoomSelectViewModel extends AndroidViewModel {
    
    private static final String TAG = "RoomSelectViewModel";
    
    // LiveData for selected room
    private MutableLiveData<String> selectedRoom;
    
    // Classification repository for accessing room data
    private ClassificationRepository classificationRepository;
    
    public RoomSelectViewModel(@NonNull Application application) {
        super(application);
        
        // Initialize LiveData
        selectedRoom = new MutableLiveData<>();
        
        // Get classification repository instance
        classificationRepository = ClassificationRepository.getInstance();
        
        Log.d(TAG, "RoomSelectViewModel initialized");
    }
    
    /**
     * Set the selected room
     * This is called from RoomSelectActivity when user confirms selection
     */
    public void selectRoom(String roomName) {
        if (roomName == null || roomName.isEmpty()) {
            Log.w(TAG, "Attempted to select empty room name");
            return;
        }
        
        Log.d(TAG, "Room selected: " + roomName);
        selectedRoom.setValue(roomName);
    }
    
    /**
     * Get the selected room as LiveData
     */
    public LiveData<String> getSelectedRoom() {
        return selectedRoom;
    }
    
    /**
     * Get the current selected room value (synchronous)
     */
    public String getCurrentSelectedRoom() {
        return selectedRoom.getValue();
    }
    
    /**
     * Check if a room has been selected
     */
    public boolean hasRoomSelected() {
        return selectedRoom.getValue() != null && !selectedRoom.getValue().isEmpty();
    }
    
    /**
     * Clear the selected room
     */
    public void clearSelection() {
        Log.d(TAG, "Clearing room selection");
        selectedRoom.setValue(null);
    }
    
    /**
     * Check if the selected room has classification data available
     */
    public boolean hasClassificationDataForSelectedRoom() {
        String room = selectedRoom.getValue();
        if (room == null || room.isEmpty()) {
            return false;
        }
        
        return !classificationRepository.getClassificationsForRoom(room).isEmpty();
    }
    
    /**
     * Get classification repository instance
     */
    public ClassificationRepository getClassificationRepository() {
        return classificationRepository;
    }
    
    /**
     * Get statistics about available room data
     */
    public String getRoomDataSummary() {
        String room = selectedRoom.getValue();
        if (room == null || room.isEmpty()) {
            return "No room selected";
        }
        
        int count = classificationRepository.getClassificationsForRoom(room).size();
        return String.format("%s: %d measurement(s) available", room, count);
    }
}

