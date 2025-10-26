package com.example.hifiwifi.repository;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Repository for managing room data
 * Uses SharedPreferences for persistence
 */
public class RoomRepository {
    
    private static final String PREFS_NAME = "room_preferences";
    private static final String ROOMS_KEY = "rooms";
    
    private Context context;
    private SharedPreferences sharedPreferences;
    private Gson gson;
    private List<Room> rooms;
    
    /**
     * Simple room data structure
     */
    public static class Room {
        private String id;
        private String name;
        
        public Room() {
            // Default constructor for Gson
        }
        
        public Room(String id, String name) {
            this.id = id;
            this.name = name;
        }
        
        public String getId() {
            return id;
        }
        
        public void setId(String id) {
            this.id = id;
        }
        
        public String getName() {
            return name;
        }
        
        public void setName(String name) {
            this.name = name;
        }
    }
    
    public RoomRepository(Context context) {
        this.context = context;
        this.sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.gson = new Gson();
        this.rooms = loadRooms();
    }
    
    /**
     * Save a room
     */
    public void saveRoom(Room room) {
        // Generate ID if not set
        if (room.getId() == null || room.getId().isEmpty()) {
            room.setId(UUID.randomUUID().toString());
        }
        
        // Update existing room or add new one
        boolean found = false;
        for (int i = 0; i < rooms.size(); i++) {
            if (rooms.get(i).getId().equals(room.getId())) {
                rooms.set(i, room);
                found = true;
                break;
            }
        }
        
        if (!found) {
            rooms.add(room);
        }
        
        saveRooms();
    }
    
    /**
     * Save a room by name (generates ID automatically)
     */
    public String saveRoom(String roomName) {
        Room room = new Room(UUID.randomUUID().toString(), roomName);
        saveRoom(room);
        return room.getId();
    }
    
    /**
     * Get all rooms
     */
    public List<Room> getRooms() {
        return new ArrayList<>(rooms);
    }
    
    /**
     * Get room by ID
     */
    public Room getRoomById(String roomId) {
        for (Room room : rooms) {
            if (room.getId().equals(roomId)) {
                return room;
            }
        }
        return null;
    }
    
    /**
     * Get room by name
     */
    public Room getRoomByName(String roomName) {
        for (Room room : rooms) {
            if (room.getName().equals(roomName)) {
                return room;
            }
        }
        return null;
    }
    
    /**
     * Delete a room
     */
    public boolean deleteRoom(String roomId) {
        boolean removed = rooms.removeIf(room -> room.getId().equals(roomId));
        if (removed) {
            saveRooms();
        }
        return removed;
    }
    
    /**
     * Delete a room by name
     */
    public boolean deleteRoomByName(String roomName) {
        boolean removed = rooms.removeIf(room -> room.getName().equals(roomName));
        if (removed) {
            saveRooms();
        }
        return removed;
    }
    
    /**
     * Check if room exists
     */
    public boolean roomExists(String roomId) {
        return getRoomById(roomId) != null;
    }
    
    /**
     * Check if room name exists
     */
    public boolean roomNameExists(String roomName) {
        return getRoomByName(roomName) != null;
    }
    
    /**
     * Get room count
     */
    public int getRoomCount() {
        return rooms.size();
    }
    
    /**
     * Clear all rooms
     */
    public void clearAllRooms() {
        rooms.clear();
        saveRooms();
    }
    
    /**
     * Load rooms from SharedPreferences
     */
    private List<Room> loadRooms() {
        String roomsJson = sharedPreferences.getString(ROOMS_KEY, "[]");
        Type listType = new TypeToken<List<Room>>(){}.getType();
        List<Room> loadedRooms = gson.fromJson(roomsJson, listType);
        return loadedRooms != null ? loadedRooms : new ArrayList<>();
    }
    
    /**
     * Save rooms to SharedPreferences
     */
    private void saveRooms() {
        String roomsJson = gson.toJson(rooms);
        sharedPreferences.edit().putString(ROOMS_KEY, roomsJson).apply();
    }
}
