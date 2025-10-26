# HTTPService Usage Guide

Quick reference for using the new HTTPService to communicate with Raspberry Pi.

## Setup

### 1. Initialize the Service

```java
// In your Activity, Fragment, or ViewModel
private HTTPService httpService;

@Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    
    // Initialize HTTP service
    httpService = new HTTPService();
    
    // Optional: Set Pi's IP address if known
    // httpService.setBaseUrl("192.168.1.100", 5000);
}
```

### 2. Don't Forget to Clean Up

```java
@Override
protected void onDestroy() {
    super.onDestroy();
    if (httpService != null) {
        httpService.shutdown();
    }
}
```

## Basic Usage Examples

### Health Check (Test Connection)

```java
httpService.checkHealth(new HTTPService.HTTPCallback() {
    @Override
    public void onHealthCheckSuccess() {
        // Pi is reachable!
        Toast.makeText(context, "Connected to Raspberry Pi!", Toast.LENGTH_SHORT).show();
        // Now safe to request explanations
    }
    
    @Override
    public void onExplanationReceived(String explanation) {
        // Not called for health check
    }
    
    @Override
    public void onError(String error) {
        // Can't reach Pi
        Toast.makeText(context, "Cannot reach Pi: " + error, Toast.LENGTH_LONG).show();
        Log.e(TAG, "Health check failed: " + error);
    }
});
```

### Request WiFi Explanation

```java
// After you've classified WiFi measurements and made a recommendation decision
httpService.requestExplanation(
    "living_room",          // location: where user is
    "gaming",               // activity: what user is doing
    "excellent",            // signal_strength classification
    "excellent",            // latency classification
    "excellent",            // bandwidth classification
    "stay_current",         // recommendation action
    null,                   // target_location (only for move_location action)
    new HTTPService.HTTPCallback() {
        @Override
        public void onExplanationReceived(String explanation) {
            // Got friendly explanation from Qwen!
            // Display in your chat UI
            chatTextView.setText(explanation);
            
            // Or add to chat history
            chatAdapter.addMessage(explanation);
        }
        
        @Override
        public void onHealthCheckSuccess() {
            // Not called for explanation request
        }
        
        @Override
        public void onError(String error) {
            // Request failed
            Toast.makeText(context, "Error: " + error, Toast.LENGTH_LONG).show();
            Log.e(TAG, "Explanation request failed: " + error);
        }
    }
);
```

### Complete Example with Classification

```java
// Step 1: Measure WiFi (using WiFiMeasurementService)
wifiMeasurementService.measureCurrentRoom("living_room", new WiFiMeasurementService.MeasurementCallback() {
    @Override
    public void onMeasurementComplete(RoomMeasurement measurement) {
        // Step 2: Classify measurements
        String signalClass = classifySignalStrength(measurement.rssi);
        String latencyClass = classifyLatency(measurement.latencyMs);
        String bandwidthClass = classifyBandwidth(measurement.downloadSpeed);
        
        // Step 3: Make recommendation decision (your algorithm)
        String action = makeRecommendation(signalClass, latencyClass, bandwidthClass);
        String targetLocation = action.equals("move_location") ? "closer to router" : null;
        
        // Step 4: Request explanation from Pi
        httpService.requestExplanation(
            "living_room",
            "gaming",
            signalClass,
            latencyClass,
            bandwidthClass,
            action,
            targetLocation,
            new HTTPService.HTTPCallback() {
                @Override
                public void onExplanationReceived(String explanation) {
                    // Display explanation to user
                    showExplanationDialog(explanation);
                }
                
                @Override
                public void onHealthCheckSuccess() {}
                
                @Override
                public void onError(String error) {
                    Toast.makeText(context, "Error: " + error, Toast.LENGTH_SHORT).show();
                }
            }
        );
    }
    
    @Override
    public void onMeasurementUpdate(NetworkMetrics metrics) {
        // Update progress UI
    }
    
    @Override
    public void onError(String error) {
        Toast.makeText(context, "Measurement error: " + error, Toast.LENGTH_SHORT).show();
    }
});

// Helper: Classify signal strength
private String classifySignalStrength(int rssi) {
    if (rssi >= -50) return "excellent";
    if (rssi >= -60) return "good";
    if (rssi >= -70) return "fair";
    if (rssi >= -80) return "poor";
    return "very_poor";
}

// Helper: Classify latency
private String classifyLatency(double latencyMs) {
    if (latencyMs < 20) return "excellent";
    if (latencyMs < 50) return "good";
    if (latencyMs < 100) return "fair";
    return "poor";
}

// Helper: Classify bandwidth
private String classifyBandwidth(double speedMbps) {
    if (speedMbps > 500) return "excellent";
    if (speedMbps > 100) return "good";
    if (speedMbps > 50) return "fair";
    return "poor";
}

// Helper: Make recommendation (simplified)
private String makeRecommendation(String signal, String latency, String bandwidth) {
    if (signal.equals("excellent") && latency.equals("excellent")) {
        return "stay_current";
    }
    if (signal.equals("poor") || signal.equals("very_poor")) {
        return "move_location";
    }
    return "stay_current";
}
```

## Configuration

### Update Raspberry Pi IP Address

```java
// When you know the Pi's IP address (from user input or discovery)
String piIpAddress = "192.168.1.100";
int port = 5000;  // Default Flask port

httpService.setBaseUrl(piIpAddress, port);

// Or use URL directly
// httpService.setBaseUrl("http://192.168.1.100:5000");
```

### Get Current URL

```java
String currentUrl = httpService.getBaseUrl();
Log.d(TAG, "Currently connecting to: " + currentUrl);
```

## Error Handling

### Common Errors

1. **"Cannot reach Raspberry Pi"**
   - Pi is not on the network
   - Wrong IP address
   - Flask server not running
   - Firewall blocking

2. **"HTTP 404"**
   - Endpoint URL is wrong
   - Flask API not properly deployed

3. **"HTTP 500"**
   - Server error on Pi
   - Ollama not running
   - Model not loaded

4. **Timeout**
   - Network is slow
   - Qwen is taking longer than 30 seconds (rare)
   - Pi is overloaded

### Handling Errors Gracefully

```java
httpService.requestExplanation(..., new HTTPService.HTTPCallback() {
    @Override
    public void onError(String error) {
        if (error.contains("Cannot reach")) {
            // Show connection help dialog
            showConnectionHelpDialog();
        } else if (error.contains("timeout")) {
            // Retry or show "Pi is busy"
            Toast.makeText(context, "Request timed out. Pi might be busy.", Toast.LENGTH_LONG).show();
        } else {
            // Generic error
            Log.e(TAG, "Error: " + error);
            Toast.makeText(context, "Something went wrong", Toast.LENGTH_SHORT).show();
        }
    }
});
```

## Testing

### Test with Mock Data

```java
// Test explanation request without real measurements
httpService.requestExplanation(
    "test_room",
    "gaming",
    "excellent",
    "excellent",
    "excellent",
    "stay_current",
    null,
    callback
);
```

### Test Different Scenarios

```java
// Scenario 1: Excellent connection
requestExplanation("living_room", "gaming", "excellent", "excellent", "excellent", "stay_current", null, callback);

// Scenario 2: Poor signal
requestExplanation("bedroom", "video_call", "poor", "fair", "fair", "move_location", "closer to router", callback);

// Scenario 3: Switch band
requestExplanation("office", "streaming", "good", "excellent", "good", "switch_band", null, callback);
```

## Performance Tips

1. **Call health check on app start** to verify Pi is reachable
2. **Cache explanations** if showing the same scenario repeatedly
3. **Show loading indicator** - explanations take 5-15 seconds
4. **Handle timeouts gracefully** - Qwen might be slow on older Pi
5. **Don't spam requests** - rate limit to avoid overwhelming Pi

## Migration from BLEService

If you were using BLEService before:

```java
// OLD (BLE)
bleService.sendMeasurement(measurement, callback);

// NEW (HTTP)
httpService.requestExplanation(
    location,
    activity,
    signalClass,
    latencyClass,
    bandwidthClass,
    recommendationAction,
    targetLocation,
    callback
);
```

Key differences:
- HTTP is simpler (no scanning, pairing, connection state)
- HTTP takes classified data (not raw measurements)
- HTTP returns only explanation text (not full analysis)
- Recommendation decision is made locally (not by Pi)

## Next Steps

1. Sync with Gradle (for OkHttp dependency)
2. Update your Activities/Fragments to use HTTPService
3. Test health check with actual Raspberry Pi
4. Test explanation requests with different scenarios
5. Add UI for Pi IP configuration

## Questions?

See main project documentation or `RASPBERRY_PI_DEPLOYMENT.md` for Pi setup instructions.

