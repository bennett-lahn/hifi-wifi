package com.example.hifiwifi.speedtest;

import android.os.AsyncTask;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;

/**
 * Simple network speed test implementation for measuring download speeds.
 * Uses HttpURLConnection to download test data and calculate Mbps.
 * Designed for hackathon demo with 10-second maximum test duration.
 */
public class SimpleSpeedTest {
    
    // Test URLs - primary and fallback
    private static final String PRIMARY_URL = "https://speed.cloudflare.com/__down?bytes=10000000";
    private static final String FALLBACK_URL = "http://ipv4.download.thinkbroadband.com/10MB.zip";
    
    // Configuration constants
    private static final int BUFFER_SIZE = 8192;
    private static final int CONNECTION_TIMEOUT = 10000; // 10 seconds
    private static final int READ_TIMEOUT = 10000; // 10 seconds
    private static final int MAX_TEST_DURATION = 10000; // 10 seconds in milliseconds
    private static final int PROGRESS_UPDATE_INTERVAL = 1000; // 1 second
    
    // Callback interfaces
    public interface SpeedTestCallback {
        void onComplete(double speedMbps, int latencyMs, double jitterMs, double packetLossPercent);
        void onError(String errorMessage);
        void onProgress(double currentSpeedMbps, long bytesDownloaded, long totalBytes);
    }
    
    private SpeedTestCallback callback;
    private SpeedTestTask currentTask;
    
    /**
     * Constructor
     * @param callback Callback interface for test results
     */
    public SimpleSpeedTest(SpeedTestCallback callback) {
        this.callback = callback;
    }
    
    /**
     * Start the speed test asynchronously
     */
    public void startTest() {
        if (currentTask != null && currentTask.getStatus() == AsyncTask.Status.RUNNING) {
            currentTask.cancel(true);
        }
        
        currentTask = new SpeedTestTask();
        currentTask.execute();
    }
    
    /**
     * Cancel the current speed test
     */
    public void cancelTest() {
        if (currentTask != null && currentTask.getStatus() == AsyncTask.Status.RUNNING) {
            currentTask.cancel(true);
        }
    }
    
    /**
     * AsyncTask for performing the speed test in background
     */
    private class SpeedTestTask extends AsyncTask<Void, SpeedTestProgress, SpeedTestResult> {
        
        // Latency and jitter measurement variables
        private java.util.List<Long> latencyMeasurements = new java.util.ArrayList<>();
        private static final int LATENCY_SAMPLE_SIZE = 10;
        private static final int PACKET_LOSS_SAMPLE_SIZE = 10;
        
        @Override
        protected SpeedTestResult doInBackground(Void... voids) {
            HttpURLConnection connection = null;
            InputStream inputStream = null;
            
            try {
                // First, measure latency and packet loss before speed test
                int latencyMs = measureLatency();
                double packetLossPercent = measurePacketLoss();
                
                // Try primary URL first
                URL url = new URL(PRIMARY_URL);
                connection = (HttpURLConnection) url.openConnection();
                connection.setConnectTimeout(CONNECTION_TIMEOUT);
                connection.setReadTimeout(READ_TIMEOUT);
                connection.setRequestMethod("GET");
                connection.setDoInput(true);
                
                // Connect to the server
                connection.connect();
                
                int responseCode = connection.getResponseCode();
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    // Try fallback URL
                    connection.disconnect();
                    url = new URL(FALLBACK_URL);
                    connection = (HttpURLConnection) url.openConnection();
                    connection.setConnectTimeout(CONNECTION_TIMEOUT);
                    connection.setReadTimeout(READ_TIMEOUT);
                    connection.setRequestMethod("GET");
                    connection.setDoInput(true);
                    connection.connect();
                    
                    responseCode = connection.getResponseCode();
                    if (responseCode != HttpURLConnection.HTTP_OK) {
                        return new SpeedTestResult(false, "HTTP Error: " + responseCode, 0.0, 0, 0.0, 0.0);
                    }
                }
                
                // Get content length for progress tracking
                long contentLength = connection.getContentLength();
                if (contentLength <= 0) {
                    contentLength = 10 * 1024 * 1024; // Assume 10MB if unknown
                }
                
                inputStream = connection.getInputStream();
                
                // Download data and measure speed
                SpeedTestResult result = performDownload(inputStream, contentLength);
                
                // Add latency and packet loss measurements to result
                if (result.success) {
                    result.latencyMs = latencyMs;
                    result.packetLossPercent = packetLossPercent;
                    result.jitterMs = calculateJitter();
                }
                
                return result;
                
            } catch (UnknownHostException e) {
                return new SpeedTestResult(false, "No internet connection", 0.0, 0, 0.0, 0.0);
            } catch (IOException e) {
                return new SpeedTestResult(false, "Network error: " + e.getMessage(), 0.0, 0, 0.0, 0.0);
            } catch (Exception e) {
                return new SpeedTestResult(false, "Unexpected error: " + e.getMessage(), 0.0, 0, 0.0, 0.0);
            } finally {
                // Clean up resources
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (IOException e) {
                        // Ignore close errors
                    }
                }
                if (connection != null) {
                    connection.disconnect();
                }
            }
        }
        
        @Override
        protected void onProgressUpdate(SpeedTestProgress... progress) {
            if (callback != null && progress.length > 0) {
                SpeedTestProgress p = progress[0];
                callback.onProgress(p.currentSpeedMbps, p.bytesDownloaded, p.totalBytes);
            }
        }
        
        @Override
        protected void onPostExecute(SpeedTestResult result) {
            if (callback != null) {
                if (result.success) {
                    callback.onComplete(result.speedMbps, result.latencyMs, result.jitterMs, result.packetLossPercent);
                } else {
                    callback.onError(result.errorMessage);
                }
            }
        }
        
        @Override
        protected void onCancelled() {
            if (callback != null) {
                callback.onError("Speed test cancelled");
            }
        }
        
        /**
         * Perform the actual download and speed measurement
         */
        private SpeedTestResult performDownload(InputStream inputStream, long contentLength) throws IOException {
            byte[] buffer = new byte[BUFFER_SIZE];
            long totalBytesDownloaded = 0;
            long startTime = System.currentTimeMillis();
            long lastProgressUpdate = startTime;
            
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1 && !isCancelled()) {
                totalBytesDownloaded += bytesRead;
                
                long currentTime = System.currentTimeMillis();
                long elapsedTime = currentTime - startTime;
                
                // Check if we've exceeded maximum test duration
                if (elapsedTime >= MAX_TEST_DURATION) {
                    break;
                }
                
                // Update progress every second
                if (currentTime - lastProgressUpdate >= PROGRESS_UPDATE_INTERVAL) {
                    double currentSpeed = calculateSpeed(totalBytesDownloaded, elapsedTime);
                    publishProgress(new SpeedTestProgress(currentSpeed, totalBytesDownloaded, contentLength));
                    lastProgressUpdate = currentTime;
                }
            }
            
            if (isCancelled()) {
                return new SpeedTestResult(false, "Test cancelled", 0.0);
            }
            
            long totalTime = System.currentTimeMillis() - startTime;
            if (totalTime == 0) {
                return new SpeedTestResult(false, "Test completed too quickly", 0.0);
            }
            
            double finalSpeed = calculateSpeed(totalBytesDownloaded, totalTime);
            return new SpeedTestResult(true, null, finalSpeed);
        }
        
        /**
         * Calculate speed in Mbps
         * Formula: ((totalBytes * 8) / 1,000,000) / durationInSeconds
         */
        private double calculateSpeed(long bytesDownloaded, long durationMs) {
            double durationSeconds = durationMs / 1000.0;
            double speedMbps = ((bytesDownloaded * 8.0) / 1_000_000.0) / durationSeconds;
            return Math.round(speedMbps * 100.0) / 100.0; // Round to 2 decimal places
        }
        
        /**
         * Measure network latency using ping-like approach
         */
        private int measureLatency() {
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
                    long latency = System.currentTimeMillis() - startTime;
                    latencyMeasurements.add(latency);
                    
                    // Keep only recent measurements for jitter calculation
                    if (latencyMeasurements.size() > LATENCY_SAMPLE_SIZE) {
                        latencyMeasurements.remove(0);
                    }
                    
                    return (int) latency;
                }
            } catch (Exception e) {
                // Latency measurement failed
            }
            return 50; // Default fallback
        }
        
        /**
         * Measure packet loss using ping-like approach
         */
        private double measurePacketLoss() {
            int totalPings = PACKET_LOSS_SAMPLE_SIZE;
            int successfulPings = 0;
            
            for (int i = 0; i < totalPings; i++) {
                try {
                    long startTime = System.currentTimeMillis();
                    URL url = new URL("http://www.google.com");
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setConnectTimeout(1000);
                    connection.setReadTimeout(1000);
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
                        if (latencyMeasurements.size() > LATENCY_SAMPLE_SIZE) {
                            latencyMeasurements.remove(0);
                        }
                    }
                } catch (Exception e) {
                    // Ping failed - count as packet loss
                }
            }
            
            double packetLossPercent = ((totalPings - successfulPings) * 100.0) / totalPings;
            return Math.round(packetLossPercent * 100.0) / 100.0; // Round to 2 decimal places
        }
        
        /**
         * Calculate jitter from recent latency measurements
         */
        private double calculateJitter() {
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
            double jitter = Math.sqrt(variance);
            return Math.round(jitter * 100.0) / 100.0; // Round to 2 decimal places
        }
    }
    
    /**
     * Data class for speed test progress updates
     */
    private static class SpeedTestProgress {
        final double currentSpeedMbps;
        final long bytesDownloaded;
        final long totalBytes;
        
        SpeedTestProgress(double currentSpeedMbps, long bytesDownloaded, long totalBytes) {
            this.currentSpeedMbps = currentSpeedMbps;
            this.bytesDownloaded = bytesDownloaded;
            this.totalBytes = totalBytes;
        }
    }
    
    /**
     * Data class for speed test results
     */
    private static class SpeedTestResult {
        final boolean success;
        final String errorMessage;
        final double speedMbps;
        int latencyMs;
        double jitterMs;
        double packetLossPercent;
        
        SpeedTestResult(boolean success, String errorMessage, double speedMbps) {
            this.success = success;
            this.errorMessage = errorMessage;
            this.speedMbps = speedMbps;
            this.latencyMs = 0;
            this.jitterMs = 0.0;
            this.packetLossPercent = 0.0;
        }
        
        SpeedTestResult(boolean success, String errorMessage, double speedMbps, int latencyMs, double jitterMs, double packetLossPercent) {
            this.success = success;
            this.errorMessage = errorMessage;
            this.speedMbps = speedMbps;
            this.latencyMs = latencyMs;
            this.jitterMs = jitterMs;
            this.packetLossPercent = packetLossPercent;
        }
    }
}
