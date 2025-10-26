# Android Developer Quick Start Guide

**WiFi Assistant - Explainer Mode**  
**For Android App Developers**

---

## 🎯 What You Need to Know

The WiFi assistant uses a **hybrid approach**:
- **Android app**: Does all measurement, classification, and decision-making (fast, reliable, offline)
- **Raspberry Pi**: Only provides friendly chatbot explanations (optional, educational)

**Result:** Fast, reliable app that works offline with optional explanations!

---

## 📋 Implementation Checklist

### ✅ Step 1: Collect WiFi Data (6-12 seconds)

```java
// Native Android APIs (instant)
WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
WifiInfo wifiInfo = wifiManager.getConnectionInfo();

int rssiDbm = wifiInfo.getRssi();              // e.g., -65 dBm
int linkSpeedMbps = wifiInfo.getLinkSpeed();   // e.g., 144 Mbps
int frequencyMhz = wifiInfo.getFrequency();    // e.g., 2437 MHz

// Latency test (1-2 seconds)
int latencyMs = measureLatency();  // HTTP HEAD request

// Actual speed test (5-10 seconds) - using speedtest library
int downloadSpeedMbps = measureDownloadSpeed();
```

### ✅ Step 2: Classify Locally (instant)

Use `WiFiClassifier.java` (included in this repo):

```java
String signalStrength = WiFiClassifier.classifySignalStrength(rssiDbm);
String latency = WiFiClassifier.classifyLatency(latencyMs);
String bandwidth = WiFiClassifier.classifyBandwidth(downloadSpeedMbps);
String frequency = WiFiClassifier.getFrequencyBand(frequencyMhz);
```

### ✅ Step 3: Make Decision Locally (instant)

Use `WiFiRecommendationEngine.java` (included in this repo):

```java
WiFiRecommendationEngine.Recommendation rec = 
    WiFiRecommendationEngine.makeRecommendation(
        signalStrength,  // "excellent", "good", "fair", "poor", "very_poor"
        latency,         // "excellent", "good", "fair", "poor"
        bandwidth,       // "excellent", "good", "fair", "poor"
        frequency,       // "5GHz" or "2.4GHz"
        "gaming"         // or "streaming", "video_call", "browsing"
    );

// rec.action = "stay_current", "move_location", or "switch_band"
// rec.targetLocation = "closer to router" (if action is move_location)
```

### ✅ Step 4: Show Result to User (immediately)

```java
// Display measurements
displaySignalStrength(signalStrength);  // Show as dots: ●●●○○
displayLatency(latency);
displayBandwidth(bandwidth);

// Display recommendation
if (rec.action.equals("stay_current")) {
    showMessage("✅ Stay where you are - WiFi is great!");
} else if (rec.action.equals("move_location")) {
    showMessage("📍 Move closer to router for better signal");
} else if (rec.action.equals("switch_band")) {
    showMessage("🔄 Switch to faster network for better speeds");
}
```

### ✅ Step 5: Get Explanation (async, optional, 2-4 seconds)

**Important:** This is OPTIONAL and runs in background. App already works without it!

```java
// Build request
JSONObject request = new JSONObject();
request.put("location", getCurrentLocation());  // "living_room"
request.put("activity", getCurrentActivity());   // "gaming"

JSONObject measurements = new JSONObject();
measurements.put("signal_strength", signalStrength);
measurements.put("latency", latency);
measurements.put("bandwidth", bandwidth);
request.put("measurements", measurements);

JSONObject recommendation = new JSONObject();
recommendation.put("action", rec.action);
recommendation.put("target_location", rec.targetLocation);
request.put("recommendation", recommendation);

// Call API asynchronously
new Thread(() -> {
    try {
        String response = httpPost(
            "http://raspberry-pi-ip:5000/explain",
            request.toString()
        );
        
        JSONObject result = new JSONObject(response);
        String explanation = result.getString("explanation");
        
        runOnUiThread(() -> {
            showChatbotMessage(explanation);
        });
    } catch (Exception e) {
        // Silent fail - app works without explanation
        Log.d("WiFi", "Explanation not available");
    }
}).start();
```

---

## 📊 Classification Thresholds

### Signal Strength (from RSSI)
```java
if (rssiDbm >= -50) return "excellent";  // -30 to -50 dBm
if (rssiDbm >= -60) return "good";       // -50 to -60 dBm
if (rssiDbm >= -70) return "fair";       // -60 to -70 dBm
if (rssiDbm >= -80) return "poor";       // -70 to -80 dBm
return "very_poor";                      // -80 to -90 dBm
```

### Latency (from ping test)
```java
if (latencyMs < 20) return "excellent";   // <20ms
if (latencyMs < 50) return "good";        // 20-50ms
if (latencyMs < 100) return "fair";       // 50-100ms
return "poor";                            // >100ms
```

### Bandwidth (from speed test)
```java
if (speedMbps >= 500) return "excellent";  // >500 Mbps
if (speedMbps >= 100) return "good";       // 100-500 Mbps
if (speedMbps >= 50) return "fair";        // 50-100 Mbps
return "poor";                             // <50 Mbps
```

---

## 🎨 UI Example

```
┌─────────────────────────────────────┐
│  Hi-Fi WiFi Assistant               │
│  ─────────────────────────────────  │
│                                     │
│  📍 Living Room                     │
│  🎮 Gaming                          │
│                                     │
│  ────────────────────────────────  │
│  WiFi Status                        │
│  ────────────────────────────────  │
│                                     │
│  Signal    Good    ●●●○○            │
│  Speed     Good    ●●●○○            │
│  Latency   Excellent ●●●●●          │
│                                     │
│  ────────────────────────────────  │
│  💡 Recommendation                  │
│  ────────────────────────────────  │
│                                     │
│  🔄 Switch to Faster Network        │
│                                     │
│  💬 Why?                            │
│  You're on a good connection, but   │
│  there's a faster network option    │
│  available that would work great    │
│  from where you are. Switching will │
│  give you much better speeds!       │
│                                     │
│  [ Apply ]     [ Stay Here ]        │
│                                     │
└─────────────────────────────────────┘
```

---

## ⏱️ Performance Timeline

```
0s    → Start measurement
2s    → Got RSSI, link speed, frequency (instant native APIs)
      → Got latency (HTTP ping test)
6-12s → Got download speed (speed test)
      → Classified all values (instant local)
      → Made decision (instant local)
      → SHOW TO USER ✅ (6-12 seconds total)

+2-4s → Got explanation from Pi (optional async)
      → Show in chat bubble
```

**User Experience:**
- ✅ See measurements and recommendation in 6-12 seconds (fast!)
- ✅ Friendly explanation appears 2-4 seconds later (nice to have)
- ✅ App works offline without explanation (reliable)

---

## 🔧 Helper Methods

### Measure Latency
```java
private int measureLatency() {
    try {
        long startTime = System.currentTimeMillis();
        
        URL url = new URL("https://www.google.com");
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
```

### Measure Download Speed
```java
// Option 1: Using Speedtest Library (RECOMMENDED)
// Add to build.gradle: implementation 'fr.bmartel:speedtest:1.32'

private int measureDownloadSpeed() {
    SpeedTestSocket speedTest = new SpeedTestSocket();
    speedTest.startDownload("http://speedtest.tele2.net/5MB.zip");
    // Returns Mbps
}

// Option 2: DIY with known file
private int measureDownloadSpeed() {
    try {
        long startTime = System.currentTimeMillis();
        
        URL url = new URL("http://speedtest.tele2.net/5MB.zip");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        InputStream input = connection.getInputStream();
        
        byte[] buffer = new byte[1024];
        int totalBytes = 0;
        while (input.read(buffer) != -1) {
            totalBytes += buffer.length;
        }
        
        long endTime = System.currentTimeMillis();
        double seconds = (endTime - startTime) / 1000.0;
        double megabits = (totalBytes * 8) / 1_000_000.0;
        
        return (int) (megabits / seconds);  // Mbps
    } catch (Exception e) {
        return -1;
    }
}
```

---

## 📦 Files You Need

### From This Repo
1. `WiFiClassifier.java` - Classification logic
2. `WiFiRecommendationEngine.java` - Decision algorithm

### API Details
- **Base URL:** `http://raspberry-pi-ip:5000`
- **Endpoint:** `POST /explain`
- **Content-Type:** `application/json`

---

## ✅ Testing Checklist

- [ ] Can measure RSSI (signal strength)
- [ ] Can measure latency (ping test)
- [ ] Can measure download speed (speed test)
- [ ] Classifications work correctly
- [ ] Recommendations make sense
- [ ] UI shows measurements clearly
- [ ] UI shows recommendation prominently
- [ ] Explanation loads asynchronously
- [ ] App works without explanation (offline mode)
- [ ] All Activities added to manifest
- [ ] All Permissions added to manifest

---

## 🎯 Required Permissions

```xml
<manifest>
    <uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
    <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
    <uses-permission android:name="android.permission.INTERNET" />
</manifest>
```

---

## 📚 Resources

- **API Documentation:** `API_DOCUMENTATION.md`
- **Classification Details:** `EXPLAINER_MODE.md`
- **Full README:** `README.md`

---

## 💡 Tips

1. **Cache Raspberry Pi IP**: Let user enter it once, save in SharedPreferences
2. **Timeout Gracefully**: If /explain takes >10s, show recommendation without explanation
3. **Offline First**: Always show measurements and recommendation, explanation is bonus
4. **Visual Feedback**: Use dots (●●●○○) or progress bars for classifications
5. **Location Tracking**: Remember where each room is for better recommendations
6. **Activity Detection**: Auto-detect activity from running apps if possible

---

## 🚀 You're Ready!

**Quick Start:**
1. Copy `WiFiClassifier.java` and `WiFiRecommendationEngine.java` to your project
2. Add WiFi permissions to manifest
3. Implement measurement collection (6-12 seconds)
4. Show measurements + recommendation immediately
5. Optionally call `/explain` API for chatbot explanation

**Result:** Fast, reliable WiFi assistant app with optional friendly explanations!

---

**Questions?** See `EXPLAINER_MODE.md` or `API_DOCUMENTATION.md`

