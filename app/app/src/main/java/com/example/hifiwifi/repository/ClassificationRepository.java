package com.example.hifiwifi.repository;

import android.util.Log;

import com.example.hifiwifi.classifier.ClassificationResult;
import com.example.hifiwifi.classifier.WiFiClassification;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Repository for storing and managing WiFi classification data in JSON format
 * Data is stored in-memory and persists during the app runtime
 * Purpose: Store classification results for each room to send to SLM chat as context
 */
public class ClassificationRepository {
    
    private static final String TAG = "ClassificationRepository";
    
    // Singleton instance
    private static ClassificationRepository instance;
    
    // In-memory storage for classification results
    private List<ClassificationResult> classificationResults;
    
    // Cached JSON representation
    private JSONObject cachedJson;
    private boolean jsonDirty = true;
    
    /**
     * Private constructor for singleton pattern
     */
    private ClassificationRepository() {
        this.classificationResults = new ArrayList<>();
    }
    
    /**
     * Get singleton instance
     */
    public static synchronized ClassificationRepository getInstance() {
        if (instance == null) {
            instance = new ClassificationRepository();
        }
        return instance;
    }
    
    /**
     * Add a classification result for a room
     * Replaces existing classification for the same room
     */
    public synchronized void addClassification(ClassificationResult result) {
        if (result == null) {
            Log.w(TAG, "Attempted to add null classification result");
            return;
        }
        
        Log.d(TAG, "Adding classification for room: " + result.getRoomName() + 
                   " (Activity: " + result.getActivityType() + ")");
        
        // Remove existing classification for this room and activity type
        classificationResults.removeIf(r -> 
            r.getRoomName().equals(result.getRoomName()) && 
            r.getActivityType().equals(result.getActivityType())
        );
        
        // Add new classification
        classificationResults.add(result);
        jsonDirty = true;
        
        Log.d(TAG, "Total classifications stored: " + classificationResults.size());
    }
    
    /**
     * Get all classification results
     */
    public synchronized List<ClassificationResult> getAllClassifications() {
        return new ArrayList<>(classificationResults);
    }
    
    /**
     * Get classification for a specific room and activity
     */
    public synchronized ClassificationResult getClassification(String roomName, String activityType) {
        for (ClassificationResult result : classificationResults) {
            if (result.getRoomName().equals(roomName) && 
                result.getActivityType().equals(activityType)) {
                return result;
            }
        }
        return null;
    }
    
    /**
     * Get all classifications for a specific room (all activities)
     */
    public synchronized List<ClassificationResult> getClassificationsForRoom(String roomName) {
        List<ClassificationResult> roomResults = new ArrayList<>();
        for (ClassificationResult result : classificationResults) {
            if (result.getRoomName().equals(roomName)) {
                roomResults.add(result);
            }
        }
        return roomResults;
    }
    
    /**
     * Clear all classification data
     */
    public synchronized void clearAll() {
        Log.d(TAG, "Clearing all classification data");
        classificationResults.clear();
        jsonDirty = true;
        cachedJson = null;
    }
    
    /**
     * Get the number of stored classifications
     */
    public synchronized int getCount() {
        return classificationResults.size();
    }
    
    /**
     * Convert classification data to JSON format for SLM chat context
     * Returns cached JSON if data hasn't changed
     */
    public synchronized String toJson() {
        if (!jsonDirty && cachedJson != null) {
            return cachedJson.toString();
        }
        
        try {
            cachedJson = buildJson();
            jsonDirty = false;
            return cachedJson.toString();
        } catch (JSONException e) {
            Log.e(TAG, "Error building JSON: " + e.getMessage(), e);
            return "{}";
        }
    }
    
    /**
     * Get pretty-printed JSON string
     */
    public synchronized String toJsonPretty() {
        try {
            if (jsonDirty || cachedJson == null) {
                cachedJson = buildJson();
                jsonDirty = false;
            }
            return cachedJson.toString(2); // Indent with 2 spaces
        } catch (JSONException e) {
            Log.e(TAG, "Error building pretty JSON: " + e.getMessage(), e);
            return "{}";
        }
    }
    
    /**
     * Build the complete JSON structure
     */
    private JSONObject buildJson() throws JSONException {
        JSONObject root = new JSONObject();
        
        // Build measurements array
        JSONArray measurements = new JSONArray();
        for (ClassificationResult result : classificationResults) {
            measurements.put(buildMeasurementJson(result));
        }
        root.put("measurements", measurements);
        
        // Build summary
        root.put("summary", buildSummaryJson());
        
        return root;
    }
    
    /**
     * Build JSON for a single measurement/classification
     */
    private JSONObject buildMeasurementJson(ClassificationResult result) throws JSONException {
        JSONObject measurement = new JSONObject();
        
        // Basic info
        measurement.put("roomName", result.getRoomName());
        measurement.put("activityType", result.getActivityType());
        
        // Classification object
        JSONObject classification = new JSONObject();
        
        // Overall classification
        classification.put("overallClassification", 
            result.getOverallClassification().name());
        
        // Metric classifications
        JSONObject metricClassifications = new JSONObject();
        if (result.getMetricClassification() != null) {
            metricClassifications.put("signalStrength", 
                result.getMetricClassification().getSignalStrengthClassification().name());
            metricClassifications.put("latency", 
                result.getMetricClassification().getLatencyClassification().name());
            metricClassifications.put("bandwidth", 
                result.getMetricClassification().getBandwidthClassification().name());
            metricClassifications.put("jitter", 
                result.getMetricClassification().getJitterClassification().name());
            metricClassifications.put("packetLoss", 
                result.getMetricClassification().getPacketLossClassification().name());
        }
        classification.put("metricClassifications", metricClassifications);
        
        // Activity importance
        JSONObject activityImportance = new JSONObject();
        if (result.getActivityImportance() != null) {
            activityImportance.put("activityType", result.getActivityType());
            activityImportance.put("mostImportantMetric", 
                result.getActivityImportance().getMostImportantMetric());
        }
        classification.put("activityImportance", activityImportance);
        
        // Reasoning and metrics
        classification.put("reasoning", result.getReasoning() != null ? result.getReasoning() : "");
        classification.put("mostCriticalMetric", 
            result.getMostCriticalMetric() != null ? result.getMostCriticalMetric() : "");
        
        // Well performing metrics
        JSONArray wellPerforming = new JSONArray();
        String[] wellPerformingMetrics = result.getWellPerformingMetrics();
        if (wellPerformingMetrics != null) {
            for (String metric : wellPerformingMetrics) {
                wellPerforming.put(metric);
            }
        }
        classification.put("wellPerformingMetrics", wellPerforming);
        
        // Poorly performing metrics
        JSONArray poorlyPerforming = new JSONArray();
        String[] poorlyPerformingMetrics = result.getPoorlyPerformingMetrics();
        if (poorlyPerformingMetrics != null) {
            for (String metric : poorlyPerformingMetrics) {
                poorlyPerforming.put(metric);
            }
        }
        classification.put("poorlyPerformingMetrics", poorlyPerforming);
        
        // Acceptability and recommendations
        classification.put("isAcceptableForActivity", result.isAcceptableForActivity());
        
        JSONArray recommendations = new JSONArray();
        if (result.getRecommendations() != null) {
            for (String recommendation : result.getRecommendations()) {
                recommendations.put(recommendation);
            }
        }
        classification.put("recommendations", recommendations);
        
        measurement.put("classification", classification);
        
        return measurement;
    }
    
    /**
     * Build summary JSON object
     */
    private JSONObject buildSummaryJson() throws JSONException {
        JSONObject summary = new JSONObject();
        
        // Total measurements
        summary.put("totalMeasurements", classificationResults.size());
        
        // Rooms covered
        Set<String> rooms = new HashSet<>();
        for (ClassificationResult result : classificationResults) {
            rooms.add(result.getRoomName());
        }
        JSONArray roomsArray = new JSONArray();
        for (String room : rooms) {
            roomsArray.put(room);
        }
        summary.put("roomsCovered", roomsArray);
        
        // Activities tested
        Set<String> activities = new HashSet<>();
        for (ClassificationResult result : classificationResults) {
            activities.add(result.getActivityType());
        }
        JSONArray activitiesArray = new JSONArray();
        for (String activity : activities) {
            activitiesArray.put(activity);
        }
        summary.put("activitiesTested", activitiesArray);
        
        // Overall network health (average of all classifications)
        String overallHealth = calculateOverallNetworkHealth();
        summary.put("overallNetworkHealth", overallHealth);
        
        return summary;
    }
    
    /**
     * Calculate overall network health based on all classifications
     */
    private String calculateOverallNetworkHealth() {
        if (classificationResults.isEmpty()) {
            return "UNKNOWN";
        }
        
        // Calculate average score
        int totalScore = 0;
        for (ClassificationResult result : classificationResults) {
            totalScore += result.getOverallClassification().getScore();
        }
        double averageScore = (double) totalScore / classificationResults.size();
        
        // Convert to classification
        if (averageScore >= 4.5) {
            return "EXCELLENT";
        } else if (averageScore >= 3.5) {
            return "GOOD";
        } else if (averageScore >= 2.5) {
            return "OKAY";
        } else if (averageScore >= 1.5) {
            return "BAD";
        } else {
            return "MARGINAL";
        }
    }
    
    /**
     * Get statistics about stored classifications
     */
    public synchronized Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("totalClassifications", classificationResults.size());
        
        // Count by room
        Map<String, Integer> roomCounts = new HashMap<>();
        for (ClassificationResult result : classificationResults) {
            String roomName = result.getRoomName();
            roomCounts.put(roomName, roomCounts.getOrDefault(roomName, 0) + 1);
        }
        stats.put("classificationsByRoom", roomCounts);
        
        // Count by activity
        Map<String, Integer> activityCounts = new HashMap<>();
        for (ClassificationResult result : classificationResults) {
            String activityType = result.getActivityType();
            activityCounts.put(activityType, activityCounts.getOrDefault(activityType, 0) + 1);
        }
        stats.put("classificationsByActivity", activityCounts);
        
        // Overall health
        stats.put("overallNetworkHealth", calculateOverallNetworkHealth());
        
        return stats;
    }
    
    /**
     * Log current state of repository
     */
    public synchronized void logState() {
        Log.i(TAG, "=== Classification Repository State ===");
        Log.i(TAG, "Total classifications: " + classificationResults.size());
        
        Map<String, Object> stats = getStatistics();
        Log.i(TAG, "Overall network health: " + stats.get("overallNetworkHealth"));
        
        @SuppressWarnings("unchecked")
        Map<String, Integer> roomCounts = (Map<String, Integer>) stats.get("classificationsByRoom");
        if (roomCounts != null && !roomCounts.isEmpty()) {
            Log.i(TAG, "Classifications by room:");
            for (Map.Entry<String, Integer> entry : roomCounts.entrySet()) {
                Log.i(TAG, "  - " + entry.getKey() + ": " + entry.getValue());
            }
        }
        
        @SuppressWarnings("unchecked")
        Map<String, Integer> activityCounts = (Map<String, Integer>) stats.get("classificationsByActivity");
        if (activityCounts != null && !activityCounts.isEmpty()) {
            Log.i(TAG, "Classifications by activity:");
            for (Map.Entry<String, Integer> entry : activityCounts.entrySet()) {
                Log.i(TAG, "  - " + entry.getKey() + ": " + entry.getValue());
            }
        }
        
        Log.i(TAG, "========================================");
    }
}

