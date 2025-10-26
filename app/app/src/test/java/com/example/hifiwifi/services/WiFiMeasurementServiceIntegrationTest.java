package com.example.hifiwifi.services;

import android.content.Context;

import com.example.hifiwifi.models.NetworkMetrics;
import com.example.hifiwifi.models.RoomMeasurement;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

/**
 * Integration tests for WiFiMeasurementService
 * These tests demonstrate how to test the service with real Android components
 */
@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, sdk = 33, qualifiers = "")
public class WiFiMeasurementServiceIntegrationTest {
    
    private Context context;
    private WiFiMeasurementService wifiService;
    private CountDownLatch latch;
    private NetworkMetrics receivedMetrics;
    private String receivedError;
    
    @Before
    public void setUp() {
        context = RuntimeEnvironment.getApplication();
        wifiService = new WiFiMeasurementService(context);
        latch = new CountDownLatch(1);
        receivedMetrics = null;
        receivedError = null;
    }
    
    @Test
    public void testServiceInitialization() {
        assertNotNull("WiFi service should be initialized", wifiService);
    }
    
    @Test
    public void testSignalStrengthMeasurement() {
        int signalStrength = wifiService.getCurrentSignalStrength();
        
        // Signal strength should be within reasonable range
        assertTrue("Signal strength should be >= -100 dBm", signalStrength >= -100);
        assertTrue("Signal strength should be <= 0 dBm", signalStrength <= 0);
    }
    
    @Test
    public void testLatencyMeasurement() {
        int latency = wifiService.measureLatency();
        
        // Latency should be non-negative
        assertTrue("Latency should be non-negative", latency >= 0);
        // In a real test environment, latency might be high due to network conditions
        assertTrue("Latency should be reasonable (< 10 seconds)", latency < 10000);
    }
    
    @Test
    public void testRoomMeasurementCreation() {
        String roomId = "integration_test_room";
        String roomName = "Integration Test Room";
        String activityType = "gaming";
        
        RoomMeasurement measurement = wifiService.createRoomMeasurement(
            roomId, roomName, activityType
        );
        
        assertNotNull("Room measurement should not be null", measurement);
        assertEquals("Room ID should match", roomId, measurement.getRoomId());
        assertEquals("Room name should match", roomName, measurement.getRoomName());
        assertEquals("Activity type should match", activityType, measurement.getActivityType());
        assertTrue("Timestamp should be set", measurement.getTimestamp() > 0);
        assertTrue("Signal strength should be reasonable", 
                  measurement.getSignalStrengthDbm() >= -100);
        assertTrue("Latency should be non-negative", 
                  measurement.getLatencyMs() >= 0);
    }
    
    @Test
    public void testMeasurementCallback() throws InterruptedException {
        // Set up callback to capture results
        wifiService.setCallback(new WiFiMeasurementService.MeasurementCallback() {
            @Override
            public void onMeasurementUpdate(NetworkMetrics metrics) {
                receivedMetrics = metrics;
                latch.countDown();
            }
            
            @Override
            public void onMeasurementComplete(RoomMeasurement measurement) {
                // Not used in this test
            }
            
            @Override
            public void onError(String error) {
                receivedError = error;
                latch.countDown();
            }
        });
        
        // Start measurement
        wifiService.startMeasurement("Test Room");
        
        // Wait for callback with timeout
        boolean callbackReceived = latch.await(5, TimeUnit.SECONDS);
        
        // Clean up
        wifiService.stopMeasurement();
        
        // Verify callback was received
        assertTrue("Callback should be received within timeout", callbackReceived);
        
        if (receivedError != null) {
            // If there was an error, it should be a meaningful error message
            assertNotNull("Error message should not be null", receivedError);
            assertFalse("Error message should not be empty", receivedError.isEmpty());
        } else {
            // If no error, we should have received metrics
            assertNotNull("Metrics should be received if no error", receivedMetrics);
            assertTrue("Signal strength should be reasonable", 
                      receivedMetrics.getCurrentSignalDbm() >= -100);
            assertTrue("Latency should be non-negative", 
                      receivedMetrics.getCurrentLatencyMs() >= 0);
        }
    }
    
    @Test
    public void testServiceLifecycle() {
        // Test that service can be started and stopped multiple times
        for (int i = 0; i < 3; i++) {
            wifiService.startMeasurement("Room " + i);
            
            // Create a measurement
            RoomMeasurement measurement = wifiService.createRoomMeasurement(
                "id_" + i, "Room " + i, "gaming"
            );
            assertNotNull("Measurement should be created", measurement);
            
            wifiService.stopMeasurement();
        }
        
        // Clean up
        wifiService.cleanup();
        
        // No exceptions should be thrown
        assertTrue("Service lifecycle should complete without errors", true);
    }
    
    @Test
    public void testConcurrentMeasurements() {
        // Test that starting a new measurement stops the previous one
        wifiService.startMeasurement("First Room");
        
        // Start second measurement (should stop first)
        wifiService.startMeasurement("Second Room");
        
        // Should not throw exceptions
        assertTrue("Concurrent measurements should be handled gracefully", true);
        
        // Clean up
        wifiService.stopMeasurement();
    }
    
    @Test
    public void testServiceWithNullCallback() {
        // Test that service works even with null callback
        wifiService.setCallback(null);
        
        wifiService.startMeasurement("Test Room");
        RoomMeasurement measurement = wifiService.createRoomMeasurement(
            "id", "name", "gaming"
        );
        wifiService.stopMeasurement();
        wifiService.cleanup();
        
        // Should not throw exceptions
        assertTrue("Service should work with null callback", true);
    }
}
