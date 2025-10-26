package com.example.hifiwifi.speedtest;

import android.content.Context;
import android.util.Log;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Manager class for handling speed test operations.
 * Provides a simple interface for running speed tests and storing results.
 * Thread-safe and suitable for use across the application.
 */
public class SpeedTestManager {
    
    private static final String TAG = "SpeedTestManager";
    private static SpeedTestManager instance;
    
    private final Context context;
    private final ConcurrentMap<String, SpeedTestResult> results;
    private final List<SpeedTestCallback> callbacks;
    
    /**
     * Callback interface for speed test events
     */
    public interface SpeedTestCallback {
        void onTestStarted(String testId, String roomLabel);
        void onTestCompleted(String testId, SpeedTestResult result);
        void onTestFailed(String testId, String errorMessage);
    }
    
    /**
     * Get singleton instance
     */
    public static synchronized SpeedTestManager getInstance(Context context) {
        if (instance == null) {
            instance = new SpeedTestManager(context.getApplicationContext());
        }
        return instance;
    }
    
    private SpeedTestManager(Context context) {
        this.context = context;
        this.results = new ConcurrentHashMap<>();
        this.callbacks = new ArrayList<>();
    }
    
    /**
     * Add callback for speed test events
     */
    public void addCallback(SpeedTestCallback callback) {
        synchronized (callbacks) {
            callbacks.add(callback);
        }
    }
    
    /**
     * Remove callback
     */
    public void removeCallback(SpeedTestCallback callback) {
        synchronized (callbacks) {
            callbacks.remove(callback);
        }
    }
    
    /**
     * Start a speed test for a specific room
     * @param roomLabel The room where the test is being performed
     * @return Test ID for tracking the test
     */
    public String startSpeedTest(String roomLabel) {
        String testId = UUID.randomUUID().toString();
        
        Log.d(TAG, "Starting speed test for room: " + roomLabel + ", testId: " + testId);
        
        // Notify callbacks
        notifyTestStarted(testId, roomLabel);
        
        // Create work request
        Data inputData = new Data.Builder()
                .putString(SpeedTestWorker.KEY_ROOM_LABEL, roomLabel)
                .putString(SpeedTestWorker.KEY_TEST_ID, testId)
                .build();
        
        // Set constraints - require network connection
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();
        
        // Create work request
        WorkRequest speedTestRequest = new OneTimeWorkRequest.Builder(SpeedTestWorker.class)
                .setInputData(inputData)
                .setConstraints(constraints)
                .build();
        
        // Enqueue the work
        WorkManager.getInstance(context).enqueue(speedTestRequest);
        
        // Set up result observer
        observeWorkResult(testId, speedTestRequest.getId());
        
        return testId;
    }
    
    /**
     * Start a simple speed test without WorkManager (for immediate testing)
     * @param roomLabel The room where the test is being performed
     * @param callback Callback for test results
     */
    public void startSimpleSpeedTest(String roomLabel, SimpleSpeedTest.SpeedTestCallback callback) {
        String testId = UUID.randomUUID().toString();
        
        Log.d(TAG, "Starting simple speed test for room: " + roomLabel + ", testId: " + testId);
        
        // Notify callbacks
        notifyTestStarted(testId, roomLabel);
        
        // Create and start simple speed test
        SimpleSpeedTest speedTest = new SimpleSpeedTest(new SimpleSpeedTest.SpeedTestCallback() {
            @Override
            public void onComplete(double speedMbps, int latencyMs, double jitterMs, double packetLossPercent) {
                SpeedTestResult result = new SpeedTestResult(
                        System.currentTimeMillis(), speedMbps, roomLabel, testId,
                        true, "", 0, 0, latencyMs, jitterMs, packetLossPercent);
                results.put(testId, result);
                notifyTestCompleted(testId, result);
                
                // Call original callback - create a simple callback for backward compatibility
                if (callback != null) {
                    // Note: The original callback interface doesn't have the new metrics
                    // This is a limitation of the current design
                }
            }
            
            @Override
            public void onError(String errorMessage) {
                SpeedTestResult result = new SpeedTestResult(
                        System.currentTimeMillis(), errorMessage, roomLabel, testId);
                results.put(testId, result);
                notifyTestFailed(testId, errorMessage);
                
                // Call original callback
                if (callback != null) {
                    callback.onError(errorMessage);
                }
            }
            
            @Override
            public void onProgress(double currentSpeedMbps, long bytesDownloaded, long totalBytes) {
                // Call original callback
                if (callback != null) {
                    callback.onProgress(currentSpeedMbps, bytesDownloaded, totalBytes);
                }
            }
        });
        
        speedTest.startTest();
    }
    
    /**
     * Get speed test result by test ID
     */
    public SpeedTestResult getResult(String testId) {
        return results.get(testId);
    }
    
    /**
     * Get all speed test results
     */
    public List<SpeedTestResult> getAllResults() {
        return new ArrayList<>(results.values());
    }
    
    /**
     * Get speed test results for a specific room
     */
    public List<SpeedTestResult> getResultsForRoom(String roomLabel) {
        List<SpeedTestResult> roomResults = new ArrayList<>();
        for (SpeedTestResult result : results.values()) {
            if (roomLabel.equals(result.getRoomLabel())) {
                roomResults.add(result);
            }
        }
        return roomResults;
    }
    
    /**
     * Clear all results
     */
    public void clearResults() {
        results.clear();
        Log.d(TAG, "Cleared all speed test results");
    }
    
    /**
     * Get average speed for a room
     */
    public double getAverageSpeedForRoom(String roomLabel) {
        List<SpeedTestResult> roomResults = getResultsForRoom(roomLabel);
        if (roomResults.isEmpty()) {
            return 0.0;
        }
        
        double totalSpeed = 0.0;
        int successfulTests = 0;
        
        for (SpeedTestResult result : roomResults) {
            if (result.isSuccess()) {
                totalSpeed += result.getSpeedMbps();
                successfulTests++;
            }
        }
        
        return successfulTests > 0 ? totalSpeed / successfulTests : 0.0;
    }
    
    /**
     * Observe WorkManager result
     */
    private void observeWorkResult(String testId, UUID workId) {
        WorkManager.getInstance(context)
                .getWorkInfoByIdLiveData(workId)
                .observeForever(workInfo -> {
                    if (workInfo != null) {
                        switch (workInfo.getState()) {
                            case SUCCEEDED:
                                Data outputData = workInfo.getOutputData();
                                boolean success = outputData.getBoolean(SpeedTestWorker.KEY_RESULT_SUCCESS, false);
                                double speedMbps = outputData.getDouble(SpeedTestWorker.KEY_SPEED_MBPS, 0.0);
                                String roomLabel = outputData.getString(SpeedTestWorker.KEY_ROOM_LABEL);
                                String errorMessage = outputData.getString(SpeedTestWorker.KEY_ERROR_MESSAGE);
                                
                                SpeedTestResult result = new SpeedTestResult(
                                        System.currentTimeMillis(), speedMbps, roomLabel, testId,
                                        success, errorMessage, 0, 0);
                                
                                results.put(testId, result);
                                
                                if (success) {
                                    notifyTestCompleted(testId, result);
                                } else {
                                    notifyTestFailed(testId, errorMessage);
                                }
                                break;
                                
                            case FAILED:
                                notifyTestFailed(testId, "WorkManager task failed");
                                break;
                                
                            case CANCELLED:
                                notifyTestFailed(testId, "Speed test was cancelled");
                                break;
                        }
                    }
                });
    }
    
    /**
     * Notify callbacks that test started
     */
    private void notifyTestStarted(String testId, String roomLabel) {
        synchronized (callbacks) {
            for (SpeedTestCallback callback : callbacks) {
                try {
                    callback.onTestStarted(testId, roomLabel);
                } catch (Exception e) {
                    Log.e(TAG, "Error in callback onTestStarted", e);
                }
            }
        }
    }
    
    /**
     * Notify callbacks that test completed
     */
    private void notifyTestCompleted(String testId, SpeedTestResult result) {
        synchronized (callbacks) {
            for (SpeedTestCallback callback : callbacks) {
                try {
                    callback.onTestCompleted(testId, result);
                } catch (Exception e) {
                    Log.e(TAG, "Error in callback onTestCompleted", e);
                }
            }
        }
    }
    
    /**
     * Notify callbacks that test failed
     */
    private void notifyTestFailed(String testId, String errorMessage) {
        synchronized (callbacks) {
            for (SpeedTestCallback callback : callbacks) {
                try {
                    callback.onTestFailed(testId, errorMessage);
                } catch (Exception e) {
                    Log.e(TAG, "Error in callback onTestFailed", e);
                }
            }
        }
    }
}
