# Custom Network Speed Test Implementation

This package contains a simple, custom network speed test implementation for Android using Java only (no Kotlin). It's designed for a hackathon demo app that measures Wi-Fi speed while walking through rooms.

## Features

- **SimpleSpeedTest.java**: Core speed test implementation using HttpURLConnection
- **SpeedTestWorker.java**: WorkManager integration for background execution
- **SpeedTestManager.java**: High-level manager for easy integration
- **SpeedTestResult.java**: Data model for storing test results
- **SpeedTestExample.java**: Usage examples

## Key Features

- ✅ Downloads test data from public URLs (Cloudflare, ThinkBroadband)
- ✅ Measures download speed in Mbps (rounded to 2 decimal places)
- ✅ **Measures network latency** using ping-like approach
- ✅ **Calculates jitter** from latency measurements (standard deviation)
- ✅ **Measures packet loss** percentage using multiple ping tests
- ✅ 10-second maximum test duration to avoid excessive data usage
- ✅ AsyncTask for background execution
- ✅ Callbacks for: onComplete (with all metrics), onError, and onProgress
- ✅ Proper error handling for network timeouts and connectivity issues
- ✅ Thread-safe and suitable for periodic execution
- ✅ WorkManager integration for background tasks

## Usage

### Basic Usage

```java
// Get the manager instance
SpeedTestManager manager = SpeedTestManager.getInstance(context);

// Start a background speed test
String testId = manager.startSpeedTest("Living Room");

// Start an immediate speed test with callbacks
manager.startSimpleSpeedTest("Kitchen", new SimpleSpeedTest.SpeedTestCallback() {
    @Override
    public void onComplete(double speedMbps, int latencyMs, double jitterMs, double packetLossPercent) {
        Log.d("SpeedTest", "Speed: " + speedMbps + " Mbps, Latency: " + latencyMs + "ms, " +
              "Jitter: " + jitterMs + "ms, Packet Loss: " + packetLossPercent + "%");
    }
    
    @Override
    public void onError(String errorMessage) {
        Log.e("SpeedTest", "Error: " + errorMessage);
    }
    
    @Override
    public void onProgress(double currentSpeedMbps, long bytesDownloaded, long totalBytes) {
        // Update progress bar
    }
});
```

### Getting Results

```java
// Get all results
List<SpeedTestResult> allResults = manager.getAllResults();

// Get results for a specific room
List<SpeedTestResult> roomResults = manager.getResultsForRoom("Living Room");

// Get average speed for a room
double averageSpeed = manager.getAverageSpeedForRoom("Living Room");
```

### Setting Up Callbacks

```java
manager.addCallback(new SpeedTestManager.SpeedTestCallback() {
    @Override
    public void onTestStarted(String testId, String roomLabel) {
        // Test started
    }
    
    @Override
    public void onTestCompleted(String testId, SpeedTestResult result) {
        // Test completed successfully
    }
    
    @Override
    public void onTestFailed(String testId, String errorMessage) {
        // Test failed
    }
});
```

## Configuration

### Test URLs
- Primary: `https://speed.cloudflare.com/__down?bytes=10000000` (10MB)
- Fallback: `http://ipv4.download.thinkbroadband.com/10MB.zip`

### Timeouts
- Connection timeout: 10 seconds
- Read timeout: 10 seconds
- Maximum test duration: 10 seconds

### Buffer Size
- 8192 bytes for reading data chunks

## Speed Calculation

The speed is calculated using the formula:
```
speedMbps = ((totalBytesDownloaded * 8) / 1,000,000) / durationInSeconds
```

Results are rounded to 2 decimal places.

## Permissions Required

The following permissions are already included in AndroidManifest.xml:
- `INTERNET` - Required for network access
- `ACCESS_NETWORK_STATE` - Required for checking network connectivity

## Dependencies

The implementation requires:
- `androidx.work:work-runtime:2.8.1` - For WorkManager integration
- Standard Android SDK (API 29+)

## Error Handling

The implementation handles:
- Network timeouts (10-second connection/read timeout)
- IOException handling
- No network connectivity cases
- HTTP error responses
- Test cancellation

## Thread Safety

All classes are designed to be thread-safe:
- `SpeedTestManager` uses `ConcurrentHashMap` for result storage
- Callbacks are synchronized
- WorkManager handles background execution safely

## Testing

Unit tests are included in `SimpleSpeedTestTest.java` using Robolectric for Android testing without requiring a device or emulator.

## Example Integration

See `SpeedTestExample.java` for complete usage examples showing how to integrate the speed test with your app's UI and data storage.

## Notes

- This implementation prioritizes simplicity and clarity over optimization
- Suitable for hackathon demos and prototyping
- All code is written in Java (no Kotlin)
- Designed for periodic execution via WorkManager
- Results are stored in memory (consider adding database persistence for production use)
