package com.example.hifiwifi.classifier;

import java.util.HashMap;
import java.util.Map;

/**
 * Factory class to create ActivityImportance objects for different activity types
 */
public class ActivityImportanceFactory {
    
    private static final Map<String, ActivityImportance> ACTIVITY_IMPORTANCE_MAP = new HashMap<>();
    
    static {
        // Gaming: Low latency and jitter are critical, bandwidth is important, signal strength matters
        ACTIVITY_IMPORTANCE_MAP.put("gaming", new ActivityImportance(
            "gaming",
            0.8,  // signal strength - important for stability
            1.0,  // latency - critical for gaming
            0.7,  // bandwidth - important for game data
            1.0,  // jitter - critical for smooth gameplay
            0.9   // packet loss - very important for gaming
        ));
        
        // Video calls: Low latency and jitter are critical, bandwidth is important
        ACTIVITY_IMPORTANCE_MAP.put("video_call", new ActivityImportance(
            "video_call",
            0.8,  // signal strength - important for stability
            1.0,  // latency - critical for real-time communication
            0.9,  // bandwidth - important for video quality
            1.0,  // jitter - critical for smooth video
            0.9   // packet loss - very important for video calls
        ));
        
        // Streaming: High bandwidth is critical, latency less important
        ACTIVITY_IMPORTANCE_MAP.put("streaming", new ActivityImportance(
            "streaming",
            0.7,  // signal strength - important for stability
            0.4,  // latency - less critical for streaming
            1.0,  // bandwidth - critical for video quality
            0.6,  // jitter - moderately important
            0.7   // packet loss - important for streaming
        ));
        
        // General web browsing: Balanced importance
        ACTIVITY_IMPORTANCE_MAP.put("general", new ActivityImportance(
            "general",
            0.6,  // signal strength - moderately important
            0.6,  // latency - moderately important
            0.8,  // bandwidth - important for web browsing
            0.5,  // jitter - less important
            0.6   // packet loss - moderately important
        ));
        
        // Work from home: Balanced but slightly more emphasis on stability
        ACTIVITY_IMPORTANCE_MAP.put("work", new ActivityImportance(
            "work",
            0.8,  // signal strength - important for stability
            0.7,  // latency - important for video calls
            0.8,  // bandwidth - important for file transfers
            0.7,  // jitter - important for video calls
            0.8   // packet loss - important for work
        ));
        
        // IoT devices: Signal strength and stability are most important
        ACTIVITY_IMPORTANCE_MAP.put("iot", new ActivityImportance(
            "iot",
            1.0,  // signal strength - critical for IoT devices
            0.3,  // latency - less important for IoT
            0.4,  // bandwidth - less important for IoT
            0.4,  // jitter - less important for IoT
            0.7   // packet loss - important for IoT reliability
        ));
    }
    
    /**
     * Get ActivityImportance for a specific activity type
     */
    public static ActivityImportance getActivityImportance(String activityType) {
        return ACTIVITY_IMPORTANCE_MAP.getOrDefault(activityType.toLowerCase(), 
            ACTIVITY_IMPORTANCE_MAP.get("general"));
    }
    
    /**
     * Get all available activity types
     */
    public static String[] getAvailableActivityTypes() {
        return ACTIVITY_IMPORTANCE_MAP.keySet().toArray(new String[0]);
    }
    
    /**
     * Check if an activity type is supported
     */
    public static boolean isActivityTypeSupported(String activityType) {
        return ACTIVITY_IMPORTANCE_MAP.containsKey(activityType.toLowerCase());
    }
}
