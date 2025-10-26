# Hi-Fi WiFi Optimization Assistant

An AI-powered assistant for WiFi network optimization and analysis using Ollama and Qwen 3.

## Project Structure

```
Hi-FI/
├── models/
│   └── Modelfile                    # Qwen 3 0.6B model configuration
├── test-scenarios/                  # WiFi test scenario examples
│   ├── scenario_1_excellent.json
│   ├── scenario_2_poor.json
│   ├── scenario_3_moderate.json
│   └── scenario_4_switch_band.json
├── training-data/                   # For Phase 2 fine-tuning
├── ollama_service.py               # Core Python service for Ollama API
├── simple_api.py                   # Flask REST API (HTTP server for Android)
├── requirements.txt                # Python dependencies
├── API_DOCUMENTATION.md            # Complete API docs for Android developers
├── ALIGNMENT_CHECKLIST.md          # Modelfile ↔ Python alignment verification
├── WiFiClassifier.java             # Android reference: classify WiFi measurements
├── WiFiRecommendationEngine.java   # Android reference: decision-making logic
├── ANDROID_QUICK_START.md          # Quick start guide for Android developers
└── README.md                       # This file
```

## Architecture

The system uses a simple **HTTP REST API** for communication between the Android app and Raspberry Pi:

```
Android App → HTTP → Raspberry Pi (Flask) → Ollama → Qwen 3
            WiFi    simple_api.py                   (LLM)

Android ← JSON Response ← Flask API ← Explanation Text
```

**Why HTTP?**
- ✅ Simple, standard REST API
- ✅ Both devices on same WiFi network
- ✅ Easy debugging and testing
- ✅ Well-documented, mature tooling
- ✅ Works with any HTTP client library

## Prerequisites

### For Raspberry Pi (Server)
- Raspberry Pi 4 (4GB+ RAM) or Raspberry Pi 5
- Raspbian/Raspberry Pi OS (64-bit recommended)
- Python 3.7+
- Ollama installed ([ARM64 support](https://ollama.ai))
- Qwen 3 model (0.6B - optimized for local inference on Pi)

### For Android App (Client)
- Android 5.0+ (API level 21+)
- Network permissions
- Location permissions (for WiFi scanning)

## Setup

### Raspberry Pi Setup

#### 1. Install Ollama and Pull Base Model

```bash
# Update system
sudo apt update && sudo apt upgrade -y

# Install Ollama (ARM64 support for Raspberry Pi)
curl https://ollama.ai/install.sh | sh

# Start Ollama server
ollama serve &

# Pull the Qwen 3 model (0.6B recommended for Pi, or 1.7B if you have 8GB RAM)
ollama pull qwen3:0.6b
# OR for larger model: ollama pull qwen3:1.7b
```

#### 2. Clone Repository and Create Custom Model

```bash
# Clone the repository
cd ~
git clone https://github.com/bennett-lahn/hifi-wifi.git Hi-FI
cd Hi-FI
git checkout qwen-3-0.5

# Create the custom wifi-assistant model from Modelfile
cd models
ollama create wifi-assistant -f Modelfile

# Verify the model is created
ollama list
# You should see "wifi-assistant" in the list

# Test the model quickly
cd ..
python3 test_ollama_direct.py
```

#### 3. Install Python Dependencies

```bash
# Install pip if not already installed
sudo apt install python3-pip -y

# Install required packages
pip3 install -r requirements.txt
```

#### 4. Start Services

**Option A: Direct Python service (for testing)**
```bash
# Start Ollama server
ollama serve &

# Test the Python service
python3 ollama_service.py
```

**Option B: Flask REST API (recommended for Android app)**
```bash
# Start Ollama server (if not running)
ollama serve &

# Start Flask API server
python3 simple_api.py
```

The Flask API will be available at:
- Local: `http://localhost:5000`
- Network: `http://<raspberry-pi-ip>:5000`

**Find your Raspberry Pi IP address:**
```bash
hostname -I
# Example output: 192.168.1.100
```

#### 5. Connect from Android Phone Over WiFi

**Prerequisites:**
- Raspberry Pi and Android phone must be on the same WiFi network
- Note your Pi's IP address (from `hostname -I`)

**Test connection from phone:**

1. **Quick browser test** (verify Pi is accessible):
   - Open browser on phone
   - Visit: `http://192.168.1.100:5000/health` (replace with your Pi's IP)
   - Should see: `{"status":"healthy","ollama_available":true,...}`

2. **Test with REST client app** (optional):
   - Install "HTTP Request Maker" or similar from Play Store
   - Create POST request to: `http://192.168.1.100:5000/analyze`
   - Headers: `Content-Type: application/json`
   - Body (classification-only format):
   ```json
   {
     "location": "living_room",
     "signal_strength": "excellent",
     "latency": "excellent",
     "bandwidth": "good",
     "jitter": "excellent",
     "packet_loss": "excellent",
     "frequency": "5GHz",
     "activity": "gaming"
   }
   ```
   - Send and wait 10-30 seconds for response

**Available REST Endpoints:**

- `GET /health` - Check if service is running
  ```bash
  curl http://192.168.1.100:5000/health
  ```

- `POST /analyze` - Analyze WiFi with classifications (Android app uses this)
  ```bash
  curl -X POST http://192.168.1.100:5000/analyze \
    -H "Content-Type: application/json" \
    -d '{
      "location": "bedroom",
      "signal_strength": "good",
      "latency": "good",
      "bandwidth": "okay",
      "jitter": "good",
      "packet_loss": "good",
      "frequency": "2.4GHz",
      "activity": "video_call"
    }'
  ```

- `POST /chat` - Natural language queries
  ```bash
  curl -X POST http://192.168.1.100:5000/chat \
    -H "Content-Type: application/json" \
    -d '{"query": "What signal strength is good for gaming?"}'
  ```

**Response format (from /analyze):**
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
      "latency_ms": 8,
      "jitter_ms": 2,
      "packet_loss_percent": 0.02
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

**Troubleshooting WiFi connection:**
- Ensure firewall allows port 5000 on Pi (usually no firewall by default)
- Check both devices show same WiFi network SSID
- Try Pi's IP with `ping 192.168.1.100` from phone terminal app
- If using mobile hotspot, some block device-to-device communication
- Check Flask is running: `curl http://localhost:5000/health` on Pi

#### 6. Optimize Ollama Performance (IMPORTANT - 2-4x Speedup!)

**Enable KV cache optimizations for massive speedup on Raspberry Pi:**

```bash
# Edit Ollama systemd service
sudo nano /etc/systemd/system/ollama.service
```

Replace the entire file with:
```ini
[Unit]
Description=Ollama Service with KV Cache Optimizations
After=network.target

[Service]
Type=simple
User=pi
Environment="OLLAMA_KEEP_ALIVE=-1"
Environment="OLLAMA_FLASH_ATTENTION=1"
Environment="OLLAMA_KV_CACHE_TYPE=q8_0"
Environment="OLLAMA_NUM_PARALLEL=2"
ExecStart=/usr/local/bin/ollama serve
Restart=on-failure

[Install]
WantedBy=multi-user.target
```

**Apply the optimizations:**
```bash
sudo systemctl daemon-reload
sudo systemctl enable ollama
sudo systemctl restart ollama
sudo systemctl status ollama
```

**What these optimizations do:**
- `OLLAMA_KEEP_ALIVE=-1`: Keep model in RAM (no reload delay, saves 3-5s per request)
- `OLLAMA_FLASH_ATTENTION=1`: 2-3x faster inference
- `OLLAMA_KV_CACHE_TYPE=q8_0`: 50% less memory, stores system prompt (instant reuse)
- `OLLAMA_NUM_PARALLEL=2`: Handle 2 requests simultaneously

**Performance impact:**
- Before: 12-20s per request
- After: 3-8s per request (2-4x faster!)
- KV cache persists across restarts ✅

**Note:** Settings are saved permanently. Tomorrow when you start the Pi, Ollama will automatically run with optimizations.

#### 7. Set Up Flask API Service (Production)

```bash
# Create Flask API service
sudo nano /etc/systemd/system/wifi-api.service
```

Add:
```ini
[Unit]
Description=WiFi Optimization API
After=network.target ollama.service
Requires=ollama.service

[Service]
Type=simple
User=pi
WorkingDirectory=/home/pi/Hi-FI
ExecStart=/usr/bin/python3 simple_api.py
Restart=on-failure

[Install]
WantedBy=multi-user.target
```

```bash
# Enable and start services
sudo systemctl enable ollama wifi-api
sudo systemctl start ollama wifi-api

# Check status
sudo systemctl status ollama
sudo systemctl status wifi-api
```

## Usage

### Python API Interface

```python
from ollama_service import OllamaService, OllamaConfig

# Initialize the service
config = OllamaConfig(
    base_url="http://localhost:11434",
    model_name="wifi-assistant",
    timeout=30
)
service = OllamaService(config)

# Analyze WiFi measurement
measurement = {
    "location": "living_room",
    "signal_dbm": -45,
    "link_speed_mbps": 866,
    "latency_ms": 12,
    "frequency": "5GHz",
    "activity": "gaming"
}

result = service.analyze_wifi_measurement(measurement)
print(result)

# Natural language query
response = service.chat_query("How can I improve my WiFi signal?")
print(response['response'])
```

### Run Example Script

```bash
# Run the example usage script
python ollama_service.py
```

### Android App Integration

For complete Android integration details, see [API_DOCUMENTATION.md](./API_DOCUMENTATION.md).

#### Quick Start for Android Developers

**1. Required Permissions (`AndroidManifest.xml`):**
```xml
<uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.INTERNET" />
```

**2. Collect and Classify WiFi Data:**
```java
WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
WifiInfo wifiInfo = wifiManager.getConnectionInfo();

// Collect raw measurements
int rssi = wifiInfo.getRssi();
int linkSpeed = wifiInfo.getLinkSpeed();
int latency = measureLatency();
int jitter = measureJitter();
float packetLoss = measurePacketLoss();

// Classify using WiFiClassifier (see WiFiClassifier.java)
JSONObject measurement = new JSONObject();
measurement.put("location", "living_room");
measurement.put("signal_strength", WiFiClassifier.classifySignalStrength(rssi));
measurement.put("latency", WiFiClassifier.classifyLatency(latency));
measurement.put("bandwidth", WiFiClassifier.classifyBandwidth(linkSpeed));
measurement.put("jitter", WiFiClassifier.classifyJitter(jitter));
measurement.put("packet_loss", WiFiClassifier.classifyPacketLoss(packetLoss));
measurement.put("frequency", wifiInfo.getFrequency() > 4000 ? "5GHz" : "2.4GHz");
measurement.put("activity", "gaming");
```

**3. Send to Raspberry Pi API:**
```java
// Using Retrofit
POST http://192.168.1.100:5000/analyze
Content-Type: application/json

{
  "location": "living_room",
  "signal_strength": "good",
  "latency": "good",
  "bandwidth": "good",
  "jitter": "okay",
  "packet_loss": "good",
  "frequency": "2.4GHz",
  "activity": "gaming"
}
```

**4. Display Results:**
```java
// Response includes:
// - recommendation.action: "stay_current", "move_location", "switch_band"
// - recommendation.message: User-friendly explanation
// - analysis.signal_rating: 0-10 rating
// - analysis.suitable_for_activity: true/false
```

#### WiFi Data Collection Reliability

All WiFi parameters come from **native Android APIs and network tests**:

| Parameter | Android API/Method | Availability | Typical Range |
|-----------|-------------------|--------------|---------------|
| Signal Strength (RSSI) | `getRssi()` | ✅ All versions | -30 to -90 dBm |
| Link Speed | `getLinkSpeed()` | ✅ All versions | 10-866+ Mbps |
| Frequency Band | `getFrequency()` | ✅ Android 5.0+ | 2437 MHz, 5180 MHz |
| Latency | HTTP ping test | ✅ All versions | 1-1000+ ms |
| Jitter | Network variability test | ✅ All versions | 1-100+ ms |
| Packet Loss | Network reliability test | ✅ All versions | 0-10% |

**Total collection time:** ~5-10 seconds per measurement (includes network tests)

See [API_DOCUMENTATION.md](./API_DOCUMENTATION.md) for complete implementation examples.

## WiFi Measurement Input Format

Android app classifies measurements and sends classifications (not raw numbers):

```json
{
  "location": "living_room",
  "signal_strength": "excellent",
  "latency": "excellent",
  "bandwidth": "excellent",
  "jitter": "excellent",
  "packet_loss": "excellent",
  "frequency": "5GHz",
  "activity": "gaming"
}
```

**Classification Values:** `excellent`, `good`, `okay`, `bad`, `marginal`

## Expected Output Format

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
      "latency_ms": 8,
      "jitter_ms": 2,
      "packet_loss_percent": 0.02
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

### Action Types
- `stay_current` - Current connection is optimal
- `move_location` - Move to a different location for better signal
- `switch_band` - Switch between 2.4GHz and 5GHz

### Quality Levels (5-Level Classification)
- `excellent` - Best performance (score: 5)
- `good` - Above average (score: 4)
- `okay` - Acceptable (score: 3)
- `bad` - Below average (score: 2)
- `marginal` - Poor performance (score: 1)

### Classification Thresholds

**Signal Strength (RSSI):**
- Excellent: ≥ -30 dBm
- Good: -30 to -50 dBm
- Okay: -50 to -65 dBm
- Bad: -65 to -80 dBm
- Marginal: < -80 dBm

**Latency:**
- Excellent: ≤ 20ms
- Good: 21-50ms
- Okay: 51-100ms
- Bad: 101-200ms
- Marginal: > 200ms

**Bandwidth:**
- Excellent: ≥ 100 Mbps
- Good: 50-99 Mbps
- Okay: 25-49 Mbps
- Bad: 10-24 Mbps
- Marginal: < 10 Mbps

**Jitter:**
- Excellent: ≤ 5ms
- Good: 6-10ms
- Okay: 11-20ms
- Bad: 21-50ms
- Marginal: > 50ms

**Packet Loss:**
- Excellent: ≤ 0.1%
- Good: 0.1-0.5%
- Okay: 0.5-1.0%
- Bad: 1.0-2.0%
- Marginal: > 2.0%

### Bottleneck Types
- `signal_strength` - Weak WiFi signal
- `latency` - High ping/delay
- `bandwidth` - Speed limitations
- `jitter` - Inconsistent connection
- `packet_loss` - Data loss issues
- `none` - No bottlenecks detected

### Activity Requirements
- **Gaming:** Needs ≤20ms latency, ≤5ms jitter, ≤0.5% packet loss
- **Video Calls:** Needs ≤50ms latency, ≤10ms jitter, ≥25 Mbps bandwidth
- **Streaming:** Needs ≥50 Mbps bandwidth, ≤1% packet loss
- **Work:** Balanced requirements across all metrics
- **General Browsing:** Needs ≥25 Mbps bandwidth
- **IoT:** Needs ≥-65 dBm signal strength, ≤1% packet loss

## Features

- **Clean HTTP API Interface**: Direct communication with Ollama API
- **WiFi Measurement Analysis**: Get optimization recommendations based on signal data
- **Natural Language Queries**: Ask questions about WiFi optimization
- **Error Handling**: Graceful handling of connection issues, timeouts, and parsing errors
- **Type Hints**: Full type annotations for better IDE support
- **Retry Logic**: Automatic retry on connection failures
- **Health Checks**: Verify Ollama server accessibility

## Testing

### Test Flask API from Command Line

```bash
# Health check
curl http://localhost:5000/health

# Test the /explain endpoint (used by Android app)
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

# Test from another device on network (replace with Pi's IP)
curl -X POST http://192.168.1.100:5000/explain \
  -H "Content-Type: application/json" \
  -d '{
    "location": "bedroom",
    "activity": "video_call",
    "measurements": {
      "signal_strength": "poor",
      "latency": "fair",
      "bandwidth": "fair"
    },
    "recommendation": {
      "action": "move_location",
      "target_location": "closer to router"
    }
  }'
```

### Test from iPhone or Android Device

**1. Find your Raspberry Pi's IP address:**
```bash
# On Raspberry Pi
hostname -I
# Example: 192.168.1.100
```

**2. Quick browser test:**
- Open Safari/Chrome on your phone
- Visit: `http://192.168.1.100:5000/health`
- You should see: `{"status":"healthy",...}`

**3. Full API test with REST client:**
- Install "HTTP Request" or "API Tester" app (free on App Store/Play Store)
- Create POST request to: `http://192.168.1.100:5000/explain`
- Add header: `Content-Type: application/json`
- Body:
```json
{
  "location": "living_room",
  "activity": "gaming",
  "measurements": {
    "signal_strength": "excellent",
    "latency": "excellent",
    "bandwidth": "excellent"
  },
  "recommendation": {"action": "stay_current"}
}
```
- Send and wait 5-15 seconds for Qwen's response

**Note:** If port 5000 is in use (macOS AirPlay), run Flask on different port:
```bash
python3 -c "from simple_api import app; app.run(host='0.0.0.0', port=5001)"
```

### Performance Benchmarks on Raspberry Pi

**Raspberry Pi 4 (4GB):**
- Model loading: ~5-10 seconds (one-time)
- Inference time: ~3-5 seconds per request
- Total API response: ~4-6 seconds
- Memory usage: ~1.5-2GB

**Raspberry Pi 5:**
- Model loading: ~3-5 seconds
- Inference time: ~1-3 seconds per request
- Total API response: ~2-4 seconds
- Memory usage: ~1.5-2GB

## Troubleshooting

### Raspberry Pi Issues

**Ollama server not accessible:**
```bash
# Check if Ollama is running
curl http://localhost:11434/api/tags

# Check process
ps aux | grep ollama

# Start Ollama server
ollama serve

# Check logs
journalctl -u ollama -f
```

**Model not found:**
```bash
# List available models
ollama list

# Create the wifi-assistant model
cd /home/pi/Hi-FI/models/
ollama create wifi-assistant -f Modelfile

# Test the model directly
ollama run wifi-assistant "Test message"
```

**Flask API not accessible from Android:**
```bash
# Check if Flask is running
curl http://localhost:5000/health

# Check firewall (Raspberry Pi usually has no firewall by default)
sudo iptables -L

# Find your Pi's IP address
hostname -I

# Test from Pi itself
curl http://localhost:5000/health

# Test from another device on same network
curl http://192.168.1.100:5000/health
```

**Out of memory on Raspberry Pi:**
```bash
# Check memory usage
free -h

# Check swap
sudo dphys-swapfile swapoff
sudo nano /etc/dphys-swapfile
# Set CONF_SWAPSIZE=2048 (2GB swap)
sudo dphys-swapfile setup
sudo dphys-swapfile swapon

# Restart services
sudo systemctl restart ollama wifi-api
```

### Android App Issues

**Connection timeout:**
- Verify Raspberry Pi IP address hasn't changed
- Check that Android device is on same WiFi network
- Increase timeout in Retrofit configuration
- Check Raspberry Pi firewall settings

**Invalid response format:**
- Verify Flask API is running (`curl http://pi-ip:5000/health`)
- Check API logs on Raspberry Pi (`journalctl -u wifi-api -f`)
- Ensure JSON fields match API specification

**WiFi data collection fails:**
- Verify all required permissions are granted
- Check that device is connected to WiFi
- Handle cases where `getFrequency()` returns 0 (older devices)

## Development

### Testing Different Scenarios

Add test scenarios in `test-scenarios/` directory with various WiFi conditions:
- Good signal (> -50 dBm)
- Fair signal (-50 to -70 dBm)
- Poor signal (< -70 dBm)
- Different activities (gaming, video calls, browsing)

### Fine-tuning

For Phase 2, add synthetic training data in `training-data/` to improve model accuracy for WiFi optimization tasks.

## Contributing

This project is being developed on the `qwen-3-0.5` branch for AI models and inference training work.

## License

[Add license information]
