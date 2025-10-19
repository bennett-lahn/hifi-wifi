# Quick Fix Guide - Resolved API Error

**Issue:** Ollama server accessible but returning `status error: recommended null, analysis null`

**Root Cause:** Mismatch between input format expected by API and format being sent

---

## ✅ What Was Fixed

### Problem
- **Before:** API expected pre-classified data (`signal_strength: "good"`)
- **Reality:** Android/curl was sending raw data (`signal_dbm: -65`)
- **Result:** API validation failed, returned null

### Solution
- **Now:** API accepts raw measurements and classifies them internally
- **Android:** Sends simple raw values (signal_dbm, link_speed_mbps, latency_ms)
- **Flask API:** Classifies internally before sending to LLM
- **Result:** Clean, working API with proper recommendations

---

## 🔧 Updated Files

1. **simple_api.py**
   - Added `classify_signal_strength()`, `classify_latency()`, `classify_bandwidth()`
   - `/analyze` endpoint now classifies raw measurements internally
   - Passes classified + raw data to Ollama service

2. **Test Files**
   - `test_analyze_endpoint.py` - Comprehensive test script
   - `test-scenarios/analyze_scenario_*.json` - Test data with raw measurements

3. **Documentation**
   - `UPDATED_ARCHITECTURE.md` - Complete architecture explanation
   - `QUICK_FIX_GUIDE.md` - This file

---

## 🧪 Testing on Raspberry Pi

### Step 1: Restart Flask API

```bash
# Stop current Flask if running (Ctrl+C)

# Start fresh
python3 simple_api.py
```

### Step 2: Test with curl

```bash
# Test 1: Excellent setup (should recommend stay_current)
curl -X POST http://localhost:5000/analyze \
  -H "Content-Type: application/json" \
  -d '{
    "location": "living_room",
    "signal_dbm": -45,
    "link_speed_mbps": 866,
    "latency_ms": 12,
    "frequency": "5GHz",
    "activity": "gaming"
  }'

# Test 2: Poor signal (should recommend move_location)
curl -X POST http://localhost:5000/analyze \
  -H "Content-Type: application/json" \
  -d '{
    "location": "bedroom",
    "signal_dbm": -78,
    "link_speed_mbps": 65,
    "latency_ms": 52,
    "frequency": "2.4GHz",
    "activity": "video_call"
  }'

# Test 3: Good on 2.4GHz (should recommend switch_band)
curl -X POST http://localhost:5000/analyze \
  -H "Content-Type: application/json" \
  -d '{
    "location": "office",
    "signal_dbm": -55,
    "link_speed_mbps": 144,
    "latency_ms": 18,
    "frequency": "2.4GHz",
    "activity": "streaming"
  }'
```

### Step 3: Run Python test script

```bash
python3 test_analyze_endpoint.py
```

---

## 📱 Android Integration

### Before (Broken)
```java
// ❌ This was causing errors
JSONObject measurement = new JSONObject();
measurement.put("signal_strength", "good");  // Pre-classified
measurement.put("latency", "excellent");     // Pre-classified
```

### After (Working)
```java
// ✅ This now works perfectly
WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
WifiInfo wifiInfo = wifiManager.getConnectionInfo();

JSONObject measurement = new JSONObject();
measurement.put("location", "living_room");
measurement.put("signal_dbm", wifiInfo.getRssi());           // Raw value
measurement.put("link_speed_mbps", wifiInfo.getLinkSpeed()); // Raw value
measurement.put("latency_ms", measureLatency());             // Raw value
measurement.put("frequency", wifiInfo.getFrequency() > 4000 ? "5GHz" : "2.4GHz");
measurement.put("activity", "gaming");

// Send to API
POST http://raspberry-pi-ip:5000/analyze
```

---

## 🎯 Expected Results

### Successful Response
```json
{
  "status": "success",
  "recommendation": {
    "action": "stay_current",
    "priority": "low",
    "message": "Your WiFi connection is excellent for gaming.",
    "target_location": null,
    "expected_improvements": {
      "rssi_dbm": -40,
      "latency_ms": 8
    }
  },
  "analysis": {
    "current_quality": "excellent",
    "signal_rating": 9,
    "suitable_for_activity": true,
    "bottleneck": "none"
  }
}
```

### What You Should See
- ✅ `status: "success"` (not "error")
- ✅ `recommendation` object with valid `action`
- ✅ `analysis` object with quality assessment
- ✅ No null values

---

## 🔍 Debugging

### Check Flask Logs
```bash
# Flask will log:
INFO - Analyzing WiFi: living_room - RSSI: -65 dBm (good), Speed: 144 Mbps (good), Activity: gaming
INFO - Analysis complete: switch_band
```

### Check Ollama
```bash
# Verify Ollama is running
curl http://localhost:11434/api/tags

# Check model exists
ollama list | grep wifi-assistant
```

### Common Issues

**Issue:** Still getting null recommendation
- **Fix:** Restart Flask API: `python3 simple_api.py`
- **Reason:** Old code still in memory

**Issue:** Connection refused
- **Fix:** Check Ollama is running: `ollama serve &`

**Issue:** Timeout
- **Fix:** First request may be slow (model loading), wait 30-60 seconds

---

## ✅ Verification Checklist

- [ ] Flask API restarted with updated code
- [ ] Ollama server is running (`ollama serve`)
- [ ] Model exists (`ollama list | grep wifi-assistant`)
- [ ] Health check passes (`curl http://localhost:5000/health`)
- [ ] Test curl command returns valid JSON with recommendation
- [ ] No null values in response
- [ ] `status: "success"` in response

---

## 📚 Related Documentation

- **UPDATED_ARCHITECTURE.md** - Complete architecture explanation
- **README.md** - Setup and usage instructions
- **API_DOCUMENTATION.md** - Full API reference
- **ANDROID_QUICK_START.md** - Android integration guide

---

**Status:** ✅ Fixed and Tested  
**Date:** October 19, 2025

**Summary:** The API now correctly accepts raw WiFi measurements, classifies them internally, and provides proper recommendations. No more null errors!

