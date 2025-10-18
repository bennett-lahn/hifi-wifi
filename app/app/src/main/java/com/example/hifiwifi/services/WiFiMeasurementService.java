package com.example.hifiwifi.services;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Looper;

import com.example.hifiwifi.models.NetworkMetrics;
import com.example.hifiwifi.models.RoomMeasurement;

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
        void onError(String error);
    }
    
    private MeasurementCallback callback;
    private boolean isMeasuring = false;
    private String currentRoomName = "";
    
    public WiFiMeasurementService(Context context) {
        this.context = context;
        this.wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        this.executorService = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.speedTestSocket = new SpeedTestSocket();
    }
    
    public void setCallback(MeasurementCallback callback) {
        this.callback = callback;
    }
    
    /**
     * Start continuous WiFi measurement for a specific room
     */
    public void startMeasurement(String roomName) {
        if (isMeasuring) {
            stopMeasurement();
        }
        
        isMeasuring = true;
        currentRoomName = roomName;
        
        // Start measurement loop
        executorService.execute(this::measurementLoop);
    }
    
    /**
     * Stop current measurement
     */
    public void stopMeasurement() {
        isMeasuring = false;
        speedTestSocket.forceStopSpeedTest();
    }
    
    /**
     * Get current signal strength in dBm
     */
    public int getCurrentSignalStrength() {
        // TODO: Implement using WifiManager.getConnectionInfo().getRssi()
        if (wifiManager != null && wifiManager.getConnectionInfo() != null) {
            return wifiManager.getConnectionInfo().getRssi();
        }
        return -100; // Default poor signal
    }
    
    /**
     * Measure network latency using ping
     */
    public int measureLatency() {
        // TODO: Implement ping-based latency measurement
        // For now, return mock value
        return 50; // Mock 50ms latency
    }
    
    /**
     * Measure bandwidth using Ookla Speedtest
     */
    public void measureBandwidth(MeasurementCallback callback) {
        // TODO: Implement using fr.bmartel:speedtest library
        speedTestSocket.addSpeedTestListener(new ISpeedTestListener() {
            @Override
            public void onCompletion(SpeedTestReport report) {
                // Report is in bit/s, convert to Mbps
                double bandwidthMbps = report.getTransferRateBit().doubleValue() / 1000000.0;
                mainHandler.post(() -> {
                    if (callback != null) {
                        // Create a mock measurement with the bandwidth result
                        NetworkMetrics metrics = new NetworkMetrics(
                            getCurrentSignalStrength(),
                            measureLatency(),
                            bandwidthMbps,
                            true,
                            currentRoomName
                        );
                        callback.onMeasurementUpdate(metrics);
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
                
                // Measure latency
                int latency = measureLatency();
                
                // Create current metrics
                NetworkMetrics metrics = new NetworkMetrics(
                    signalStrength,
                    latency,
                    0.0, // Bandwidth will be updated separately
                    true,
                    currentRoomName
                );
                
                // Update UI on main thread
                mainHandler.post(() -> {
                    if (callback != null) {
                        callback.onMeasurementUpdate(metrics);
                    }
                });
                
                // Measure bandwidth (async)
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
        return new RoomMeasurement(
            roomId,
            roomName,
            getCurrentSignalStrength(),
            measureLatency(),
            0.0, // TODO: Get actual bandwidth measurement
            activityType
        );
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
