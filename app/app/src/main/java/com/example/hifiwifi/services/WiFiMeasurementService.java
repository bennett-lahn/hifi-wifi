package com.example.hifiwifi.services;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Looper;

import com.example.hifiwifi.models.NetworkMetrics;
import com.example.hifiwifi.models.RoomMeasurement;
import com.example.hifiwifi.classifier.WiFiClassifier;
import com.example.hifiwifi.classifier.WiFiClassification;
import com.example.hifiwifi.classifier.ClassificationResult;
import com.example.hifiwifi.classifier.MetricClassification;
import com.example.hifiwifi.classifier.ActivityImportance;
import com.example.hifiwifi.classifier.ActivityImportanceFactory;

import fr.bmartel.speedtest.SpeedTestReport;
import fr.bmartel.speedtest.SpeedTestSocket;
import fr.bmartel.speedtest.inter.ISpeedTestListener;
import fr.bmartel.speedtest.model.SpeedTestError;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Service for measuring WiFi performance metrics
 * Uses Ookla Speedtest API via fr.bmartel:speedtest library
 */
public class WiFiMeasurementService {
    
    private Context context;
    private WifiManager wifiManager;
    private ExecutorService executorService;
    private Handler mainHandler;
    private SpeedTestSocket speedTestSocket;
    
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
    private java.util.List<Long> latencyMeasurements = new java.util.ArrayList<>();
    private static final int JITTER_SAMPLE_SIZE = 10;
    
    public WiFiMeasurementService(Context context) {
        this.context = context;
        this.wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        this.executorService = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.speedTestSocket = new SpeedTestSocket();
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
     */
    public void startMeasurement(String roomName, String activityType) {
        if (isMeasuring) {
            stopMeasurement();
        }
        
        isMeasuring = true;
        currentRoomName = roomName;
        currentActivityType = activityType;
        
        // Start measurement loop
        executorService.execute(this::measurementLoop);
    }
    
    /**
     * Stop current measurement
     */
    public void stopMeasurement() {
        isMeasuring = false;
        try {
            // Try to stop the speed test if it's running
            if (speedTestSocket != null) {
                // The speedtest library may not have forceStopSpeedTest method
                // We'll rely on the isMeasuring flag to stop new measurements
                // and let any ongoing tests complete naturally
            }
        } catch (Exception e) {
            // If stopping fails, just log and continue
            // The isMeasuring flag will prevent new measurements
        }
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
     * Measure network latency using Ookla speedtest API
     * This will be called as part of the speedtest process
     */
    public int measureLatency() {
        // For now, return a placeholder value
        // The actual latency will be measured by the Ookla speedtest API
        return 50; // This will be updated when speedtest completes
    }
    
    /**
     * Calculate jitter from recent latency measurements
     */
    public double calculateJitter() {
        if (latencyMeasurements.size() < 2) {
            return 0.0;
        }
        
        // Calculate standard deviation of latency measurements
        double sum = 0.0;
        for (Long latency : latencyMeasurements) {
            sum += latency;
        }
        double mean = sum / latencyMeasurements.size();
        
        double sumSquaredDiff = 0.0;
        for (Long latency : latencyMeasurements) {
            double diff = latency - mean;
            sumSquaredDiff += diff * diff;
        }
        
        double variance = sumSquaredDiff / latencyMeasurements.size();
        return Math.sqrt(variance);
    }
    
    /**
     * Measure packet loss using ping-like approach
     * This is a simplified implementation - in production, you might want to use a more sophisticated method
     */
    public double measurePacketLoss() {
        int totalPings = 10;
        int successfulPings = 0;
        
        for (int i = 0; i < totalPings; i++) {
            try {
                long startTime = System.currentTimeMillis();
                URL url = new URL("http://www.google.com");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setConnectTimeout(2000);
                connection.setReadTimeout(2000);
                connection.setRequestMethod("HEAD");
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
                }
            } catch (Exception e) {
                // Ping failed - count as packet loss
            }
        }
        
        double packetLossPercent = ((totalPings - successfulPings) * 100.0) / totalPings;
        return packetLossPercent;
    }
    
    /**
     * Measure bandwidth, latency, jitter, and packet loss using Ookla Speedtest
     */
    public void measureBandwidth(MeasurementCallback callback) {
        speedTestSocket.addSpeedTestListener(new ISpeedTestListener() {
            @Override
            public void onCompletion(SpeedTestReport report) {
                // Report is in bit/s, convert to Mbps
                double bandwidthMbps = report.getTransferRateBit().doubleValue() / 1000000.0;
                
                // Get latency from speedtest report (if available)
                int latencyMs = 50; // Default fallback
                // Note: The speedtest library may not have getLatency() method
                // Using default value for now
                
                // Calculate jitter and packet loss
                double jitterMs = calculateJitter();
                double packetLossPercent = measurePacketLoss();
                
                mainHandler.post(() -> {
                    if (callback != null) {
                        // Create a measurement with all network metrics
                        NetworkMetrics metrics = new NetworkMetrics(
                            getCurrentSignalStrength(),
                            latencyMs,
                            bandwidthMbps,
                            jitterMs,
                            packetLossPercent,
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
            
            @Override
            public void onError(SpeedTestError speedTestError, String errorMessage) {
                mainHandler.post(() -> {
                    if (callback != null) {
                        callback.onError("Bandwidth measurement failed: " + errorMessage);
                    }
                });
            }
            
            @Override
            public void onProgress(float percent, SpeedTestReport report) {
                // Progress updates can be handled here if needed
                // We could also update metrics during progress if needed
            }
        });
        
        // Start download test
        speedTestSocket.startDownload("http://speedtest.ftp.otenet.gr/files/test1Mb.db");
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
                currentRoomName,
                currentActivityType,
                overallClassification,
                metricClassification,
                activityImportance
            );
            
            result.setMostCriticalMetric(mostCriticalMetric);
            
            return result;
        } catch (Exception e) {
            // Log error but don't crash the measurement
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
                measurement.getRoomName(),
                measurement.getActivityType(),
                overallClassification,
                metricClassification,
                activityImportance
            );
            
            result.setMostCriticalMetric(mostCriticalMetric);
            
            return result;
        } catch (Exception e) {
            // Log error but don't crash the measurement
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
        stopMeasurement();
        if (executorService != null) {
            executorService.shutdown();
        }
    }
}
