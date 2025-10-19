package com.example.hifiwifi.services;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.gson.Gson;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Service for HTTP communication with Raspberry Pi
 * Sends WiFi measurements and receives friendly explanations from Qwen LLM
 * 
 * Replaces BLEService for simpler HTTP-based communication since both devices
 * are on the same WiFi network.
 */
public class HTTPService {
    
    private static final String TAG = "HTTPService";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    
    // TODO: Update with your Raspberry Pi's IP address once it's set up
    // Example: "http://192.168.1.100:5000"
    private String baseUrl = "http://192.168.1.100:5000";
    private static final String EXPLAIN_ENDPOINT = "/explain";
    private static final String HEALTH_ENDPOINT = "/health";
    
    private OkHttpClient httpClient;
    private ExecutorService executorService;
    private Handler mainHandler;
    private Gson gson;
    
    /**
     * Callback interface for HTTP operations
     */
    public interface HTTPCallback {
        /**
         * Called when an explanation is successfully received from Pi
         * @param explanation User-friendly explanation text from Qwen
         */
        void onExplanationReceived(String explanation);
        
        /**
         * Called when health check succeeds (Pi is reachable)
         */
        void onHealthCheckSuccess();
        
        /**
         * Called when an error occurs
         * @param error Error message
         */
        void onError(String error);
    }
    
    public HTTPService() {
        // Configure HTTP client with appropriate timeouts
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)  // Qwen takes 5-15 seconds to generate
                .writeTimeout(10, TimeUnit.SECONDS)
                .build();
        
        this.executorService = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.gson = new Gson();
    }
    
    /**
     * Request class for /explain endpoint
     * Matches the API format expected by Raspberry Pi Flask server
     */
    public static class ExplainRequest {
        public String location;
        public String activity;
        public Measurements measurements;
        public Recommendation recommendation;
        
        public static class Measurements {
            public String signal_strength;  // "excellent", "good", "fair", "poor", "very_poor"
            public String latency;          // "excellent", "good", "fair", "poor"
            public String bandwidth;        // "excellent", "good", "fair", "poor"
        }
        
        public static class Recommendation {
            public String action;           // "stay_current", "move_location", "switch_band"
            public String target_location;  // Optional, for move_location action
        }
    }
    
    /**
     * Response class from /explain endpoint
     */
    public static class ExplainResponse {
        public String status;
        public String explanation;
        public Metadata metadata;
        public String error;  // Present if status is "error"
        
        public static class Metadata {
            public String location;
            public String activity;
            public String recommendation;
        }
    }
    
    /**
     * Send WiFi explanation request to Raspberry Pi
     * 
     * The Pi will use Qwen LLM to generate a friendly, conversational explanation
     * of why the given recommendation makes sense for the user's situation.
     * 
     * @param location Room name (e.g., "living_room", "bedroom", "office")
     * @param activity User activity (e.g., "gaming", "streaming", "video_call", "browsing")
     * @param signalStrength Classified signal strength ("excellent", "good", "fair", "poor", "very_poor")
     * @param latency Classified latency ("excellent", "good", "fair", "poor")
     * @param bandwidth Classified bandwidth ("excellent", "good", "fair", "poor")
     * @param action Recommended action ("stay_current", "move_location", "switch_band")
     * @param targetLocation Optional target location (only used for "move_location" action)
     * @param callback Callback for receiving results
     */
    public void requestExplanation(
            String location,
            String activity,
            String signalStrength,
            String latency,
            String bandwidth,
            String action,
            String targetLocation,
            HTTPCallback callback
    ) {
        executorService.execute(() -> {
            try {
                // Build request object
                ExplainRequest explainRequest = new ExplainRequest();
                explainRequest.location = location;
                explainRequest.activity = activity;
                
                explainRequest.measurements = new ExplainRequest.Measurements();
                explainRequest.measurements.signal_strength = signalStrength;
                explainRequest.measurements.latency = latency;
                explainRequest.measurements.bandwidth = bandwidth;
                
                explainRequest.recommendation = new ExplainRequest.Recommendation();
                explainRequest.recommendation.action = action;
                if (targetLocation != null && !targetLocation.isEmpty()) {
                    explainRequest.recommendation.target_location = targetLocation;
                }
                
                // Convert to JSON
                String jsonBody = gson.toJson(explainRequest);
                Log.d(TAG, "Sending explanation request: " + jsonBody);
                
                // Create HTTP request
                RequestBody body = RequestBody.create(jsonBody, JSON);
                Request request = new Request.Builder()
                        .url(baseUrl + EXPLAIN_ENDPOINT)
                        .post(body)
                        .build();
                
                // Execute request (this may take 5-15 seconds for Qwen to respond)
                try (Response response = httpClient.newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        String errorMsg = "HTTP " + response.code() + ": " + response.message();
                        Log.e(TAG, errorMsg);
                        notifyError(callback, errorMsg);
                        return;
                    }
                    
                    // Parse response
                    String responseBody = response.body().string();
                    Log.d(TAG, "Received explanation response: " + responseBody.substring(0, Math.min(100, responseBody.length())) + "...");
                    
                    ExplainResponse explainResponse = gson.fromJson(responseBody, ExplainResponse.class);
                    
                    if ("success".equals(explainResponse.status)) {
                        Log.i(TAG, "Explanation received successfully");
                        notifyExplanation(callback, explainResponse.explanation);
                    } else {
                        String error = explainResponse.error != null ? explainResponse.error : "Unknown error";
                        Log.e(TAG, "Request failed: " + error);
                        notifyError(callback, "Request failed: " + error);
                    }
                }
                
            } catch (IOException e) {
                Log.e(TAG, "Network error while requesting explanation", e);
                notifyError(callback, "Network error: " + e.getMessage() + 
                    "\n\nMake sure Raspberry Pi is reachable at " + baseUrl);
            } catch (Exception e) {
                Log.e(TAG, "Unexpected error while requesting explanation", e);
                notifyError(callback, "Error: " + e.getMessage());
            }
        });
    }
    
    /**
     * Check if Raspberry Pi is reachable and Flask API is running
     * 
     * @param callback Callback for receiving results
     */
    public void checkHealth(HTTPCallback callback) {
        executorService.execute(() -> {
            try {
                Log.d(TAG, "Checking health at: " + baseUrl + HEALTH_ENDPOINT);
                
                Request request = new Request.Builder()
                        .url(baseUrl + HEALTH_ENDPOINT)
                        .get()
                        .build();
                
                try (Response response = httpClient.newCall(request).execute()) {
                    if (response.isSuccessful()) {
                        Log.i(TAG, "Health check passed - Pi is reachable");
                        notifyHealthCheck(callback);
                    } else {
                        String errorMsg = "Pi not reachable: HTTP " + response.code();
                        Log.e(TAG, errorMsg);
                        notifyError(callback, errorMsg);
                    }
                }
                
            } catch (IOException e) {
                Log.e(TAG, "Health check failed - cannot reach Pi", e);
                notifyError(callback, "Cannot reach Raspberry Pi at " + baseUrl + 
                    "\n\nMake sure:\n1. Pi is on same WiFi network\n2. Flask server is running\n3. IP address is correct");
            }
        });
    }
    
    /**
     * Update the Raspberry Pi's base URL
     * Call this when you know the Pi's IP address
     * 
     * @param ipAddress Pi's IP address (e.g., "192.168.1.100")
     * @param port Flask server port (default: 5000)
     */
    public void setBaseUrl(String ipAddress, int port) {
        this.baseUrl = "http://" + ipAddress + ":" + port;
        Log.i(TAG, "Base URL updated to: " + this.baseUrl);
    }
    
    /**
     * Get the current base URL
     * 
     * @return Current base URL
     */
    public String getBaseUrl() {
        return baseUrl;
    }
    
    // Notify callbacks on main thread for UI updates
    
    private void notifyExplanation(HTTPCallback callback, String explanation) {
        mainHandler.post(() -> {
            if (callback != null) {
                callback.onExplanationReceived(explanation);
            }
        });
    }
    
    private void notifyHealthCheck(HTTPCallback callback) {
        mainHandler.post(() -> {
            if (callback != null) {
                callback.onHealthCheckSuccess();
            }
        });
    }
    
    private void notifyError(HTTPCallback callback, String error) {
        mainHandler.post(() -> {
            if (callback != null) {
                callback.onError(error);
            }
        });
    }
    
    /**
     * Clean up resources when service is no longer needed
     * Call this in onDestroy() or when shutting down
     */
    public void shutdown() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
}

