# WiFi Optimization API Documentation for App Developers

This document provides complete integration details for Android developers connecting to the Ollama-powered WiFi optimization service.

## Overview

The `ollama_service.py` Python service runs on a Raspberry Pi and provides AI-powered WiFi optimization recommendations via HTTP API. Your Android app collects WiFi measurements and sends them to this service for analysis.

## Architecture

```
Android App (WiFi Scanner)
    ↓ HTTP POST (JSON)
Raspberry Pi (Ollama Service)
    ↓ Local AI Inference
Qwen 3 Model (0.6B)
    ↓ JSON Response
Android App (Display Recommendations)
```

## Raspberry Pi Setup

### Hardware Requirements
- Raspberry Pi 4 (4GB+ RAM recommended) or Raspberry Pi 5
- MicroSD card (32GB+)
- Network connection (WiFi or Ethernet)

### Software Installation

```bash
# Update system
sudo apt update && sudo apt upgrade -y

# Install Ollama (ARM64 support)
curl https://ollama.ai/install.sh | sh

# Pull Qwen 3 model (optimized for Pi)
ollama pull qwen3:0.6b

# Install Python dependencies
pip3 install requests

# Create the custom WiFi assistant model
cd /path/to/Hi-FI/models/
ollama create wifi-assistant -f Modelfile
```

### Running the Service

```bash
# Start Ollama server (runs on port 11434)
ollama serve &

# Test the Python service
python3 ollama_service.py
```

### Making Service Accessible to Android App

Option 1: **Local Network Access**
```bash
# Find Raspberry Pi IP address
hostname -I

# Android app will connect to: http://192.168.1.XXX:11434
```

Option 2: **Flask REST API Wrapper** (Recommended for production)
```python
# Create simple_api.py
from flask import Flask, request, jsonify
from ollama_service import OllamaService

app = Flask(__name__)
service = OllamaService()

@app.route('/analyze', methods=['POST'])
def analyze():
    measurement = request.json
    result = service.analyze_wifi_measurement(measurement)
    return jsonify(result)

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000)
```

## Android Data Collection

### Required Permissions (AndroidManifest.xml)

```xml
<uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.INTERNET" />
```

### WiFi Data Collection Code

```java
import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import org.json.JSONObject;

public class WiFiDataCollector {
    
    private WifiManager wifiManager;
    
    public WiFiDataCollector(Context context) {
        wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
    }
    
    /**
     * Collect all WiFi measurement data
     * @param location User's current location (e.g., "living_room", "bedroom")
     * @param activity Current activity (e.g., "gaming", "video_call", "browsing", "streaming")
     * @return JSONObject ready to send to Ollama service
     */
    public JSONObject collectWiFiData(String location, String activity) {
        JSONObject measurement = new JSONObject();
        
        try {
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            
            // 1. Signal Strength (RSSI) - GUARANTEED
            int rssi = wifiInfo.getRssi();
            measurement.put("signal_dbm", rssi);
            
            // 2. Link Speed - GUARANTEED
            int linkSpeed = wifiInfo.getLinkSpeed(); // Mbps
            measurement.put("link_speed_mbps", linkSpeed);
            
            // 3. Frequency Band - GUARANTEED (Android 5.0+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                int frequency = wifiInfo.getFrequency(); // MHz
                String band = frequency > 4000 ? "5GHz" : "2.4GHz";
                measurement.put("frequency", band);
            } else {
                measurement.put("frequency", "unknown");
            }
            
            // 4. Latency - Calculate with ping test
            int latency = measureLatency(); // Custom method below
            measurement.put("latency_ms", latency);
            
            // 5. User context
            measurement.put("location", location);
            measurement.put("activity", activity);
            
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return measurement;
    }
    
    /**
     * Measure network latency with HTTP HEAD request
     * @return Latency in milliseconds
     */
    private int measureLatency() {
        try {
            long startTime = System.currentTimeMillis();
            HttpURLConnection connection = (HttpURLConnection) 
                new URL("https://www.google.com").openConnection();
            connection.setRequestMethod("HEAD");
            connection.setConnectTimeout(3000);
            connection.connect();
            int responseCode = connection.getResponseCode();
            connection.disconnect();
            
            if (responseCode == 200) {
                return (int) (System.currentTimeMillis() - startTime);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1; // Error
    }
}
```

## API Endpoint Specification

### Input Format (What Android Sends)

**Endpoint:** `POST http://<raspberry-pi-ip>:5000/analyze`

**Content-Type:** `application/json`

**Request Body:**
```json
{
  "location": "living_room",
  "signal_dbm": -45,
  "link_speed_mbps": 866,
  "latency_ms": 12,
  "frequency": "5GHz",
  "activity": "gaming"
}
```

**Field Specifications:**

| Field | Type | Range/Values | Source | Required |
|-------|------|--------------|--------|----------|
| `location` | string | User-defined room name | User input | Yes |
| `signal_dbm` | integer | -30 to -90 dBm | `getRssi()` | Yes |
| `link_speed_mbps` | integer | 1-866+ Mbps | `getLinkSpeed()` | Yes |
| `latency_ms` | integer | 0-1000+ ms | HTTP ping test | Yes |
| `frequency` | string | "2.4GHz" \| "5GHz" \| "unknown" | `getFrequency()` | Yes |
| `activity` | string | "gaming" \| "video_call" \| "streaming" \| "browsing" | User input | Yes |

### Output Format (What Android Receives)

**Success Response (200 OK):**
```json
{
  "status": "success",
  "recommendation": {
    "action": "move_location",
    "priority": "high",
    "message": "Your signal is weak for gaming. Move closer to the router or to a room with better coverage.",
    "target_location": "office",
    "expected_improvements": {
      "rssi_dbm": -50,
      "latency_ms": 15
    }
  },
  "analysis": {
    "current_quality": "poor",
    "signal_rating": 4,
    "suitable_for_activity": false,
    "bottleneck": "signal_strength"
  }
}
```

**Error Response:**
```json
{
  "status": "error",
  "error": "Connection timeout after 30 seconds",
  "recommendation": null,
  "analysis": null
}
```

### Response Field Definitions

#### `recommendation` Object

| Field | Type | Possible Values | Description |
|-------|------|-----------------|-------------|
| `action` | string | `"stay_current"` \| `"move_location"` \| `"switch_band"` | Recommended action |
| `priority` | string | `"low"` \| `"medium"` \| `"high"` | Urgency level |
| `message` | string | User-friendly text | Explanation for user |
| `target_location` | string | Room name or `null` | Suggested location to move to |
| `expected_improvements` | object | See below | Predicted improvements |
| `expected_improvements.rssi_dbm` | integer | -30 to -90 | Expected signal strength |
| `expected_improvements.latency_ms` | integer | 1-100+ | Expected latency |

#### `analysis` Object

| Field | Type | Possible Values | Description |
|-------|------|-----------------|-------------|
| `current_quality` | string | `"excellent"` \| `"good"` \| `"moderate"` \| `"poor"` \| `"very_poor"` | Overall quality assessment |
| `signal_rating` | integer | 0-10 | Numeric rating (10 = best) |
| `suitable_for_activity` | boolean | `true` \| `false` | Can handle current activity? |
| `bottleneck` | string | `"signal_strength"` \| `"latency"` \| `"bandwidth"` \| `"none"` | Primary issue |

## Android HTTP Request Implementation

### Using Retrofit (Recommended)

**1. Add dependency to `build.gradle`:**
```gradle
dependencies {
    implementation 'com.squareup.retrofit2:retrofit:2.9.0'
    implementation 'com.squareup.retrofit2:converter-gson:2.9.0'
}
```

**2. Create API interface:**
```java
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface WiFiOptimizationAPI {
    @POST("/analyze")
    Call<OptimizationResponse> analyzeWiFi(@Body WiFiMeasurement measurement);
}
```

**3. Create data classes:**
```java
public class WiFiMeasurement {
    public String location;
    public int signal_dbm;
    public int link_speed_mbps;
    public int latency_ms;
    public String frequency;
    public String activity;
}

public class OptimizationResponse {
    public String status;
    public Recommendation recommendation;
    public Analysis analysis;
    public String error;
}

public class Recommendation {
    public String action;
    public String priority;
    public String message;
    public String target_location;
    public ExpectedImprovements expected_improvements;
}

public class ExpectedImprovements {
    public int rssi_dbm;
    public int latency_ms;
}

public class Analysis {
    public String current_quality;
    public int signal_rating;
    public boolean suitable_for_activity;
    public String bottleneck;
}
```

**4. Make API call:**
```java
Retrofit retrofit = new Retrofit.Builder()
    .baseUrl("http://192.168.1.100:5000") // Your Pi's IP
    .addConverterFactory(GsonConverterFactory.create())
    .build();

WiFiOptimizationAPI api = retrofit.create(WiFiOptimizationAPI.class);

WiFiMeasurement measurement = new WiFiMeasurement();
measurement.location = "living_room";
measurement.signal_dbm = -65;
measurement.link_speed_mbps = 144;
measurement.latency_ms = 25;
measurement.frequency = "2.4GHz";
measurement.activity = "gaming";

api.analyzeWiFi(measurement).enqueue(new Callback<OptimizationResponse>() {
    @Override
    public void onResponse(Call<OptimizationResponse> call, Response<OptimizationResponse> response) {
        if (response.isSuccessful() && response.body() != null) {
            OptimizationResponse result = response.body();
            
            if ("success".equals(result.status)) {
                // Display recommendation to user
                String message = result.recommendation.message;
                String action = result.recommendation.action;
                int rating = result.analysis.signal_rating;
                
                // Update UI
                showRecommendation(message, action, rating);
            } else {
                // Handle error
                showError(result.error);
            }
        }
    }
    
    @Override
    public void onFailure(Call<OptimizationResponse> call, Throwable t) {
        showError("Network error: " + t.getMessage());
    }
});
```

## Signal Strength Reference Guide (for UI)

Display these guidelines in your app to help users understand signal quality:

### RSSI Scale (Signal Strength)

| RSSI Range | Quality | Color | Suitable Activities |
|------------|---------|-------|---------------------|
| -30 to -50 dBm | Excellent | 🟢 Green | All activities, 4K streaming, gaming |
| -50 to -60 dBm | Good | 🟢 Green | HD streaming, video calls, gaming |
| -60 to -70 dBm | Moderate | 🟡 Yellow | SD streaming, browsing, light gaming |
| -70 to -80 dBm | Poor | 🟠 Orange | Basic browsing only |
| -80 to -90 dBm | Very Poor | 🔴 Red | Barely usable |

### Latency Scale

| Latency | Quality | Suitable For |
|---------|---------|--------------|
| < 20ms | Excellent | Gaming, video calls |
| 20-50ms | Good | Most activities |
| 50-100ms | Moderate | Streaming, browsing |
| > 100ms | Poor | Frustrating experience |

### Frequency Band Comparison

| Band | Speed | Range | Interference | Best For |
|------|-------|-------|--------------|----------|
| 5GHz | ⚡ Faster | 📶 Shorter | ✨ Less | Gaming, streaming, close to router |
| 2.4GHz | 🐌 Slower | 📡 Longer | 🌊 More | Far from router, through walls |

## Example UI Flow

```
1. User opens app
   ├─ Select room location (dropdown: Living Room, Bedroom, Kitchen, etc.)
   └─ Select activity (dropdown: Gaming, Video Call, Streaming, Browsing)

2. User taps "Scan WiFi"
   ├─ App collects WiFi data (RSSI, link speed, frequency)
   ├─ App measures latency (1-2 seconds)
   └─ Progress indicator shown

3. App sends data to Raspberry Pi
   ├─ Shows "Analyzing..." message
   └─ Waits for response (5-10 seconds)

4. Display results
   ├─ Signal rating (0-10 with visual gauge)
   ├─ Quality badge (Excellent/Good/Moderate/Poor/Very Poor)
   ├─ Recommendation card with action and message
   ├─ "Current vs Expected" comparison if action suggested
   └─ Detailed breakdown (RSSI, frequency, latency, link speed)

5. Action buttons
   ├─ "Scan Again" (repeat measurement)
   ├─ "Try Another Room" (change location)
   └─ "Learn More" (explain signal strength)
```

## Testing & Debugging

### Test the Python Service Directly

```bash
# Test with curl
curl -X POST http://localhost:5000/analyze \
  -H "Content-Type: application/json" \
  -d '{
    "location": "living_room",
    "signal_dbm": -65,
    "link_speed_mbps": 144,
    "latency_ms": 25,
    "frequency": "2.4GHz",
    "activity": "gaming"
  }'
```

### Android Debug Logging

```java
// Log WiFi measurements before sending
Log.d("WiFi", "RSSI: " + measurement.signal_dbm + " dBm");
Log.d("WiFi", "Link Speed: " + measurement.link_speed_mbps + " Mbps");
Log.d("WiFi", "Frequency: " + measurement.frequency);
Log.d("WiFi", "Latency: " + measurement.latency_ms + " ms");

// Log API response
Log.d("WiFi", "Recommendation: " + response.recommendation.action);
Log.d("WiFi", "Message: " + response.recommendation.message);
```

## Performance Considerations

### Response Times
- **WiFi data collection:** ~100ms (instant)
- **Latency measurement:** 1-2 seconds
- **API request to Pi:** ~200ms
- **AI inference (Qwen 3):** 3-5 seconds
- **Total time:** 5-8 seconds

### Network Requirements
- Raspberry Pi and Android device must be on same network OR
- Port forwarding configured for remote access

### Battery Optimization
- Don't scan continuously - user-initiated only
- Cache results for 30-60 seconds
- Use WorkManager for background scanning if needed

## Error Handling

```java
// Handle common errors
switch (error.type) {
    case CONNECTION_TIMEOUT:
        showError("Raspberry Pi not responding. Check if Ollama service is running.");
        break;
    case NETWORK_ERROR:
        showError("Cannot reach WiFi optimization service. Check network connection.");
        break;
    case INVALID_RESPONSE:
        showError("Unexpected response from service. Try again.");
        break;
    case MODEL_ERROR:
        showError("AI model error. Contact support if issue persists.");
        break;
}
```

## Security Notes

⚠️ **For Hackathon/Development:**
- HTTP is acceptable for local network testing
- No authentication required

🔒 **For Production:**
- Use HTTPS with self-signed certificate
- Add API key authentication
- Rate limiting on Raspberry Pi
- Input validation on both sides

## Support & Resources

- **Ollama Documentation:** https://ollama.ai/docs
- **Qwen 3 Model:** https://ollama.ai/library/qwen3
- **Android WiFi API:** https://developer.android.com/reference/android/net/wifi/WifiManager
- **Raspberry Pi Setup:** https://www.raspberrypi.org/documentation/

## Example Test Scenarios

Use these for testing your app:

```json
// Excellent scenario
{
  "location": "office",
  "signal_dbm": -42,
  "link_speed_mbps": 866,
  "latency_ms": 8,
  "frequency": "5GHz",
  "activity": "gaming"
}

// Poor scenario - should recommend moving
{
  "location": "bedroom",
  "signal_dbm": -78,
  "link_speed_mbps": 65,
  "latency_ms": 52,
  "frequency": "2.4GHz",
  "activity": "video_call"
}

// Should recommend switching bands
{
  "location": "living_room",
  "signal_dbm": -55,
  "link_speed_mbps": 144,
  "latency_ms": 18,
  "frequency": "2.4GHz",
  "activity": "streaming"
}
```

