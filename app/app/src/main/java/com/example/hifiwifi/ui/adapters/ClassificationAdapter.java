package com.example.hifiwifi.ui.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hifiwifi.R;
import com.example.hifiwifi.models.ClassificationResult;

import java.util.ArrayList;
import java.util.List;

/**
 * RecyclerView adapter for classification results
 */
public class ClassificationAdapter extends RecyclerView.Adapter<ClassificationAdapter.ClassificationViewHolder> {
    
    private List<ClassificationResult> classifications;
    
    public ClassificationAdapter(List<ClassificationResult> classifications) {
        this.classifications = classifications != null ? new ArrayList<>(classifications) : new ArrayList<>();
    }
    
    @NonNull
    @Override
    public ClassificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_classification, parent, false);
        return new ClassificationViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ClassificationViewHolder holder, int position) {
        ClassificationResult classification = classifications.get(position);
        holder.bind(classification);
    }
    
    @Override
    public int getItemCount() {
        return classifications.size();
    }
    
    public void updateClassifications(List<ClassificationResult> newClassifications) {
        this.classifications = newClassifications != null ? new ArrayList<>(newClassifications) : new ArrayList<>();
        notifyDataSetChanged();
    }
    
    static class ClassificationViewHolder extends RecyclerView.ViewHolder {
        private TextView textRoomName;
        private TextView textOverallGrade;
        private TextView textZone;
        private TextView textGamingScore;
        private TextView textStreamingScore;
        private TextView textVideoCallScore;
        
        public ClassificationViewHolder(@NonNull View itemView) {
            super(itemView);
            textRoomName = itemView.findViewById(R.id.text_room_name);
            textOverallGrade = itemView.findViewById(R.id.text_overall_grade);
            textZone = itemView.findViewById(R.id.text_zone);
            textGamingScore = itemView.findViewById(R.id.text_gaming_score);
            textStreamingScore = itemView.findViewById(R.id.text_streaming_score);
            textVideoCallScore = itemView.findViewById(R.id.text_video_call_score);
        }
        
        public void bind(ClassificationResult classification) {
            textRoomName.setText(classification.getRoomName());
            textOverallGrade.setText(classification.getOverallGrade());
            textZone.setText("Zone: " + capitalizeFirst(classification.getZone()));
            textGamingScore.setText(String.valueOf(classification.getGamingScore()));
            textStreamingScore.setText(String.valueOf(classification.getStreamingScore()));
            textVideoCallScore.setText(String.valueOf(classification.getVideoCallScore()));
            
            // Set grade color
            setGradeColor(classification.getOverallGrade());
        }
        
        private void setGradeColor(String grade) {
            int color;
            switch (grade) {
                case "A":
                    color = Color.parseColor("#4CAF50"); // Green
                    break;
                case "B":
                    color = Color.parseColor("#8BC34A"); // Light Green
                    break;
                case "C":
                    color = Color.parseColor("#FFC107"); // Amber
                    break;
                case "D":
                    color = Color.parseColor("#FF9800"); // Orange
                    break;
                case "F":
                default:
                    color = Color.parseColor("#F44336"); // Red
                    break;
            }
            textOverallGrade.setTextColor(color);
        }
        
        private String capitalizeFirst(String str) {
            if (str == null || str.isEmpty()) {
                return str;
            }
            return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
        }
    }
}
