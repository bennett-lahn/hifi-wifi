# Quick Reference - WiFi Assistant Explainer Mode

**Version 2.0 - Chatbot Explainer Architecture**

---

## 🎯 Architecture: Android Decides, LLM Explains

```
Android: Measure → Classify → Decide (6-12s) → Show User ✅
   ↓ (optional, async)
Pi API: Generate friendly explanation (2-4s) → Display in chat
```

---

## 📊 Classification Thresholds

### Signal Strength (RSSI)
```
excellent   : -30 to -50 dBm
good        : -50 to -60 dBm
fair        : -60 to -70 dBm
poor        : -70 to -80 dBm
very_poor   : -80 to -90 dBm
```

### Latency
```
excellent   : < 20 ms
good        : 20-50 ms
fair        : 50-100 ms
poor        : > 100 ms
```

### Bandwidth (Speed)
```
excellent   : ≥ 500 Mbps
good        : 100-500 Mbps
fair        : 50-100 Mbps
poor        : < 50 Mbps
```

---

## 🔧 Android Implementation

### 1. Classify (WiFiClassifier.java)
```java
String signal = WiFiClassifier.classifySignalStrength(rssiDbm);
String latency = WiFiClassifier.classifyLatency(latencyMs);
String bandwidth = WiFiClassifier.classifyBandwidth(speedMbps);
```

### 2. Decide (WiFiRecommendationEngine.java)
```java
Recommendation rec = WiFiRecommendationEngine.makeRecommendation(
    signal, latency, bandwidth, frequency, activity
);
// rec.action: "stay_current", "move_location", "switch_band"
```

### 3. Explain (Optional - API call)
```java
POST /explain with classifications + decision
Response: Friendly chatbot explanation text
```

---

## 📤 API Endpoint: /explain

**Request:**
```json
POST http://raspberry-pi-ip:5000/explain

{
  "location": "living_room",
  "activity": "gaming",
  "measurements": {
    "signal_strength": "excellent",
    "latency": "excellent",
    "bandwidth": "excellent"
  },
  "recommendation": {
    "action": "stay_current"
  }
}
```

**Response:**
```json
{
  "status": "success",
  "explanation": "Your connection is excellent right now! With strong signal, fast latency, and perfect bandwidth, everything's running smoothly for gaming.",
  "metadata": {
    "location": "living_room",
    "activity": "gaming",
    "recommendation": "stay_current"
  }
}
```

---

## 💬 Example Explanations

### Stay Current
> "Your connection is excellent right now! Everything's running smoothly for gaming. No need to change anything!"

### Move Location
> "Your signal is weak here, which could slow down your video call. Moving closer to the router will give you a stronger connection."

### Switch Band
> "You're on a good connection, but there's a faster network option available that would work great from where you are."

**Note:** No technical jargon (no "2.4GHz", "5GHz", "dBm")

---

## 🎮 Decision Rules

```
Excellent everything → stay_current
Poor/very_poor signal → move_location (closer to router)
Good signal on 2.4GHz + high bandwidth activity → switch_band
Insufficient for activity → move_location
Otherwise → stay_current
```

---

## 🚀 Testing

```bash
# Test direct service
python3 -c "
from ollama_service import OllamaService, OllamaConfig
service = OllamaService(OllamaConfig())
result = service.explain_wifi_recommendation(
    'living_room', 'gaming',
    {'signal_strength': 'excellent', 'latency': 'excellent', 'bandwidth': 'excellent'},
    {'action': 'stay_current'}
)
print(result['explanation'])
"

# Test API endpoint
curl -X POST http://localhost:5000/explain \
  -H "Content-Type: application/json" \
  -d '{
    "location": "living_room",
    "activity": "gaming",
    "measurements": {
      "signal_strength": "excellent",
      "latency": "excellent",
      "bandwidth": "excellent"
    },
    "recommendation": {"action": "stay_current"}
  }'
```

---

## ⏱️ Performance

- **Android Decision:** 6-12 seconds (measurement + classification + decision)
- **LLM Explanation:** +2-4 seconds (optional, async)
- **Total with explanation:** 8-16 seconds
- **Offline mode:** Works without explanation (6-12s)

---

## 📚 Documentation

- **`EXPLAINER_MODE.md`** - Complete architecture
- **`ANDROID_QUICK_START.md`** - Developer guide
- **`IMPLEMENTATION_COMPLETE.md`** - Summary
- **`WiFiClassifier.java`** - Classification code
- **`WiFiRecommendationEngine.java`** - Decision code
- **`README.md`** - Project overview

---

## ✅ Key Benefits

- ✅ **100% reliable decisions** (algorithm, not AI)
- ✅ **Friendly explanations** (conversational chatbot)
- ✅ **Fast** (6-12s to show recommendation)
- ✅ **Offline capable** (works without Pi)
- ✅ **No jargon** (user-friendly language)

---

**Status:** 🚀 Ready for Android Integration  
**Version:** 2.0 - Explainer Mode

