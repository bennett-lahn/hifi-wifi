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
    private String baseUrl = "http://10.19.26.184:5000";
    private static final String ANALYZE_ENDPOINT = "/analyze";
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
         * Called when an analysis recommendation is successfully received from SLM
         * Override this for full access to the response structure
         * 
         * @param response Complete analysis response from /analyze endpoint
         */
        default void onAnalysisReceived(AnalyzeResponse response) {
            // Default implementation extracts message and calls onExplanationReceived
            // This provides backward compatibility and convenience
            if (response != null) {
                String message = response.toDisplayMessage();
                onExplanationReceived(message);
            }
        }
        
        /**
         * Called when an explanation is successfully received from Pi
         * Also called by default when analysis is received (with formatted message)
         * 
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
                .readTimeout(180, TimeUnit.SECONDS)  // SLM may take up to 3 minutes to generate response
                .writeTimeout(10, TimeUnit.SECONDS)
                .build();
        
        this.executorService = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.gson = new Gson();
    }
    
    /**
     * Request class for /analyze endpoint - New Android Format
     * Matches Flask API's expected structure with measurements array
     */
    public static class AnalyzeRequest {
        public Measurement[] measurements;
        public Summary summary;  // Optional summary data
        
        public static class Measurement {
            public String roomName;         // e.g., "Living Room"
            public String activityType;     // e.g., "gaming", "streaming", "video_call"
            public String frequencyBand;    // e.g., "5GHz", "2.4GHz"
            public Classification classification;
            
            public static class Classification {
                public String signal_strength;  // "excellent", "good", "okay", "bad", "marginal"
                public String latency;          // "excellent", "good", "okay", "bad", "marginal"
                public String bandwidth;        // "excellent", "good", "okay", "bad", "marginal"
                public String jitter;           // "excellent", "good", "okay", "bad", "marginal"
                public String packet_loss;      // "excellent", "good", "okay", "bad", "marginal"
            }
        }
        
        public static class Summary {
            // Optional summary fields - can be expanded later
            public int totalMeasurements;
            public long timestamp;
        }
    }
    
    /**
     * Response class from /analyze endpoint
     * Matches Flask API response structure
     */
    public static class AnalyzeResponse {
        public String status;               // "success" or "error"
        public Recommendation recommendation;
        public Analysis analysis;
        public String error;                // Present if status is "error"
        
        public static class Recommendation {
            public String action;           // "stay_current", "move_location", "switch_band"
            public String reason;           // Human-readable reason (from SLM)
            public String message;          // Alternative field name for reason
            public String priority;         // "high", "medium", "low"
            public String target_location;  // Optional, for move_location
        }
        
        public static class Analysis {
            public String location;
            public String activity;
            public String current_quality;  // "excellent", "good", "okay", "bad", "marginal"
            public Metrics metrics;
            
            public static class Metrics {
                public String signal_strength;
                public String latency;
                public String bandwidth;
                public String jitter;
                public String packet_loss;
            }
        }
        
        /**
         * Extract a user-friendly message from the analysis response
         * Instead of showing raw JSON, this formats it nicely for chat display
         * 
         * @return Formatted message string suitable for chat display
         */
        public String toDisplayMessage() {
            if (recommendation == null) {
                return "Analysis complete, but no recommendation available.";
            }
            
            // Get the message text (check both "reason" and "message" fields)
            String messageText = null;
            if (recommendation.reason != null && !recommendation.reason.isEmpty()) {
                messageText = recommendation.reason;
            } else if (recommendation.message != null && !recommendation.message.isEmpty()) {
                messageText = recommendation.message;
            }
            
            if (messageText == null) {
                return "Analysis complete, but no details available.";
            }
            
            // Return just the message text - don't add extra formatting
            // The SLM's response should already be well-formatted
            return messageText;
        }
        
        /**
         * Get a brief summary of the analysis
         * 
         * @return Brief summary string
         */
        public String getSummary() {
            if (analysis == null) {
                return "WiFi analysis complete";
            }
            
            String location = analysis.location != null ? analysis.location : "your location";
            String quality = analysis.current_quality != null ? analysis.current_quality : "unknown";
            
            return String.format("WiFi in %s is %s", location, quality);
        }
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
        public String explanation;  // Contains JSON string that needs to be parsed
        public Metadata metadata;
        public String error;  // Present if status is "error"
        
        public static class Metadata {
            public String location;
            public String activity;
            public String recommendation;
        }
        
        /**
         * Extract the actual message from the explanation field
         * The explanation field contains a JSON string that needs to be parsed
         * to extract the actual message text
         * 
         * @param gson Gson instance for parsing nested JSON
         * @return The extracted message text, or the raw explanation if parsing fails
         */
        public String getMessage(com.google.gson.Gson gson) {
            if (explanation == null || explanation.isEmpty()) {
                return "No explanation available";
            }
            
            try {
                // Try to parse the explanation as nested JSON
                // It might contain: {"status": "success", "recommendation": {"message": "..."}}
                com.google.gson.JsonObject jsonObj = gson.fromJson(explanation, com.google.gson.JsonObject.class);
                
                // Try to extract message from recommendation
                if (jsonObj.has("recommendation")) {
                    com.google.gson.JsonObject recommendation = jsonObj.getAsJsonObject("recommendation");
                    
                    // Check for "message" field
                    if (recommendation.has("message")) {
                        return recommendation.get("message").getAsString();
                    }
                    
                    // Check for "reason" field
                    if (recommendation.has("reason")) {
                        return recommendation.get("reason").getAsString();
                    }
                }
                
                // If no nested structure, return the explanation as-is
                // (it might already be a plain text message)
                return explanation;
                
            } catch (Exception e) {
                // If parsing fails, return the explanation as-is
                // It might already be plain text
                Log.w(TAG, "Could not parse nested JSON in explanation, using as-is", e);
                return explanation;
            }
        }
    }
    
    /**
     * Send WiFi measurements to SLM for analysis and recommendation
     * 
     * The SLM will analyze the classified measurements and provide a recommendation
     * on what action to take (stay, move, or switch band).
     * 
     * @param location Room name (e.g., "living_room", "bedroom", "office")
     * @param activity User activity (e.g., "gaming", "streaming", "video_call", "browsing")
     * @param signalStrength Classified signal strength ("excellent", "good", "okay", "bad", "marginal")
     * @param latency Classified latency ("excellent", "good", "okay", "bad", "marginal")
     * @param bandwidth Classified bandwidth ("excellent", "good", "okay", "bad", "marginal")
     * @param jitter Classified jitter ("excellent", "good", "okay", "bad", "marginal")
     * @param packetLoss Classified packet loss ("excellent", "good", "okay", "bad", "marginal")
     * @param frequency Frequency band ("2.4GHz" or "5GHz")
     * @param callback Callback for receiving results
     */
    public void requestAnalysis(
            String location,
            String activity,
            String signalStrength,
            String latency,
            String bandwidth,
            String jitter,
            String packetLoss,
            String frequency,
            HTTPCallback callback
    ) {
        executorService.execute(() -> {
            try {
                // Build request object in new Android format
                AnalyzeRequest analyzeRequest = new AnalyzeRequest();
                
                // Create measurement object
                AnalyzeRequest.Measurement measurement = new AnalyzeRequest.Measurement();
                measurement.roomName = location;  // Keep as-is (e.g., "Living Room")
                measurement.activityType = activity;  // e.g., "gaming", "streaming"
                measurement.frequencyBand = frequency;  // e.g., "5GHz", "2.4GHz"
                
                // Create classification object with lowercase values
                measurement.classification = new AnalyzeRequest.Measurement.Classification();
                measurement.classification.signal_strength = signalStrength.toLowerCase();
                measurement.classification.latency = latency.toLowerCase();
                measurement.classification.bandwidth = bandwidth.toLowerCase();
                measurement.classification.jitter = jitter.toLowerCase();
                measurement.classification.packet_loss = packetLoss.toLowerCase();
                
                // Wrap in measurements array
                analyzeRequest.measurements = new AnalyzeRequest.Measurement[]{measurement};
                
                // Add optional summary
                analyzeRequest.summary = new AnalyzeRequest.Summary();
                analyzeRequest.summary.totalMeasurements = 1;
                analyzeRequest.summary.timestamp = System.currentTimeMillis();
                
                // Convert to JSON
                String jsonBody = gson.toJson(analyzeRequest);
                Log.d(TAG, "Sending analysis request (new Android format): " + jsonBody);
                
                // Create HTTP request
                RequestBody body = RequestBody.create(jsonBody, JSON);
                Request request = new Request.Builder()
                        .url(baseUrl + ANALYZE_ENDPOINT)
                        .post(body)
                        .build();
                
                // Execute request (SLM may take up to 3 minutes to respond)
                try (Response response = httpClient.newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        String errorMsg = "HTTP " + response.code() + ": " + response.message();
                        Log.e(TAG, errorMsg);
                        notifyError(callback, errorMsg);
                        return;
                    }
                    
                    // Parse response
                    String responseBody = response.body().string();
                    Log.d(TAG, "Received analysis response: " + responseBody.substring(0, Math.min(100, responseBody.length())) + "...");
                    
                    AnalyzeResponse analyzeResponse = gson.fromJson(responseBody, AnalyzeResponse.class);
                    
                    if (analyzeResponse == null) {
                        Log.e(TAG, "Failed to parse analysis response - gson returned null");
                        notifyError(callback, "Failed to parse server response");
                        return;
                    }
                    
                    if ("success".equals(analyzeResponse.status)) {
                        Log.i(TAG, "Analysis received successfully");
                        
                        // Extract and log the message that will be displayed
                        String displayMessage = analyzeResponse.toDisplayMessage();
                        Log.d(TAG, "Extracted message: " + displayMessage.substring(0, Math.min(50, displayMessage.length())) + "...");
                        
                        notifyAnalysis(callback, analyzeResponse);
                    } else {
                        String error = analyzeResponse.error != null ? analyzeResponse.error : "Unknown error";
                        Log.e(TAG, "Analysis failed: " + error);
                        notifyError(callback, "Analysis failed: " + error);
                    }
                }
                
            } catch (com.google.gson.JsonSyntaxException e) {
                Log.e(TAG, "Failed to parse JSON response from analysis", e);
                notifyError(callback, "Invalid response format from server");
            } catch (IOException e) {
                Log.e(TAG, "Network error while requesting analysis", e);
                notifyError(callback, "Network error: " + e.getMessage() + 
                    "\n\nMake sure Raspberry Pi is reachable at " + baseUrl);
            } catch (Exception e) {
                Log.e(TAG, "Unexpected error while requesting analysis", e);
                notifyError(callback, "Error: " + e.getMessage());
            }
        });
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
                
                // Execute request (SLM may take up to 3 minutes to respond)
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
                    
                    if (explainResponse == null) {
                        Log.e(TAG, "Failed to parse explanation response - gson returned null");
                        notifyError(callback, "Failed to parse server response");
                        return;
                    }
                    
                    if ("success".equals(explainResponse.status)) {
                        Log.i(TAG, "Explanation received successfully");
                        
                        // Extract the actual message from the nested JSON
                        String message = explainResponse.getMessage(gson);
                        Log.d(TAG, "Extracted message: " + message.substring(0, Math.min(50, message.length())) + "...");
                        
                        notifyExplanation(callback, message);
                    } else {
                        String error = explainResponse.error != null ? explainResponse.error : "Unknown error";
                        Log.e(TAG, "Request failed: " + error);
                        notifyError(callback, "Request failed: " + error);
                    }
                }
                
            } catch (com.google.gson.JsonSyntaxException e) {
                Log.e(TAG, "Failed to parse JSON response from explanation", e);
                notifyError(callback, "Invalid response format from server");
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
    
    private void notifyAnalysis(HTTPCallback callback, AnalyzeResponse response) {
        mainHandler.post(() -> {
            if (callback != null) {
                callback.onAnalysisReceived(response);
            }
        });
    }
    
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
     * Request class for general chat endpoint
     * Matches Flask API /chat endpoint format
     */
    public static class ChatRequest {
        public String query;         // Natural language question
        public Boolean format_json;  // Optional: return JSON format (default: false)
    }
    
    /**
     * Response class for general chat endpoint
     */
    public static class ChatResponse {
        public String status;
        public String response;
        public String error;
        
        /**
         * Extract the message from the chat response
         * Handles cases where the SLM returns:
         * - Plain text messages
         * - Markdown-formatted JSON (```json ... ```)
         * - JSON strings that need parsing
         * 
         * @return The response message, or a fallback if not available
         */
        public String getMessage() {
            if (response == null || response.isEmpty()) {
                if (error != null && !error.isEmpty()) {
                    return "Error: " + error;
                }
                return "No response received from SLM";
            }
            
            String cleanedResponse = response.trim();
            
            // Check if response is wrapped in markdown code blocks
            if (cleanedResponse.startsWith("```json") || cleanedResponse.startsWith("```")) {
                // Remove markdown code block formatting
                cleanedResponse = cleanedResponse.replaceFirst("^```json\\s*", "");
                cleanedResponse = cleanedResponse.replaceFirst("^```\\s*", "");
                cleanedResponse = cleanedResponse.replaceFirst("```\\s*$", "");
                cleanedResponse = cleanedResponse.trim();
            }
            
            // Check if the cleaned response looks like JSON
            if (cleanedResponse.startsWith("{") && cleanedResponse.endsWith("}")) {
                try {
                    // Try to parse as JSON and extract message
                    com.google.gson.JsonObject jsonObj = new com.google.gson.Gson()
                        .fromJson(cleanedResponse, com.google.gson.JsonObject.class);
                    
                    // Try different possible field names for the message
                    if (jsonObj.has("message")) {
                        return jsonObj.get("message").getAsString();
                    }
                    if (jsonObj.has("response")) {
                        return jsonObj.get("response").getAsString();
                    }
                    if (jsonObj.has("text")) {
                        return jsonObj.get("text").getAsString();
                    }
                    
                    // If it has a recommendation field, try to extract from there
                    if (jsonObj.has("recommendation")) {
                        com.google.gson.JsonElement recElement = jsonObj.get("recommendation");
                        if (recElement.isJsonObject()) {
                            com.google.gson.JsonObject recObj = recElement.getAsJsonObject();
                            if (recObj.has("message")) {
                                return recObj.get("message").getAsString();
                            }
                            if (recObj.has("reason")) {
                                return recObj.get("reason").getAsString();
                            }
                        } else if (recElement.isJsonPrimitive()) {
                            return recElement.getAsString();
                        }
                    }
                    
                    // If we can't find a specific message field, return the original
                    Log.w(TAG, "Could not find message field in JSON response, using original");
                    return response;
                    
                } catch (Exception e) {
                    // If JSON parsing fails, return the cleaned text
                    Log.w(TAG, "Failed to parse chat response as JSON, using as plain text", e);
                    return cleanedResponse;
                }
            }
            
            // If not JSON, return the cleaned response as-is
            return cleanedResponse;
        }
    }
    
    /**
     * Send a general chat query to Raspberry Pi
     * Matches Flask API /chat endpoint
     * 
     * @param query User's question or message
     * @param callback Callback for receiving results
     */
    public void sendChatQuery(String query, HTTPCallback callback) {
        sendChatQuery(query, false, callback);
    }
    
    /**
     * Send a general chat query to Raspberry Pi with optional JSON formatting
     * Matches Flask API /chat endpoint
     * 
     * @param query User's question or message
     * @param formatJson If true, request JSON-formatted response
     * @param callback Callback for receiving results
     */
    public void sendChatQuery(
            String query,
            boolean formatJson,
            HTTPCallback callback
    ) {
        executorService.execute(() -> {
            try {
                // Build request object matching Flask API format
                ChatRequest chatRequest = new ChatRequest();
                chatRequest.query = query;
                chatRequest.format_json = formatJson;
                
                // Convert to JSON
                String jsonBody = gson.toJson(chatRequest);
                Log.d(TAG, "Sending chat query: " + query);
                Log.v(TAG, "Full chat request: " + jsonBody);
                
                // Create HTTP request
                RequestBody body = RequestBody.create(jsonBody, JSON);
                Request request = new Request.Builder()
                        .url(baseUrl + "/chat")
                        .post(body)
                        .build();
                
                // Execute request (may take up to 3 minutes for SLM response)
                try (Response response = httpClient.newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        String errorMsg = "HTTP " + response.code() + ": " + response.message();
                        Log.e(TAG, "Chat request failed: " + errorMsg);
                        
                        // Try to read error response body for more details
                        try {
                            String errorBody = response.body() != null ? response.body().string() : "No error details";
                            Log.e(TAG, "Error response body: " + errorBody);
                        } catch (Exception e) {
                            Log.e(TAG, "Could not read error body", e);
                        }
                        
                        notifyError(callback, errorMsg);
                        return;
                    }
                    
                    // Check if response body exists
                    if (response.body() == null) {
                        Log.e(TAG, "Response body is null");
                        notifyError(callback, "Empty response from server");
                        return;
                    }
                    
                    // Parse response
                    String responseBody = response.body().string();
                    Log.d(TAG, "Received chat response: " + responseBody.substring(0, Math.min(100, responseBody.length())) + "...");
                    
                    ChatResponse chatResponse = gson.fromJson(responseBody, ChatResponse.class);
                    
                    if (chatResponse == null) {
                        Log.e(TAG, "Failed to parse chat response - gson returned null");
                        Log.e(TAG, "Raw response was: " + responseBody);
                        notifyError(callback, "Failed to parse server response");
                        return;
                    }
                    
                    if ("success".equals(chatResponse.status)) {
                        Log.i(TAG, "Chat response received successfully");
                        
                        // Extract just the message text, not the full JSON
                        String message = chatResponse.getMessage();
                        Log.d(TAG, "Extracted message: " + message.substring(0, Math.min(50, message.length())) + "...");
                        
                        notifyExplanation(callback, message);
                    } else {
                        String error = chatResponse.error != null ? chatResponse.error : "Unknown error";
                        Log.e(TAG, "Chat request failed: " + error);
                        notifyError(callback, "Chat failed: " + error);
                    }
                }
                
            } catch (com.google.gson.JsonSyntaxException e) {
                Log.e(TAG, "Failed to parse JSON response from chat query", e);
                notifyError(callback, "Invalid response format from server");
            } catch (IOException e) {
                Log.e(TAG, "Network error while sending chat query", e);
                notifyError(callback, "Network error: " + e.getMessage() + 
                    "\n\nMake sure Raspberry Pi is reachable at " + baseUrl);
            } catch (Exception e) {
                Log.e(TAG, "Unexpected error while sending chat query", e);
                notifyError(callback, "Error: " + e.getMessage());
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
