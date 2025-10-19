package com.example.hifiwifi.services;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.example.hifiwifi.models.NetworkMetrics;
import com.example.hifiwifi.models.RoomMeasurement;
import com.example.hifiwifi.classifier.WiFiClassifier;
import com.example.hifiwifi.classifier.WiFiClassification;
import com.example.hifiwifi.classifier.ClassificationResult;
import com.example.hifiwifi.classifier.MetricClassification;
import com.example.hifiwifi.classifier.ActivityImportance;
import com.example.hifiwifi.classifier.ActivityImportanceFactory;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * Service for measuring WiFi performance metrics
 * Uses custom simple speed test implementation with HTTP/HTTPS support
 */
public class WiFiMeasurementService {
    private static final String TAG = "WiFiMeasurementService";
    
    // Speed test configuration
    private static final int CONNECTION_TIMEOUT = 5000; // 5 seconds for speed tests
    private static final int READ_TIMEOUT = 5000; // 5 seconds for speed tests
    private static final int PING_TIMEOUT = 2000; // 2 seconds for ping tests
    
    // Speed test URLs - prioritizing working servers
    private static final String[] testUrls = {
        "http://ipv4.download.thinkbroadband.com/10MB.zip",  // HTTP - Most reliable, tested working
        "https://speedtest.tele2.net/10MB.zip",              // HTTPS fallback
        "https://speedtest.tele2.net/1MB.zip",               // HTTPS - Smaller file
        "http://speedtest.ftp.otenet.gr/files/test10Mb.db",  // HTTP fallback - Greek server
        "http://proof.ovh.net/files/10Mb.dat"                // HTTP fallback - OVH (French)
    };
    
    // Single test URLs - prioritizing working servers
    private static final String[] singleTestUrls = {
        "http://ipv4.download.thinkbroadband.com/10MB.zip",  // HTTP - Most reliable, tested working
        "https://speedtest.tele2.net/10MB.zip",              // HTTPS fallback
        "https://speedtest.tele2.net/1MB.zip",               // HTTPS - Smaller file
        "http://speedtest.ftp.otenet.gr/files/test10Mb.db",  // HTTP fallback - Greek server
        "http://proof.ovh.net/files/10Mb.dat"                // HTTP fallback - OVH
    };
    
    private Context context;
    private WifiManager wifiManager;
    private ExecutorService executorService;
    private Handler mainHandler;
    
    // Callback interfaces
    public interface MeasurementCallback {
        void onMeasurementUpdate(NetworkMetrics metrics);
        void onMeasurementComplete(RoomMeasurement measurement);
        void onClassificationComplete(ClassificationResult classificationResult);
        void onError(String error);
    }
    
    private MeasurementCallback callback;
    private boolean isMeasuring = false;
    private String currentRoomName = "";
    private String currentActivityType = "general";
    
    // WiFi Classification components
    private WiFiClassifier classifier;
    private ActivityImportanceFactory importanceFactory;
    
    // Jitter measurement variables
    private List<Long> latencyMeasurements = new ArrayList<>();
    private static final int JITTER_SAMPLE_SIZE = 10;
    
    public WiFiMeasurementService(Context context) {
        this.context = context;
        this.wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        this.executorService = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.classifier = new WiFiClassifier();
        this.importanceFactory = new ActivityImportanceFactory();
    }
    
    public void setCallback(MeasurementCallback callback) {
        this.callback = callback;
    }
    
    /**
     * Start continuous WiFi measurement for a specific room
     */
    public void startMeasurement(String roomName) {
        startMeasurement(roomName, "general");
    }
    
    /**
     * Start continuous WiFi measurement for a specific room with activity type
     * This method is for continuous monitoring, not single tests
     */
    public void startMeasurement(String roomName, String activityType) {
        Log.d(TAG, "Starting continuous measurement for room: " + roomName + ", activity: " + activityType);
        
        if (isMeasuring) {
            Log.w(TAG, "Measurement already in progress, stopping previous measurement");
            stopMeasurement();
        }
        
        isMeasuring = true;
        currentRoomName = roomName;
        currentActivityType = activityType;
        
        // Start measurement loop
        executorService.execute(this::measurementLoop);
    }
    
    /**
     * Start a single speed test for a specific room
     */
    public void startSingleSpeedTest(String roomName, String activityType) {
        Log.d(TAG, "Starting single speed test for room: " + roomName + ", activity: " + activityType);
        
        if (isMeasuring) {
            Log.w(TAG, "Measurement already in progress, stopping previous measurement");
            stopMeasurement();
        }
        
        isMeasuring = true;
        currentRoomName = roomName;
        currentActivityType = activityType;
        
        // Start single test
        executorService.execute(this::singleSpeedTest);
    }
    
    /**
     * Stop current measurement
     */
    public void stopMeasurement() {
        Log.d(TAG, "Stopping measurement");
        isMeasuring = false;
            // The isMeasuring flag will prevent new measurements
        // Any ongoing tests will complete naturally
    }
    
    /**
     * Get current signal strength in dBm
     */
    public int getCurrentSignalStrength() {
        if (wifiManager != null && wifiManager.getConnectionInfo() != null) {
            return wifiManager.getConnectionInfo().getRssi();
        }
        return -100; // Default poor signal
    }
    
    /**
     * Measure network latency using simple ping approach
     */
    public int measureLatency() {
        Log.d(TAG, "Starting latency measurement");
        int totalPings = 25;
        long totalLatency = 0;
        int successfulPings = 0;
        
        for (int i = 0; i < totalPings; i++) {
            try {
                long startTime = System.currentTimeMillis();
                HttpURLConnection connection = setupConnection("https://www.google.com");
                connection.setConnectTimeout(PING_TIMEOUT);
                connection.setReadTimeout(PING_TIMEOUT);
                connection.connect();
                int responseCode = connection.getResponseCode();
                connection.disconnect();
                
                if (responseCode == 200) {
                    long latency = System.currentTimeMillis() - startTime;
                    totalLatency += latency;
                    successfulPings++;
                    Log.v(TAG, "Ping " + (i+1) + " successful, latency: " + latency + "ms");
                } else {
                    Log.w(TAG, "Ping " + (i+1) + " failed with response code: " + responseCode);
                }
            } catch (Exception e) {
                Log.w(TAG, "Ping " + (i+1) + " failed with exception: " + e.getMessage());
            }
        }
        
        if (successfulPings > 0) {
            int averageLatency = (int) (totalLatency / successfulPings);
            Log.d(TAG, "Latency measurement complete - average: " + averageLatency + "ms");
            return averageLatency;
        } else {
            Log.w(TAG, "All latency pings failed, using default value");
            return 50; // Default fallback
        }
    }
    
    /**
     * Calculate jitter from recent latency measurements
     * Jitter is the standard deviation of latency, excluding extreme outliers
     */
    public double calculateJitter() {
        if (latencyMeasurements.size() < 2) {
            return 0.0;
        }
        
        // First pass: Calculate mean
        double sum = 0.0;
        for (Long latency : latencyMeasurements) {
            sum += latency;
        }
        double mean = sum / latencyMeasurements.size();
        
        // Filter out extreme outliers (values > 3x the mean)
        List<Long> filteredMeasurements = new ArrayList<>();
        double outlierThreshold = mean * 3.0;
        for (Long latency : latencyMeasurements) {
            if (latency <= outlierThreshold) {
                filteredMeasurements.add(latency);
            } else {
                Log.d(TAG, "Filtering outlier latency: " + latency + "ms (threshold: " + outlierThreshold + "ms)");
            }
        }
        
        // If we filtered too many, use original
        if (filteredMeasurements.size() < 2) {
            filteredMeasurements = new ArrayList<>(latencyMeasurements);
        }
        
        // Recalculate mean without outliers
        sum = 0.0;
        for (Long latency : filteredMeasurements) {
            sum += latency;
        }
        mean = sum / filteredMeasurements.size();
        
        // Calculate standard deviation (jitter)
        double sumSquaredDiff = 0.0;
        for (Long latency : filteredMeasurements) {
            double diff = latency - mean;
            sumSquaredDiff += diff * diff;
        }
        
        double variance = sumSquaredDiff / filteredMeasurements.size();
        double jitter = Math.sqrt(variance);
        
        Log.d(TAG, "Jitter calculated: " + jitter + "ms from " + filteredMeasurements.size() + " samples (filtered " + (latencyMeasurements.size() - filteredMeasurements.size()) + " outliers)");
        
        return jitter;
    }
    
    /**
     * Measure packet loss using ping-like approach
     * This is a simplified implementation - in production, you might want to use a more sophisticated method
     */
    public double measurePacketLoss() {
        Log.d(TAG, "Starting packet loss measurement");
        int totalPings = 25;
        int successfulPings = 0;
        
        for (int i = 0; i < totalPings; i++) {
            try {
                long startTime = System.currentTimeMillis();
                HttpURLConnection connection = setupConnection("https://www.google.com");
                connection.setConnectTimeout(PING_TIMEOUT);
                connection.setReadTimeout(PING_TIMEOUT);
                connection.connect();
                int responseCode = connection.getResponseCode();
                connection.disconnect();
                
                if (responseCode == 200) {
                    successfulPings++;
                    // Record latency for jitter calculation
                    long latency = System.currentTimeMillis() - startTime;
                    latencyMeasurements.add(latency);
                    
                    // Keep only recent measurements for jitter calculation
                    if (latencyMeasurements.size() > JITTER_SAMPLE_SIZE) {
                        latencyMeasurements.remove(0);
                    }
                    Log.v(TAG, "Ping " + (i+1) + " successful, latency: " + latency + "ms");
                } else {
                    Log.w(TAG, "Ping " + (i+1) + " failed with response code: " + responseCode);
                }
            } catch (Exception e) {
                Log.w(TAG, "Ping " + (i+1) + " failed with exception: " + e.getMessage());
                // Ping failed - count as packet loss
            }
        }
        
        double packetLossPercent = ((totalPings - successfulPings) * 100.0) / totalPings;
        Log.d(TAG, "Packet loss measurement complete - " + successfulPings + "/" + totalPings + " successful (" + packetLossPercent + "% loss)");
        return packetLossPercent;
    }
    
    /**
     * Measure bandwidth using simple HTTP download test
     */
    public void measureBandwidth(MeasurementCallback callback) {
        Log.d(TAG, "Starting simple bandwidth measurement for room: " + currentRoomName);
        
        // Try multiple speed test servers (ordered by reliability, HTTPS preferred)
        String[] testUrls = {
            "https://speedtest.tele2.net/10MB.zip",              // HTTPS - Very reliable
            "https://speedtest.tele2.net/1MB.zip",               // HTTPS - Smaller file
            "http://ipv4.download.thinkbroadband.com/10MB.zip",  // HTTP fallback - Very reliable
            "http://speedtest.ftp.otenet.gr/files/test10Mb.db",  // HTTP fallback - Greek server
            "http://proof.ovh.net/files/10Mb.dat"                // HTTP fallback - OVH (French)
        };
        
        // Run speed test in background thread
        executorService.execute(() -> {
            performSimpleSpeedTest(testUrls, callback);
        });
    }
    
    /**
     * Perform simple speed test by downloading a file and measuring time
     * Supports both HTTP and HTTPS connections
     */
    private void performSimpleSpeedTest(String[] testUrls, MeasurementCallback callback) {
        final double[] bandwidthMbps = {0.0};
        final int[] latencyMs = {50}; // Default latency
        boolean success = false;
        
        for (String testUrl : testUrls) {
            try {
                Log.d(TAG, "Trying speed test server: " + testUrl);
                
                long startTime = System.currentTimeMillis();
                HttpURLConnection connection = setupConnection(testUrl);
                connection.setRequestMethod("GET"); // Change to GET for actual download
                
                        // Connect first
                        connection.connect();
                        int responseCode = connection.getResponseCode();
                        
                        if (responseCode == HttpURLConnection.HTTP_OK) {
                            // Start measuring download time after connection is established
                            long downloadStartTime = System.currentTimeMillis();
                            
                            // Read the file to measure download speed
                            long totalBytes = 0;
                            byte[] buffer = new byte[8192];
                            int bytesRead;
                            
                            while ((bytesRead = connection.getInputStream().read(buffer)) != -1) {
                                totalBytes += bytesRead;
                            }
                            
                            long downloadEndTime = System.currentTimeMillis();
                            long downloadDurationMs = downloadEndTime - downloadStartTime;
                            long totalDurationMs = downloadEndTime - startTime;
                            
                            if (downloadDurationMs > 0) {
                                // Calculate bandwidth in Mbps using only download time
                                double bytesPerSecond = (totalBytes * 1000.0) / downloadDurationMs;
                                bandwidthMbps[0] = (bytesPerSecond * 8) / 1000000.0; // Convert to Mbps
                                
                                // Don't update latency here - it should come from ping measurements
                                // latencyMs will be set from earlier measurements
                                
                                Log.d(TAG, "Speed test completed successfully: " + bandwidthMbps[0] + " Mbps, Download time: " + downloadDurationMs + "ms, Total time: " + totalDurationMs + "ms");
                                success = true;
                                break;
                            }
                } else {
                    Log.w(TAG, "HTTP response code: " + responseCode + " for " + testUrl);
                }
                
                connection.disconnect();
                
            } catch (Exception e) {
                Log.w(TAG, "Speed test failed for " + testUrl + ": " + e.getMessage());
                if (e.getMessage() != null && e.getMessage().contains("SSL")) {
                    Log.w(TAG, "SSL error detected, trying next server");
                }
            }
        }
        
        if (!success) {
            Log.w(TAG, "All speed test servers failed, using signal-based estimation");
            bandwidthMbps[0] = estimateBandwidthFromSignal(getCurrentSignalStrength());
        }
        
        // Calculate jitter and packet loss
        final double[] jitterMs = {calculateJitter()};
        final double[] packetLossPercent = {measurePacketLoss()};
        
        Log.d(TAG, "Final metrics - Bandwidth: " + bandwidthMbps[0] + " Mbps, Latency: " + latencyMs[0] + "ms, Jitter: " + jitterMs[0] + "ms, Packet Loss: " + packetLossPercent[0] + "%");
        
        // Update UI on main thread
                mainHandler.post(() -> {
                    if (callback != null) {
                        // Create a measurement with all network metrics
                        NetworkMetrics metrics = new NetworkMetrics(
                            getCurrentSignalStrength(),
                    latencyMs[0],
                    bandwidthMbps[0],
                    jitterMs[0],
                    packetLossPercent[0],
                            true,
                            currentRoomName
                        );
                        callback.onMeasurementUpdate(metrics);
                        
                        // Perform classification and notify callback
                        ClassificationResult classificationResult = performClassification(metrics);
                        if (classificationResult != null) {
                            callback.onClassificationComplete(classificationResult);
                        }
                    }
                });
            }
            
    /**
     * Attempt fallback measurement methods when primary speed test fails
     */
    private void attemptFallbackMeasurement(MeasurementCallback callback, String errorMessage) {
        Log.w(TAG, "Attempting fallback measurement due to error: " + errorMessage);
        
        // For HTTP errors, try alternative measurement methods
        if (errorMessage.contains("403") || errorMessage.contains("INVALID_HTTP_RESPONSE") || errorMessage.contains("HTTP")) {
            Log.w(TAG, "HTTP error detected - trying alternative measurement approach");
            
            // Use basic network connectivity test as fallback
            performBasicNetworkTest(callback);
        } else {
            // For other errors, provide basic metrics
            Log.w(TAG, "Using basic metrics due to speed test failure");
            provideBasicMetrics(callback, errorMessage);
        }
    }
    
    /**
     * Perform basic network test when speed test fails
     */
    private void performBasicNetworkTest(MeasurementCallback callback) {
        Log.d(TAG, "Performing basic network test as fallback");
        
        try {
            // Measure basic metrics
            int signalStrength = getCurrentSignalStrength();
            double packetLossPercent = measurePacketLoss();
            double jitterMs = calculateJitter();
            
            // Estimate bandwidth based on signal strength (very rough approximation)
            double estimatedBandwidth = estimateBandwidthFromSignal(signalStrength);
            
            Log.d(TAG, "Basic test results - Signal: " + signalStrength + "dBm, Estimated BW: " + estimatedBandwidth + "Mbps");
            
            NetworkMetrics metrics = new NetworkMetrics(
                signalStrength,
                50, // Default latency
                estimatedBandwidth,
                jitterMs,
                packetLossPercent,
                true,
                currentRoomName
            );
            
            mainHandler.post(() -> {
                if (callback != null) {
                    callback.onMeasurementUpdate(metrics);
                    
                    // Create a basic measurement
                    RoomMeasurement measurement = new RoomMeasurement(
                        "room_" + System.currentTimeMillis(),
                        currentRoomName,
                        signalStrength,
                        50,
                        estimatedBandwidth,
                        jitterMs,
                        packetLossPercent,
                        currentActivityType
                    );
                    callback.onMeasurementComplete(measurement);
                    
                    // Perform classification on the basic measurement
                    ClassificationResult classificationResult = performClassification(measurement);
                    if (classificationResult != null) {
                        callback.onClassificationComplete(classificationResult);
                    }
                }
            });
            
        } catch (Exception e) {
            Log.e(TAG, "Basic network test failed: " + e.getMessage());
                mainHandler.post(() -> {
                    if (callback != null) {
                    callback.onError("Network test failed: " + e.getMessage());
                    }
                });
            }
    }
    
    /**
     * Provide basic metrics when all tests fail
     */
    private void provideBasicMetrics(MeasurementCallback callback, String originalError) {
        Log.w(TAG, "Providing basic metrics due to test failure");
        
        int signalStrength = getCurrentSignalStrength();
        double estimatedBandwidth = estimateBandwidthFromSignal(signalStrength);
        
        NetworkMetrics metrics = new NetworkMetrics(
            signalStrength,
            50, // Default latency
            estimatedBandwidth,
            0.0, // No jitter data
            0.0, // No packet loss data
            true,
            currentRoomName
        );
        
        mainHandler.post(() -> {
            if (callback != null) {
                callback.onMeasurementUpdate(metrics);
                
                // Create a basic measurement
                RoomMeasurement measurement = new RoomMeasurement(
                    "room_" + System.currentTimeMillis(),
                    currentRoomName,
                    signalStrength,
                    50,
                    estimatedBandwidth,
                    0.0,
                    0.0,
                    currentActivityType
                );
                callback.onMeasurementComplete(measurement);
                
                // Perform classification on the basic measurement
                ClassificationResult classificationResult = performClassification(measurement);
                if (classificationResult != null) {
                    callback.onClassificationComplete(classificationResult);
                }
            }
        });
    }
    
    /**
     * Setup HTTP/HTTPS connection with proper headers and SSL handling
     */
    private HttpURLConnection setupConnection(String urlString) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        
        // Essential headers to avoid 403 errors
        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.120 Mobile Safari/537.36");
        connection.setRequestProperty("Accept", "*/*");
        connection.setRequestProperty("Accept-Encoding", "identity");
        connection.setRequestProperty("Connection", "keep-alive");
        
        // Additional headers for better compatibility
        connection.setRequestProperty("Accept-Language", "en-US,en;q=0.9");
        connection.setRequestProperty("Cache-Control", "no-cache");
        
        // Timeouts
        connection.setConnectTimeout(CONNECTION_TIMEOUT);
        connection.setReadTimeout(READ_TIMEOUT);
        connection.setRequestMethod("HEAD");
        connection.setDoInput(true);
        connection.setInstanceFollowRedirects(true);
        
        // Handle HTTPS with proper SSL configuration
        if (urlString.startsWith("https://")) {
            setupHttpsConnection((HttpsURLConnection) connection);
        }
        
        return connection;
    }
    
    /**
     * Setup HTTPS connection with SSL context and trust manager
     * Note: This implementation accepts all certificates for speed testing purposes
     * In production, implement proper certificate validation
     */
    private void setupHttpsConnection(HttpsURLConnection httpsConnection) {
        try {
            // Create a trust manager that accepts all certificates
            // WARNING: This is for speed testing only - in production use proper certificate validation
            TrustManager[] trustAllCerts = new TrustManager[] {
                new X509TrustManager() {
                    @Override
                    public X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[0];
                    }
                    
                    @Override
                    public void checkClientTrusted(X509Certificate[] certs, String authType) throws CertificateException {
                        // Accept all client certificates for speed testing
                        Log.v(TAG, "Accepting client certificate for speed test");
                    }
                    
                    @Override
                    public void checkServerTrusted(X509Certificate[] certs, String authType) throws CertificateException {
                        // Accept all server certificates for speed testing
                        // In production, implement proper certificate validation
                        Log.v(TAG, "Accepting server certificate for speed test");
                    }
                }
            };
            
            // Install the all-trusting trust manager
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
            httpsConnection.setSSLSocketFactory(sslContext.getSocketFactory());
            
            // Create hostname verifier that accepts all hostnames
            httpsConnection.setHostnameVerifier((hostname, session) -> {
                Log.v(TAG, "Verifying hostname: " + hostname);
                return true; // Accept all hostnames for speed testing
            });
            
            // Set additional HTTPS properties
            httpsConnection.setInstanceFollowRedirects(true);
            
            Log.d(TAG, "HTTPS connection configured with SSL context for speed testing");
            
        } catch (Exception e) {
            Log.w(TAG, "Failed to setup HTTPS connection: " + e.getMessage());
            // Continue with default HTTPS handling - Android will use system certificates
            Log.d(TAG, "Falling back to default Android HTTPS handling");
        }
    }
    
    /**
     * Estimate bandwidth based on signal strength (very rough approximation)
     */
    private double estimateBandwidthFromSignal(int signalStrength) {
        // Very rough estimation based on signal strength
        if (signalStrength >= -30) return 100.0; // Excellent signal
        if (signalStrength >= -50) return 75.0;  // Good signal
        if (signalStrength >= -70) return 50.0;  // Fair signal
        if (signalStrength >= -80) return 25.0;  // Poor signal
        return 10.0; // Very poor signal
    }
    
    /**
     * Main measurement loop that runs every 2-3 seconds
     */
    private void measurementLoop() {
        while (isMeasuring) {
            try {
                // Get current signal strength
                int signalStrength = getCurrentSignalStrength();
                
                // Measure packet loss and collect latency samples for jitter
                double packetLossPercent = measurePacketLoss();
                double jitterMs = calculateJitter();
                
                // Create current metrics with basic measurements
                NetworkMetrics metrics = new NetworkMetrics(
                    signalStrength,
                    measureLatency(), // Placeholder latency
                    0.0, // Bandwidth will be updated by speedtest
                    jitterMs,
                    packetLossPercent,
                    true,
                    currentRoomName
                );
                
                // Update UI on main thread
                mainHandler.post(() -> {
                    if (callback != null) {
                        callback.onMeasurementUpdate(metrics);
                    }
                });
                
                // Measure bandwidth, latency, jitter, and packet loss (async)
                measureBandwidth(callback);
                
                // Wait 2-3 seconds before next measurement
                Thread.sleep(2500);
                
            } catch (InterruptedException e) {
                break; // Exit loop if interrupted
            } catch (Exception e) {
                mainHandler.post(() -> {
                    if (callback != null) {
                        callback.onError("Measurement error: " + e.getMessage());
                    }
                });
            }
        }
    }
    
    /**
     * Single speed test that runs once and completes
     */
    private void singleSpeedTest() {
        Log.d(TAG, "Executing single speed test for room: " + currentRoomName);
        
        try {
            // Get current signal strength
            int signalStrength = getCurrentSignalStrength();
            Log.d(TAG, "Current signal strength: " + signalStrength + " dBm");
            
            // Measure packet loss and collect latency samples for jitter
            double packetLossPercent = measurePacketLoss();
            double jitterMs = calculateJitter();
            
            // Calculate average latency from the samples collected during packet loss measurement
            int averageLatency = 50; // Default
            if (!latencyMeasurements.isEmpty()) {
                long totalLatency = 0;
                for (Long latency : latencyMeasurements) {
                    totalLatency += latency;
                }
                averageLatency = (int) (totalLatency / latencyMeasurements.size());
            }
            
            Log.d(TAG, "Initial measurements - Packet Loss: " + packetLossPercent + "%, Jitter: " + jitterMs + "ms, Avg Latency: " + averageLatency + "ms");
            
            // Create initial metrics with basic measurements
            NetworkMetrics initialMetrics = new NetworkMetrics(
                signalStrength,
                averageLatency, // Use average latency from packet loss measurements
                0.0, // Bandwidth will be updated by speedtest
                jitterMs,
                packetLossPercent,
                true,
                currentRoomName
            );
            
            Log.d(TAG, "Sending initial metrics to UI");
            // Update UI on main thread
            mainHandler.post(() -> {
                if (callback != null) {
                    callback.onMeasurementUpdate(initialMetrics);
                }
            });
            
            // Perform single comprehensive test (download + ping + jitter + packet loss)
            performSingleComprehensiveTest(signalStrength, jitterMs, packetLossPercent);
            
        } catch (Exception e) {
            Log.e(TAG, "Single test error: " + e.getMessage());
            mainHandler.post(() -> {
                if (callback != null) {
                    callback.onError("Single test error: " + e.getMessage());
                }
            });
            isMeasuring = false;
        }
    }
    
    /**
     * Perform a single comprehensive test that measures all metrics in one go
     */
    private void performSingleComprehensiveTest(int signalStrength, double jitterMs, double packetLossPercent) {
        Log.d(TAG, "Starting comprehensive single test");
        
        // Create variables that can be accessed by inner classes
        final boolean[] bandwidthComplete = {false};
        final double[] finalBandwidth = {0.0};
        final int[] finalLatency = {50};
        
        // Calculate average latency from jitter measurements
        if (!latencyMeasurements.isEmpty()) {
            long totalLatency = 0;
            for (Long latency : latencyMeasurements) {
                totalLatency += latency;
            }
            finalLatency[0] = (int) (totalLatency / latencyMeasurements.size());
            Log.d(TAG, "Using average latency from jitter measurements: " + finalLatency[0] + "ms");
        }
        
        // Create a test completion handler
        Runnable checkTestCompletion = () -> {
            if (bandwidthComplete[0]) {
                // All tests complete, create final measurement
                Log.d(TAG, "All tests complete, creating final measurement");
                
                RoomMeasurement finalMeasurement = new RoomMeasurement(
                    "room_" + System.currentTimeMillis(),
                    currentRoomName,
                    signalStrength,
                    finalLatency[0],
                    finalBandwidth[0],
                    jitterMs,
                    packetLossPercent,
                    currentActivityType
                );
                
                mainHandler.post(() -> {
                    if (callback != null) {
                        callback.onMeasurementComplete(finalMeasurement);
                        
                        // Perform classification on the final measurement
                        ClassificationResult classificationResult = performClassification(finalMeasurement);
                        if (classificationResult != null) {
                            callback.onClassificationComplete(classificationResult);
                        }
                    }
                });
                
                // Stop measuring after single test
                isMeasuring = false;
            }
        };
        
        // Create a single callback that handles all measurements
        MeasurementCallback comprehensiveCallback = new MeasurementCallback() {
            @Override
            public void onMeasurementUpdate(NetworkMetrics metrics) {
                // Update with current metrics
                mainHandler.post(() -> {
                    if (callback != null) {
                        callback.onMeasurementUpdate(metrics);
                    }
                });
            }
            
            @Override
            public void onMeasurementComplete(RoomMeasurement measurement) {
                // This won't be called in our single test approach
            }
            
            @Override
            public void onClassificationComplete(ClassificationResult classificationResult) {
                mainHandler.post(() -> {
                    if (callback != null) {
                        callback.onClassificationComplete(classificationResult);
                    }
                });
            }
            
            @Override
            public void onError(String error) {
                Log.e(TAG, "Comprehensive test error: " + error);
                mainHandler.post(() -> {
                    if (callback != null) {
                        callback.onError(error);
                    }
                });
                isMeasuring = false;
            }
        };
        
        // Start bandwidth test using simple speed test
        Log.d(TAG, "Starting single bandwidth test");
        
        // Try multiple speed test servers (prioritizing working servers)
        String[] singleTestUrls = {
            "http://ipv4.download.thinkbroadband.com/10MB.zip",  // HTTP - Most reliable, tested working
            "https://speedtest.tele2.net/10MB.zip",              // HTTPS fallback
            "https://speedtest.tele2.net/1MB.zip",               // HTTPS - Smaller file
            "http://speedtest.ftp.otenet.gr/files/test10Mb.db",  // HTTP fallback - Greek server
            "http://proof.ovh.net/files/10Mb.dat"                // HTTP fallback - OVH
        };
        
        // Run simple speed test
        executorService.execute(() -> {
            try {
                Log.d(TAG, "Performing simple speed test");
                
                boolean success = false;
                for (String testUrl : singleTestUrls) {
                    try {
                        Log.d(TAG, "Trying speed test server: " + testUrl);
                        
                        long startTime = System.currentTimeMillis();
                        HttpURLConnection connection = setupConnection(testUrl);
                        connection.setRequestMethod("GET");
                        
                        // Connect first
                        connection.connect();
                        int responseCode = connection.getResponseCode();
                        
                        if (responseCode == HttpURLConnection.HTTP_OK) {
                            // Start measuring download time after connection is established
                            long downloadStartTime = System.currentTimeMillis();
                            
                            // Read the file to measure download speed
                            long totalBytes = 0;
                            byte[] buffer = new byte[8192];
                            int bytesRead;
                            
                            while ((bytesRead = connection.getInputStream().read(buffer)) != -1) {
                                totalBytes += bytesRead;
                            }
                            
                            long downloadEndTime = System.currentTimeMillis();
                            long downloadDurationMs = downloadEndTime - downloadStartTime;
                            long totalDurationMs = downloadEndTime - startTime;
                            
                            if (downloadDurationMs > 0) {
                                // Calculate bandwidth in Mbps using only download time
                                double bytesPerSecond = (totalBytes * 1000.0) / downloadDurationMs;
                                finalBandwidth[0] = (bytesPerSecond * 8) / 1000000.0; // Convert to Mbps
                                
                                // Don't update latency here - use the value from jitter measurements
                                Log.d(TAG, "Speed test completed successfully: " + finalBandwidth[0] + " Mbps, Download time: " + downloadDurationMs + "ms, Total time: " + totalDurationMs + "ms");
                                success = true;
                                break;
                            }
                        } else {
                            Log.w(TAG, "HTTP response code: " + responseCode + " for " + testUrl);
                        }
                        
                        connection.disconnect();
                        
                    } catch (Exception e) {
                        Log.w(TAG, "Speed test failed for " + testUrl + ": " + e.getMessage());
                        if (e.getMessage() != null && e.getMessage().contains("SSL")) {
                            Log.w(TAG, "SSL error detected, trying next server");
                        }
                    }
                }
                
                if (!success) {
                    Log.w(TAG, "All speed test servers failed, using signal-based estimation");
                    finalBandwidth[0] = estimateBandwidthFromSignal(signalStrength);
                }
                
                bandwidthComplete[0] = true;
                checkTestCompletion.run();
                
            } catch (Exception e) {
                Log.e(TAG, "Speed test error: " + e.getMessage());
                finalBandwidth[0] = estimateBandwidthFromSignal(signalStrength);
                bandwidthComplete[0] = true;
                checkTestCompletion.run();
            }
        });
        
        // Latency is already calculated from jitter measurements above
        checkTestCompletion.run();
    }
    
    /**
     * Create a complete measurement for a room
     */
    public RoomMeasurement createRoomMeasurement(String roomId, String roomName, String activityType) {
        // Measure current metrics
        int signalStrength = getCurrentSignalStrength();
        int latency = measureLatency();
        double jitter = calculateJitter();
        double packetLoss = measurePacketLoss();
        
        RoomMeasurement measurement = new RoomMeasurement(
            roomId,
            roomName,
            signalStrength,
            latency,
            0.0, // Bandwidth measurement is async, will be updated separately
            jitter,
            packetLoss,
            activityType
        );
        
        // Perform classification and notify callback
        ClassificationResult classificationResult = performClassification(measurement);
        if (classificationResult != null && callback != null) {
            mainHandler.post(() -> callback.onClassificationComplete(classificationResult));
        }
        
        return measurement;
    }
    
    /**
     * Perform WiFi classification based on network metrics
     */
    private ClassificationResult performClassification(NetworkMetrics metrics) {
        try {
            Log.d(TAG, "Performing classification on network metrics");
            Log.d(TAG, "Metrics - Signal: " + metrics.getCurrentSignalDbm() + "dBm, " +
                      "Latency: " + metrics.getCurrentLatencyMs() + "ms, " +
                      "Bandwidth: " + metrics.getCurrentBandwidthMbps() + "Mbps, " +
                      "Jitter: " + metrics.getCurrentJitterMs() + "ms, " +
                      "Packet Loss: " + metrics.getCurrentPacketLossPercent() + "%");
            
            // Get activity importance weights
            ActivityImportance activityImportance = importanceFactory.getActivityImportance(currentActivityType);
            
            // Classify individual metrics
            MetricClassification metricClassification = classifier.classifyMetrics(metrics);
            
            // Calculate weighted overall classification
            WiFiClassification overallClassification = classifier.calculateWeightedClassification(
                metricClassification, activityImportance);
            
            // Get most critical metric
            String mostCriticalMetric = classifier.getMostCriticalMetric(metricClassification, activityImportance);
            
            // Create result
            ClassificationResult result = new ClassificationResult(
                "room_" + System.currentTimeMillis(),
                currentRoomName,
                currentActivityType,
                overallClassification,
                metricClassification,
                activityImportance
            );
            
            result.setMostCriticalMetric(mostCriticalMetric);
            
            Log.d(TAG, "Classification complete - Overall: " + overallClassification.getDisplayName() + 
                      ", Most Critical: " + mostCriticalMetric);
            
            return result;
        } catch (Exception e) {
            // Log error but don't crash the measurement
            Log.e(TAG, "Classification error: " + e.getMessage(), e);
            if (callback != null) {
                mainHandler.post(() -> callback.onError("Classification error: " + e.getMessage()));
            }
            return null;
        }
    }
    
    /**
     * Perform WiFi classification based on room measurement
     */
    private ClassificationResult performClassification(RoomMeasurement measurement) {
        try {
            Log.d(TAG, "Performing classification on room measurement: " + measurement.getRoomName());
            Log.d(TAG, "Measurement - Signal: " + measurement.getSignalStrengthDbm() + "dBm, " +
                      "Latency: " + measurement.getLatencyMs() + "ms, " +
                      "Bandwidth: " + measurement.getBandwidthMbps() + "Mbps, " +
                      "Jitter: " + measurement.getJitterMs() + "ms, " +
                      "Packet Loss: " + measurement.getPacketLossPercent() + "%");
            
            // Get activity importance weights
            ActivityImportance activityImportance = importanceFactory.getActivityImportance(measurement.getActivityType());
            
            // Classify individual metrics
            MetricClassification metricClassification = classifier.classifyMetrics(measurement);
            
            // Calculate weighted overall classification
            WiFiClassification overallClassification = classifier.calculateWeightedClassification(
                metricClassification, activityImportance);
            
            // Get most critical metric
            String mostCriticalMetric = classifier.getMostCriticalMetric(metricClassification, activityImportance);
            
            // Create result
            ClassificationResult result = new ClassificationResult(
                measurement.getRoomId(),
                measurement.getRoomName(),
                measurement.getActivityType(),
                overallClassification,
                metricClassification,
                activityImportance
            );
            
            result.setMostCriticalMetric(mostCriticalMetric);
            
            Log.d(TAG, "Classification complete - Overall: " + overallClassification.getDisplayName() + 
                      ", Most Critical: " + mostCriticalMetric);
            
            return result;
        } catch (Exception e) {
            // Log error but don't crash the measurement
            Log.e(TAG, "Classification error: " + e.getMessage(), e);
            if (callback != null) {
                mainHandler.post(() -> callback.onError("Classification error: " + e.getMessage()));
            }
            return null;
        }
    }
    
    /**
     * Get available activity types for classification
     */
    public String[] getAvailableActivityTypes() {
        return importanceFactory.getAvailableActivityTypes();
    }
    
    /**
     * Check if an activity type is supported
     */
    public boolean isActivityTypeSupported(String activityType) {
        return importanceFactory.isActivityTypeSupported(activityType);
    }
    
    /**
     * Get current activity type
     */
    public String getCurrentActivityType() {
        return currentActivityType;
    }
    
    /**
     * Set current activity type for ongoing measurements
     */
    public void setCurrentActivityType(String activityType) {
        this.currentActivityType = activityType;
    }
    
    /**
     * Cleanup resources
     */
    public void cleanup() {
        Log.d(TAG, "Cleaning up WiFiMeasurementService");
        stopMeasurement();
        
        if (executorService != null) {
            Log.d(TAG, "Shutting down executor service");
            executorService.shutdown();
            try {
                // Wait for tasks to finish
                if (!executorService.awaitTermination(2, java.util.concurrent.TimeUnit.SECONDS)) {
                    Log.w(TAG, "Executor did not terminate gracefully, forcing shutdown");
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                Log.w(TAG, "Interrupted while waiting for executor shutdown");
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        
        // No speed test socket to clean up (using simple HTTP connections)
        
        callback = null;
        Log.d(TAG, "WiFiMeasurementService cleanup completed");
    }
}
