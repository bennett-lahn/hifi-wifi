/**
 * WiFiClassifier - Classification helper for Android WiFi measurements
 * 
 * This class provides methods to classify raw WiFi measurements into
 * human-readable categories that the AI model can better understand.
 * 
 * Usage:
 *   1. Collect raw WiFi data using Android APIs
 *   2. Classify using these methods
 *   3. Send both classified + raw values to the API
 *   
 * @author Hi-Fi WiFi Optimization Team
 * @version 1.0
 */

package com.example.hifiwifi;

import org.json.JSONException;
import org.json.JSONObject;

public class WiFiClassifier {
    
    /**
     * Classify signal strength (RSSI) into quality categories.
     * 
     * @param rssiDbm Signal strength in dBm (negative value, e.g., -42)
     * @return Classification: "excellent", "good", "fair", "poor", or "very_poor"
     */
    public static String classifySignalStrength(int rssiDbm) {
        if (rssiDbm >= -50) return "excellent";  // -30 to -50 dBm
        if (rssiDbm >= -60) return "good";       // -50 to -60 dBm
        if (rssiDbm >= -70) return "fair";       // -60 to -70 dBm
        if (rssiDbm >= -80) return "poor";       // -70 to -80 dBm
        return "very_poor";                      // -80 to -90 dBm
    }
    
    /**
     * Classify latency (ping) into quality categories.
     * 
     * @param latencyMs Latency in milliseconds
     * @return Classification: "excellent", "good", "fair", or "poor"
     */
    public static String classifyLatency(int latencyMs) {
        if (latencyMs < 20) return "excellent";   // <20ms - perfect for real-time
        if (latencyMs < 50) return "good";        // 20-50ms - suitable for most uses
        if (latencyMs < 100) return "fair";       // 50-100ms - noticeable delays
        return "poor";                            // >100ms - frustrating lag
    }
    
    /**
     * Classify bandwidth (link speed) into quality categories.
     * 
     * @param linkSpeedMbps Link speed in Mbps
     * @return Classification: "excellent", "good", "fair", or "poor"
     */
    public static String classifyBandwidth(int linkSpeedMbps) {
        if (linkSpeedMbps >= 500) return "excellent";  // >500 Mbps - 4K streaming
        if (linkSpeedMbps >= 100) return "good";       // 100-500 Mbps - HD streaming
        if (linkSpeedMbps >= 50) return "fair";        // 50-100 Mbps - SD streaming
        return "poor";                                 // <50 Mbps - basic tasks
    }
    
    /**
     * Create a complete measurement JSON object with classifications.
     * 
     * @param location Room name (e.g., "living_room")
     * @param rssiDbm Signal strength in dBm
     * @param latencyMs Latency in milliseconds
     * @param linkSpeedMbps Link speed in Mbps
     * @param frequency Frequency band ("5GHz" or "2.4GHz")
     * @param activity User activity ("gaming", "video_call", "streaming", "browsing")
     * @return JSONObject ready to send to the API
     * @throws JSONException if JSON creation fails
     */
    public static JSONObject createMeasurement(
        String location,
        int rssiDbm,
        int latencyMs,
        int linkSpeedMbps,
        String frequency,
        String activity
    ) throws JSONException {
        JSONObject measurement = new JSONObject();
        
        // Location and activity
        measurement.put("location", location);
        measurement.put("activity", activity);
        
        // Signal strength (classified + raw)
        measurement.put("signal_strength", classifySignalStrength(rssiDbm));
        measurement.put("signal_dbm", rssiDbm);
        
        // Latency (classified + raw)
        measurement.put("latency", classifyLatency(latencyMs));
        measurement.put("latency_ms", latencyMs);
        
        // Bandwidth (classified + raw)
        measurement.put("bandwidth", classifyBandwidth(linkSpeedMbps));
        measurement.put("link_speed_mbps", linkSpeedMbps);
        
        // Frequency band
        measurement.put("frequency", frequency);
        
        return measurement;
    }
    
    /**
     * Example usage demonstrating WiFi data collection and classification.
     */
    public static void exampleUsage(Context context) {
        // 1. Get WiFi data from Android APIs
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        
        // 2. Extract raw values
        int rssiDbm = wifiInfo.getRssi();                // e.g., -42 dBm
        int linkSpeedMbps = wifiInfo.getLinkSpeed();     // e.g., 866 Mbps
        String ssid = wifiInfo.getSSID();
        
        // Determine frequency (5GHz or 2.4GHz)
        int frequencyMhz = wifiInfo.getFrequency();      // API 21+
        String frequency = (frequencyMhz > 4000) ? "5GHz" : "2.4GHz";
        
        // Measure latency (via ping/HTTP test - implement separately)
        int latencyMs = measureLatency();                 // e.g., 12 ms
        
        // 3. Create classified measurement
        try {
            JSONObject measurement = createMeasurement(
                "living_room",      // location
                rssiDbm,            // e.g., -42
                latencyMs,          // e.g., 12
                linkSpeedMbps,      // e.g., 866
                frequency,          // "5GHz"
                "gaming"            // current activity
            );
            
            // 4. Send to API
            sendToAPI(measurement);
            
        } catch (JSONException e) {
            Log.e("WiFiClassifier", "Error creating measurement", e);
        }
    }
    
    /**
     * Helper method to determine frequency band from frequency in MHz.
     * 
     * @param frequencyMhz Frequency in MHz (e.g., 2437, 5180)
     * @return "5GHz" or "2.4GHz"
     */
    public static String getFrequencyBand(int frequencyMhz) {
        return (frequencyMhz > 4000) ? "5GHz" : "2.4GHz";
    }
    
    /**
     * Measure network latency using HTTP ping test.
     * 
     * @return Latency in milliseconds, or -1 if failed
     */
    private static int measureLatency() {
        // Implementation: Perform HTTP HEAD request to router or gateway
        // Measure round-trip time
        // Return average of 3-5 pings
        
        // Example implementation:
        try {
            long startTime = System.currentTimeMillis();
            
            URL url = new URL("http://192.168.1.1");  // Router IP
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("HEAD");
            connection.setConnectTimeout(5000);
            connection.connect();
            
            long endTime = System.currentTimeMillis();
            return (int) (endTime - startTime);
            
        } catch (Exception e) {
            return -1;  // Failed to measure
        }
    }
    
    /**
     * Send classified measurement to the WiFi optimization API.
     * 
     * @param measurement JSONObject with classified measurements
     */
    private static void sendToAPI(JSONObject measurement) {
        // Implementation: Use Retrofit or OkHttp to POST to API
        // URL: http://raspberry-pi-ip:5000/analyze
        
        // Example with Retrofit:
        /*
        ApiService api = RetrofitClient.getClient().create(ApiService.class);
        Call<ApiResponse> call = api.analyzeMeasurement(measurement);
        call.enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse result = response.body();
                    handleRecommendation(result);
                }
            }
            
            @Override
            public void onFailure(Call<ApiResponse> call, Throwable t) {
                Log.e("WiFiClassifier", "API call failed", t);
            }
        });
        */
    }
}

/**
 * Example JSON output from createMeasurement():
 * 
 * {
 *   "location": "living_room",
 *   "signal_strength": "excellent",
 *   "signal_dbm": -42,
 *   "latency": "excellent",
 *   "latency_ms": 12,
 *   "bandwidth": "excellent",
 *   "link_speed_mbps": 866,
 *   "frequency": "5GHz",
 *   "activity": "gaming"
 * }
 * 
 * This gets sent to: POST http://raspberry-pi-ip:5000/analyze
 * 
 * API will respond with:
 * {
 *   "status": "success",
 *   "recommendation": {
 *     "action": "stay_current",
 *     "priority": "low",
 *     "message": "Your WiFi is excellent for gaming",
 *     "reasoning": "Excellent signal and latency ensure optimal gaming...",
 *     ...
 *   },
 *   "analysis": {
 *     "current_quality": "excellent",
 *     "suitable_for_activity": true,
 *     "why_suitable": "Setup is perfect for gaming...",
 *     ...
 *   }
 * }
 */

