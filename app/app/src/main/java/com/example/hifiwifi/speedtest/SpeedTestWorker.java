package com.example.hifiwifi.speedtest;

import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * WorkManager Worker for performing network speed tests.
 * Integrates SimpleSpeedTest with WorkManager for background execution.
 * Stores results with timestamp and room information.
 */
public class SpeedTestWorker extends Worker {
    
    private static final String TAG = "SpeedTestWorker";
    private static final int WORKER_TIMEOUT_SECONDS = 15; // Slightly longer than test duration
    
    // Input data keys
    public static final String KEY_ROOM_LABEL = "room_label";
    public static final String KEY_TEST_ID = "test_id";
    
    // Output data keys
    public static final String KEY_RESULT_SUCCESS = "result_success";
    public static final String KEY_SPEED_MBPS = "speed_mbps";
    public static final String KEY_ERROR_MESSAGE = "error_message";
    public static final String KEY_TIMESTAMP = "timestamp";
    public static final String KEY_BYTES_DOWNLOADED = "bytes_downloaded";
    public static final String KEY_TEST_DURATION_MS = "test_duration_ms";
    
    // Thread-safe result storage
    private volatile SpeedTestResult result;
    private final CountDownLatch latch = new CountDownLatch(1);
    
    public SpeedTestWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }
    
    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "Starting speed test worker");
        
        try {
            // Get input parameters
            String roomLabel = getInputData().getString(KEY_ROOM_LABEL);
            String testId = getInputData().getString(KEY_TEST_ID);
            
            if (roomLabel == null) {
                roomLabel = "Unknown Room";
            }
            if (testId == null) {
                testId = String.valueOf(System.currentTimeMillis());
            }
            
            Log.d(TAG, "Running speed test for room: " + roomLabel + ", testId: " + testId);
            
            // Perform the speed test
            performSpeedTest(roomLabel, testId);
            
            // Wait for test completion with timeout
            boolean completed = latch.await(WORKER_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            
            if (!completed) {
                Log.e(TAG, "Speed test timed out");
                return Result.failure(createFailureData("Speed test timed out"));
            }
            
            if (result == null) {
                Log.e(TAG, "Speed test failed - no result");
                return Result.failure(createFailureData("Speed test failed - no result"));
            }
            
            if (result.isSuccess()) {
                Log.d(TAG, "Speed test completed successfully: " + result.getSpeedMbps() + " Mbps");
                return Result.success(createSuccessData(result));
            } else {
                Log.e(TAG, "Speed test failed: " + result.getErrorMessage());
                return Result.failure(createFailureData(result.getErrorMessage()));
            }
            
        } catch (InterruptedException e) {
            Log.e(TAG, "Speed test worker interrupted", e);
            Thread.currentThread().interrupt();
            return Result.failure(createFailureData("Worker interrupted: " + e.getMessage()));
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error in speed test worker", e);
            return Result.failure(createFailureData("Unexpected error: " + e.getMessage()));
        }
    }
    
    /**
     * Perform the actual speed test using SimpleSpeedTest
     */
    private void performSpeedTest(String roomLabel, String testId) {
        SimpleSpeedTest speedTest = new SimpleSpeedTest(new SimpleSpeedTest.SpeedTestCallback() {
            @Override
            public void onComplete(double speedMbps, int latencyMs, double jitterMs, double packetLossPercent) {
                Log.d(TAG, "Speed test completed: " + speedMbps + " Mbps, " + latencyMs + "ms latency, " + 
                      jitterMs + "ms jitter, " + packetLossPercent + "% packet loss");
                result = new SpeedTestResult(
                    System.currentTimeMillis(), speedMbps, roomLabel, testId,
                    true, "", 0, 0, latencyMs, jitterMs, packetLossPercent);
                latch.countDown();
            }
            
            @Override
            public void onError(String errorMessage) {
                Log.e(TAG, "Speed test error: " + errorMessage);
                result = new SpeedTestResult(
                    System.currentTimeMillis(), errorMessage, roomLabel, testId);
                latch.countDown();
            }
            
            @Override
            public void onProgress(double currentSpeedMbps, long bytesDownloaded, long totalBytes) {
                Log.d(TAG, "Speed test progress: " + currentSpeedMbps + " Mbps, " + 
                      bytesDownloaded + "/" + totalBytes + " bytes");
                // Update result with progress if needed
                if (result == null) {
                    result = new SpeedTestResult(
                        System.currentTimeMillis(), "In progress", roomLabel, testId);
                }
            }
        });
        
        // Start the test
        speedTest.startTest();
    }
    
    /**
     * Create output data for successful test
     */
    private Data createSuccessData(SpeedTestResult result) {
        return new Data.Builder()
                .putBoolean(KEY_RESULT_SUCCESS, true)
                .putDouble(KEY_SPEED_MBPS, result.getSpeedMbps())
                .putString(KEY_ROOM_LABEL, result.getRoomLabel())
                .putString(KEY_TEST_ID, result.getTestId())
                .putLong(KEY_TIMESTAMP, System.currentTimeMillis())
                .putString(KEY_ERROR_MESSAGE, "")
                .build();
    }
    
    /**
     * Create output data for failed test
     */
    private Data createFailureData(String errorMessage) {
        return new Data.Builder()
                .putBoolean(KEY_RESULT_SUCCESS, false)
                .putDouble(KEY_SPEED_MBPS, 0.0)
                .putString(KEY_ERROR_MESSAGE, errorMessage)
                .putLong(KEY_TIMESTAMP, System.currentTimeMillis())
                .build();
    }
    
}
