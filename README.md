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
├── simple_api.py                   # Flask REST API wrapper (for Android)
├── requirements.txt                # Python dependencies
├── API_DOCUMENTATION.md            # Complete API docs for Android developers
├── ALIGNMENT_CHECKLIST.md          # Modelfile ↔ Python alignment verification
└── README.md                       # This file
```

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

#### 1. Install Ollama and Model

```bash
# Update system
sudo apt update && sudo apt upgrade -y

# Install Ollama (ARM64 support for Raspberry Pi)
curl https://ollama.ai/install.sh | sh

# Pull the Qwen 3 model (0.6B - optimized for Pi)
ollama pull qwen3:0.6b
```

#### 2. Create Custom WiFi Assistant Model

```bash
# Clone or copy this repository to your Raspberry Pi
cd /home/pi/Hi-FI/models/

# Create the custom model from Modelfile
ollama create wifi-assistant -f Modelfile

# Verify the model is created
ollama list
# You should see "wifi-assistant" in the list
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

#### 5. Keep Services Running (Production)

Create systemd services to auto-start on boot:

```bash
# Create Ollama service
sudo nano /etc/systemd/system/ollama.service
```

Add:
```ini
[Unit]
Description=Ollama Service
After=network.target

[Service]
Type=simple
User=pi
ExecStart=/usr/local/bin/ollama serve
Restart=on-failure

[Install]
WantedBy=multi-user.target
```

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

**2. Collect WiFi Data:**
```java
WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
WifiInfo wifiInfo = wifiManager.getConnectionInfo();

JSONObject measurement = new JSONObject();
measurement.put("location", "living_room");
measurement.put("signal_dbm", wifiInfo.getRssi());           // Native API
measurement.put("link_speed_mbps", wifiInfo.getLinkSpeed()); // Native API
measurement.put("frequency", wifiInfo.getFrequency() > 4000 ? "5GHz" : "2.4GHz");
measurement.put("latency_ms", measureLatency());             // HTTP ping test
measurement.put("activity", "gaming");
```

**3. Send to Raspberry Pi API:**
```java
// Using Retrofit
POST http://192.168.1.100:5000/analyze
Content-Type: application/json

{
  "location": "living_room",
  "signal_dbm": -65,
  "link_speed_mbps": 144,
  "latency_ms": 25,
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

All WiFi parameters come from **native Android APIs** (100% reliable):

| Parameter | Android API | Availability | Typical Range |
|-----------|-------------|--------------|---------------|
| Signal Strength (RSSI) | `getRssi()` | ✅ All versions | -30 to -90 dBm |
| Link Speed | `getLinkSpeed()` | ✅ All versions | 1-866+ Mbps |
| Frequency Band | `getFrequency()` | ✅ Android 5.0+ | 2437 MHz, 5180 MHz |
| Latency | HTTP ping test | ✅ All versions | 1-1000+ ms |

**Total collection time:** ~1-2 seconds per measurement

See [API_DOCUMENTATION.md](./API_DOCUMENTATION.md) for complete implementation examples.

## WiFi Measurement Input Format

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

### Action Types
- `stay_current` - Current connection is optimal
- `move_location` - Move to a different location for better signal
- `switch_band` - Switch between 2.4GHz and 5GHz

### Quality Levels
- `excellent` - Signal > -50 dBm
- `good` - Signal -50 to -60 dBm
- `moderate` - Signal -60 to -70 dBm
- `poor` - Signal -70 to -80 dBm
- `very_poor` - Signal < -80 dBm

### Bottleneck Types
- `signal_strength` - Weak WiFi signal
- `latency` - High ping/delay
- `bandwidth` - Speed limitations
- `none` - No bottlenecks detected

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

# Test WiFi analysis
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

# Test from another device on network (replace with Pi's IP)
curl -X POST http://192.168.1.100:5000/analyze \
  -H "Content-Type: application/json" \
  -d '{
    "location": "bedroom",
    "signal_dbm": -78,
    "link_speed_mbps": 65,
    "latency_ms": 52,
    "frequency": "2.4GHz",
    "activity": "video_call"
  }'
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
