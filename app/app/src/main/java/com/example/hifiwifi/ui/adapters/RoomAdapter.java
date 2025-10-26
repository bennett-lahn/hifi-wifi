package com.example.hifiwifi.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hifiwifi.R;
import com.example.hifiwifi.repository.RoomRepository;

import java.util.ArrayList;
import java.util.List;

/**
 * RecyclerView adapter for room list
 */
public class RoomAdapter extends RecyclerView.Adapter<RoomAdapter.RoomViewHolder> {
    
    private List<RoomRepository.Room> rooms;
    private OnRoomClickListener clickListener;
    
    public interface OnRoomClickListener {
        void onRoomClick(RoomRepository.Room room);
        void onDeleteClick(RoomRepository.Room room);
    }
    
    public RoomAdapter(List<RoomRepository.Room> rooms, OnRoomClickListener clickListener) {
        this.rooms = rooms != null ? new ArrayList<>(rooms) : new ArrayList<>();
        this.clickListener = clickListener;
    }
    
    @NonNull
    @Override
    public RoomViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_room, parent, false);
        return new RoomViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull RoomViewHolder holder, int position) {
        RoomRepository.Room room = rooms.get(position);
        holder.bind(room, clickListener);
    }
    
    @Override
    public int getItemCount() {
        return rooms.size();
    }
    
    public void updateRooms(List<RoomRepository.Room> newRooms) {
        this.rooms = newRooms != null ? new ArrayList<>(newRooms) : new ArrayList<>();
        notifyDataSetChanged();
    }
    
    static class RoomViewHolder extends RecyclerView.ViewHolder {
        private TextView textRoomName;
        private TextView textRoomId;
        private Button buttonDelete;
        
        public RoomViewHolder(@NonNull View itemView) {
            super(itemView);
            textRoomName = itemView.findViewById(R.id.text_room_name);
            textRoomId = itemView.findViewById(R.id.text_room_id);
            buttonDelete = itemView.findViewById(R.id.button_delete);
        }
        
        public void bind(RoomRepository.Room room, OnRoomClickListener clickListener) {
            textRoomName.setText(room.getName());
            textRoomId.setText("ID: " + room.getId());
            
            // Set click listeners
            itemView.setOnClickListener(v -> {
                if (clickListener != null) {
                    clickListener.onRoomClick(room);
                }
            });
            
            buttonDelete.setOnClickListener(v -> {
                if (clickListener != null) {
                    clickListener.onDeleteClick(room);
                }
            });
        }
    }
}
