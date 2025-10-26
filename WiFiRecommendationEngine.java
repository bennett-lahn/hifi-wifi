/**
 * WiFiRecommendationEngine - Algorithmic decision-making for WiFi optimization
 * 
 * This class contains the pure algorithmic logic for making WiFi recommendations.
 * NO AI/LLM is used here - just deterministic, testable algorithms.
 * 
 * The LLM is only used AFTER this to explain the decision in friendly language.
 * 
 * Flow: Measure → Classify (WiFiClassifier) → Decide (this class) → Explain (LLM)
 * 
 * @author Hi-Fi WiFi Optimization Team
 * @version 2.0 - Explainer Mode
 */

package com.example.hifiwifi;

public class WiFiRecommendationEngine {
    
    /**
     * Make a recommendation based on classified WiFi measurements.
     * This is pure algorithmic decision-making, no AI needed.
     * 
     * @param signalStrength "excellent", "good", "fair", "poor", "very_poor"
     * @param latency "excellent", "good", "fair", "poor"
     * @param bandwidth "excellent", "good", "fair", "poor"
     * @param frequency "5GHz" or "2.4GHz"
     * @param activity "gaming", "video_call", "streaming", "browsing"
     * @return Recommendation object with action and reasoning
     */
    public static Recommendation makeRecommendation(
        String signalStrength,
        String latency,
        String bandwidth,
        String frequency,
        String activity
    ) {
        Recommendation rec = new Recommendation();
        
        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        // RULE 1: Everything excellent = stay put
        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        if ("excellent".equals(signalStrength) && 
            "excellent".equals(latency) &&
            "excellent".equals(bandwidth)) {
            rec.action = "stay_current";
            rec.reasonCode = "optimal_all_metrics";
            rec.targetLocation = null;
            return rec;
        }
        
        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        // RULE 2: Poor/very poor signal = move closer
        // (Don't suggest band switch if signal is poor - won't help)
        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        if ("poor".equals(signalStrength) || "very_poor".equals(signalStrength)) {
            rec.action = "move_location";
            rec.reasonCode = "weak_signal";
            rec.targetLocation = "closer to router";
            return rec;
        }
        
        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        // RULE 3: Good/excellent signal on 2.4GHz + high-bandwidth activity
        // = suggest switching to 5GHz for better speeds
        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        if (("good".equals(signalStrength) || "excellent".equals(signalStrength)) &&
            "2.4GHz".equals(frequency) &&
            isHighBandwidthActivity(activity)) {
            rec.action = "switch_band";
            rec.reasonCode = "optimization_opportunity";
            rec.targetLocation = null;
            return rec;
        }
        
        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        // RULE 4: Activity requirements not met = move closer
        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        if (!isSufficientForActivity(signalStrength, latency, bandwidth, activity)) {
            rec.action = "move_location";
            rec.reasonCode = "insufficient_for_activity";
            rec.targetLocation = "closer to router";
            return rec;
        }
        
        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        // RULE 5: Default = stay current (good enough)
        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        rec.action = "stay_current";
        rec.reasonCode = "sufficient_for_activity";
        rec.targetLocation = null;
        return rec;
    }
    
    /**
     * Check if activity needs high bandwidth.
     */
    private static boolean isHighBandwidthActivity(String activity) {
        return "streaming".equals(activity) || 
               "gaming".equals(activity) || 
               "video_call".equals(activity);
    }
    
    /**
     * Check if current WiFi is sufficient for the activity.
     */
    private static boolean isSufficientForActivity(
        String signal, 
        String latency, 
        String bandwidth, 
        String activity
    ) {
        switch (activity) {
            case "gaming":
                // Gaming needs excellent latency and at least fair signal
                return ("excellent".equals(latency) || "good".equals(latency)) && 
                       !"poor".equals(signal) && 
                       !"very_poor".equals(signal);
            
            case "video_call":
                // Video calls need good latency and stable signal
                return ("excellent".equals(latency) || "good".equals(latency)) && 
                       !"poor".equals(signal) && 
                       !"very_poor".equals(signal);
            
            case "streaming":
                // Streaming needs good bandwidth
                return !"poor".equals(bandwidth);
            
            case "browsing":
                // Browsing is least demanding
                return !"very_poor".equals(signal);
            
            default:
                // Unknown activity, be permissive
                return true;
        }
    }
    
    /**
     * Recommendation result class.
     */
    public static class Recommendation {
        public String action;          // "stay_current", "move_location", "switch_band"
        public String reasonCode;      // Why this decision was made (for debugging)
        public String targetLocation;  // If moving, where to move (can be null)
        
        @Override
        public String toString() {
            return "Recommendation{" +
                    "action='" + action + '\'' +
                    ", reasonCode='" + reasonCode + '\'' +
                    ", targetLocation='" + targetLocation + '\'' +
                    '}';
        }
    }
    
    /**
     * Example usage demonstrating the complete flow.
     */
    public static void exampleUsage() {
        // Step 1: Already collected and classified WiFi data
        String signalStrength = "good";
        String latency = "excellent";
        String bandwidth = "good";
        String frequency = "2.4GHz";
        String activity = "streaming";
        
        // Step 2: Make algorithmic decision (instant, local)
        Recommendation recommendation = makeRecommendation(
            signalStrength, latency, bandwidth, frequency, activity
        );
        
        System.out.println("Decision: " + recommendation);
        // Output: Decision: Recommendation{action='switch_band', reasonCode='optimization_opportunity', targetLocation='null'}
        
        // Step 3: Now send to /explain API to get friendly explanation
        // (See example API call below)
    }
    
    /**
     * Example API request to get explanation for the recommendation.
     * 
     * This would be called AFTER makeRecommendation() to get a friendly
     * explanation that can be shown to the user.
     */
    public static void exampleExplainAPICall() {
        /*
        POST http://raspberry-pi-ip:5000/explain
        Content-Type: application/json
        
        {
          "location": "living_room",
          "activity": "streaming",
          "measurements": {
            "signal_strength": "good",
            "latency": "excellent",
            "bandwidth": "good"
          },
          "recommendation": {
            "action": "switch_band"
          }
        }
        
        Response:
        {
          "status": "success",
          "explanation": "You're on a good connection, but there's a faster network option available that would work great from where you are. Switching to it will give you much better speeds for streaming - think faster loading and higher quality video!"
        }
        */
    }
}

/**
 * COMPLETE ANDROID FLOW EXAMPLE:
 * 
 * 1. Measure WiFi data (6-12 seconds)
 * 2. Classify locally using WiFiClassifier (instant)
 * 3. Decide locally using WiFiRecommendationEngine (instant)
 * 4. Show decision to user immediately
 * 5. Call /explain API async to get friendly explanation (2-4 seconds)
 * 6. Display explanation in chatbot UI when ready
 * 
 * Benefits:
 * - User sees recommendation in ~6-12 seconds (fast!)
 * - Explanation enhances understanding (educational)
 * - Works offline (can skip explanation if no network)
 * - 100% deterministic decisions (no AI surprises)
 * - Small, fast LLM only explains, doesn't decide
 */

