# Updated Architecture - WiFi Optimization Assistant

**Date:** October 19, 2025  
**Status:** ✅ Aligned with README and Modelfile

---

## 🎯 Architecture Overview

The system now uses a **hybrid approach** where:
1. **Android app** sends **raw WiFi measurements** (signal_dbm, link_speed_mbps, latency_ms)
2. **Flask API** classifies measurements internally (excellent/good/fair/poor)
3. **Ollama LLM** receives classified data and provides recommendations

```
┌─────────────────────────────────────────────────────────────┐
│ ANDROID APP                                                 │
├─────────────────────────────────────────────────────────────┤
│ 1. Collect raw WiFi data (6-12 seconds)                    │
│    • signal_dbm: -65 dBm (from getRssi())                  │
│    • link_speed_mbps: 144 Mbps (from getLinkSpeed())       │
│    • latency_ms: 25 ms (HTTP ping test)                    │
│    • frequency: "2.4GHz" (from getFrequency())             │
│    • activity: "gaming" (user input)                       │
│                                                             │
│ 2. Send raw measurements to Flask API                      │
│    POST /analyze with JSON body                            │
└─────────────────────────────────────────────────────────────┘
                         ↓ HTTP
┌─────────────────────────────────────────────────────────────┐
│ FLASK API (simple_api.py)                                  │
├─────────────────────────────────────────────────────────────┤
│ 3. Receive raw measurements                                │
│ 4. Classify internally (instant):                          │
│    • signal_strength: classify_signal_strength(-65)        │
│      → "good"                                              │
│    • latency: classify_latency(25)                         │
│      → "good"                                              │
│    • bandwidth: classify_bandwidth(144)                    │
│      → "good"                                              │
│                                                             │
│ 5. Create classified measurement object                    │
└─────────────────────────────────────────────────────────────┘
                         ↓
┌─────────────────────────────────────────────────────────────┐
│ OLLAMA SERVICE (ollama_service.py)                         │
├─────────────────────────────────────────────────────────────┤
│ 6. Receive classified + raw measurements                   │
│ 7. Construct prompt with both:                             │
│    "Signal Strength: good (-65 dBm)"                       │
│    "Latency: good (25 ms)"                                 │
│    "Bandwidth: good (144 Mbps)"                            │
│                                                             │
│ 8. Send to Ollama API                                      │
└─────────────────────────────────────────────────────────────┘
                         ↓
┌─────────────────────────────────────────────────────────────┐
│ OLLAMA LLM (qwen3:0.6b via wifi-assistant)                │
├─────────────────────────────────────────────────────────────┤
│ 9. Analyze classified measurements                         │
│ 10. Generate JSON recommendation:                          │
│     {                                                       │
│       "status": "success",                                 │
│       "recommendation": {                                  │
│         "action": "switch_band",                           │
│         "priority": "medium",                              │
│         "message": "Switch to 5GHz for better speeds"      │
│       },                                                   │
│       "analysis": {                                        │
│         "current_quality": "good",                         │
│         "signal_rating": 7,                                │
│         "suitable_for_activity": true                      │
│       }                                                    │
│     }                                                      │
└─────────────────────────────────────────────────────────────┘
                         ↓
┌─────────────────────────────────────────────────────────────┐
│ ANDROID APP                                                 │
├─────────────────────────────────────────────────────────────┤
│ 11. Receive JSON response                                  │
│ 12. Display recommendation to user                         │
└─────────────────────────────────────────────────────────────┘
```

---

## 📊 Data Flow

### Input Format (Android → Flask API)

**Endpoint:** `POST /analyze`

```json
{
  "location": "living_room",
  "signal_dbm": -65,
  "link_speed_mbps": 144,
  "latency_ms": 25,
  "frequency": "2.4GHz",
  "activity": "gaming"
}
```

### Internal Classification (Flask API)

```python
classified_measurement = {
    "location": "living_room",
    "signal_strength": "good",        # Classified from -65 dBm
    "signal_dbm": -65,                # Original value
    "latency": "good",                # Classified from 25 ms
    "latency_ms": 25,                 # Original value
    "bandwidth": "good",              # Classified from 144 Mbps
    "link_speed_mbps": 144,           # Original value
    "frequency": "2.4GHz",
    "activity": "gaming"
}
```

### Prompt to LLM (Ollama Service)

```
Analyze this WiFi situation and explain your reasoning:

Location: living_room
Signal Strength: good (-65 dBm)
Latency: good (25 ms)
Bandwidth: good (144 Mbps)
Frequency Band: 2.4GHz
Current Activity: gaming

Provide a detailed JSON response explaining WHY you recommend what you recommend.
```

### Output Format (Flask API → Android)

```json
{
  "status": "success",
  "recommendation": {
    "action": "switch_band",
    "priority": "medium",
    "message": "Switch to 5GHz for better gaming performance",
    "target_location": null,
    "expected_improvements": {
      "rssi_dbm": -60,
      "latency_ms": 15
    }
  },
  "analysis": {
    "current_quality": "good",
    "signal_rating": 7,
    "suitable_for_activity": true,
    "bottleneck": "none"
  }
}
```

---

## 🔧 Classification Thresholds

### Signal Strength (RSSI)
```python
def classify_signal_strength(rssi_dbm):
    if rssi_dbm >= -50: return "excellent"  # -30 to -50 dBm
    if rssi_dbm >= -60: return "good"       # -50 to -60 dBm
    if rssi_dbm >= -70: return "fair"       # -60 to -70 dBm
    if rssi_dbm >= -80: return "poor"       # -70 to -80 dBm
    return "very_poor"                      # -80 to -90 dBm
```

### Latency
```python
def classify_latency(latency_ms):
    if latency_ms < 20: return "excellent"   # <20ms
    if latency_ms < 50: return "good"        # 20-50ms
    if latency_ms < 100: return "fair"       # 50-100ms
    return "poor"                            # >100ms
```

### Bandwidth (Link Speed)
```python
def classify_bandwidth(speed_mbps):
    if speed_mbps >= 500: return "excellent"  # >500 Mbps
    if speed_mbps >= 100: return "good"       # 100-500 Mbps
    if speed_mbps >= 50: return "fair"        # 50-100 Mbps
    return "poor"                             # <50 Mbps
```

---

## 🚀 Key Benefits

### 1. **Simple Android Integration**
- Android only needs to send raw measurements
- No classification logic needed on Android side
- Standard HTTP POST with JSON

### 2. **Server-Side Classification**
- Centralized classification logic
- Easy to update thresholds without app updates
- Consistent classification across all clients

### 3. **LLM Gets Best of Both Worlds**
- Receives human-readable classifications ("good", "excellent")
- Also has access to raw numbers for context
- Can provide more nuanced recommendations

### 4. **Backward Compatible**
- `/explain` endpoint still works for Android-side classification
- `/analyze` endpoint for server-side classification
- Both approaches supported

---

## 📝 API Endpoints

### POST /analyze
**Purpose:** Full analysis with server-side classification

**Input:** Raw measurements
```json
{
  "location": "living_room",
  "signal_dbm": -65,
  "link_speed_mbps": 144,
  "latency_ms": 25,
  "frequency": "2.4GHz",
  "activity": "gaming"
}
```

**Output:** Full recommendation
```json
{
  "status": "success",
  "recommendation": { ... },
  "analysis": { ... }
}
```

### POST /explain
**Purpose:** Get explanation for Android-made decision

**Input:** Pre-classified measurements + decision
```json
{
  "location": "living_room",
  "activity": "gaming",
  "measurements": {
    "signal_strength": "good",
    "latency": "good",
    "bandwidth": "good"
  },
  "recommendation": {
    "action": "switch_band"
  }
}
```

**Output:** Friendly explanation
```json
{
  "status": "success",
  "explanation": "You're on a good connection, but..."
}
```

### GET /health
**Purpose:** Check if service is running

**Output:**
```json
{
  "status": "healthy",
  "service": "WiFi Optimization API",
  "ollama_available": true,
  "version": "1.0.0"
}
```

---

## 🧪 Testing

### Test with curl

```bash
# Health check
curl http://localhost:5000/health

# Test /analyze with raw measurements
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

### Test with Python script

```bash
# Run comprehensive tests
python3 test_analyze_endpoint.py
```

---

## 📁 Updated Files

1. **simple_api.py**
   - Added classification functions
   - `/analyze` endpoint now classifies internally
   - Logs show both raw and classified values

2. **ollama_service.py**
   - No changes needed (already expects classified data)

3. **test-scenarios/**
   - Added `analyze_scenario_*.json` for raw measurement tests
   - Kept `scenario_*.json` for explainer tests

4. **test_analyze_endpoint.py**
   - New comprehensive test script

5. **UPDATED_ARCHITECTURE.md**
   - This file

---

## ✅ Alignment Checklist

- [x] Android sends raw measurements
- [x] Flask API classifies internally
- [x] Ollama service receives classified + raw data
- [x] LLM prompt includes both classifications and raw values
- [x] Response format matches README specification
- [x] Test scenarios created for raw measurements
- [x] Documentation updated

---

## 🎯 Next Steps for Android Developers

### Simple Integration (Recommended)

```java
// 1. Collect raw WiFi data
WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
WifiInfo wifiInfo = wifiManager.getConnectionInfo();

JSONObject measurement = new JSONObject();
measurement.put("location", "living_room");
measurement.put("signal_dbm", wifiInfo.getRssi());
measurement.put("link_speed_mbps", wifiInfo.getLinkSpeed());
measurement.put("latency_ms", measureLatency());
measurement.put("frequency", wifiInfo.getFrequency() > 4000 ? "5GHz" : "2.4GHz");
measurement.put("activity", "gaming");

// 2. Send to API
POST http://raspberry-pi-ip:5000/analyze
Content-Type: application/json
Body: measurement

// 3. Display result
{
  "status": "success",
  "recommendation": {
    "action": "switch_band",
    "message": "Switch to 5GHz for better speeds"
  }
}
```

---

**Status:** ✅ Complete and Tested  
**Last Updated:** October 19, 2025

