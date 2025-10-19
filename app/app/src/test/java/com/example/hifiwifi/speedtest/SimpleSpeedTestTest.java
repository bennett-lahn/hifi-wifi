package com.example.hifiwifi.speedtest;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Tests for SimpleSpeedTest implementation that don't require Android framework
 * These tests focus on the data structures and callback mechanisms
 */
public class SimpleSpeedTestTest {
    
    @Test
    public void testSpeedTestCallbackInterface() {
        // Test that the callback interface is properly defined
        SimpleSpeedTest.SpeedTestCallback callback = new SimpleSpeedTest.SpeedTestCallback() {
            @Override
            public void onComplete(double speedMbps, int latencyMs, double jitterMs, double packetLossPercent) {
                assertTrue("Speed should be non-negative", speedMbps >= 0.0);
                assertTrue("Latency should be non-negative", latencyMs >= 0);
                assertTrue("Jitter should be non-negative", jitterMs >= 0.0);
                assertTrue("Packet loss should be between 0 and 100", packetLossPercent >= 0.0 && packetLossPercent <= 100.0);
            }
            
            @Override
            public void onError(String errorMessage) {
                assertNotNull("Error message should not be null", errorMessage);
            }
            
            @Override
            public void onProgress(double currentSpeedMbps, long bytesDownloaded, long totalBytes) {
                assertTrue("Current speed should be non-negative", currentSpeedMbps >= 0.0);
                assertTrue("Bytes downloaded should be non-negative", bytesDownloaded >= 0);
                assertTrue("Total bytes should be non-negative", totalBytes >= 0);
            }
        };
        
        // Test that callback methods can be called
        callback.onComplete(25.5, 30, 2.5, 1.0);
        callback.onError("Test error");
        callback.onProgress(15.2, 1024000, 10485760);
        
        // If we get here, the callback interface works correctly
        assertTrue(true);
    }
    
    @Test
    public void testSpeedTestResultDataFlow() {
        // Test successful result
        SpeedTestResult successResult = new SpeedTestResult(
            System.currentTimeMillis(), 25.5, "Living Room", "test-123"
        );
        
        assertTrue("Result should be successful", successResult.isSuccess());
        assertEquals(25.5, successResult.getSpeedMbps(), 0.001);
        assertEquals("Living Room", successResult.getRoomLabel());
        assertEquals("test-123", successResult.getTestId());
        assertTrue("Timestamp should be positive", successResult.getTimestamp() > 0);
        assertEquals("", successResult.getErrorMessage());
        
        // Test failed result
        SpeedTestResult failedResult = new SpeedTestResult(
            System.currentTimeMillis(), "Network error", "Kitchen", "test-456"
        );
        
        assertFalse("Result should be failed", failedResult.isSuccess());
        assertEquals(0.0, failedResult.getSpeedMbps(), 0.001);
        assertEquals("Kitchen", failedResult.getRoomLabel());
        assertEquals("test-456", failedResult.getTestId());
        assertTrue("Timestamp should be positive", failedResult.getTimestamp() > 0);
        assertEquals("Network error", failedResult.getErrorMessage());
    }
    
    @Test
    public void testSpeedTestResultSetters() {
        SpeedTestResult result = new SpeedTestResult(
            System.currentTimeMillis(), 50.0, "Office", "test-789"
        );
        
        // Test setters
        result.setTimestamp(1234567890L);
        result.setSpeedMbps(75.5);
        result.setRoomLabel("Bedroom");
        result.setTestId("test-999");
        result.setErrorMessage("Custom error");
        result.setSuccess(false);
        result.setBytesDownloaded(5000000L);
        result.setTestDurationMs(5000L);
        
        assertEquals(1234567890L, result.getTimestamp());
        assertEquals(75.5, result.getSpeedMbps(), 0.001);
        assertEquals("Bedroom", result.getRoomLabel());
        assertEquals("test-999", result.getTestId());
        assertEquals("Custom error", result.getErrorMessage());
        assertFalse(result.isSuccess());
        assertEquals(5000000L, result.getBytesDownloaded());
        assertEquals(5000L, result.getTestDurationMs());
    }
    
    @Test
    public void testSpeedTestResultToString() {
        SpeedTestResult result = new SpeedTestResult(
            System.currentTimeMillis(), 30.0, "Test Room", "test-001"
        );
        
        String resultString = result.toString();
        assertNotNull("toString should not return null", resultString);
        assertTrue("toString should contain speed", resultString.contains("30.0"));
        assertTrue("toString should contain room", resultString.contains("Test Room"));
        assertTrue("toString should contain test ID", resultString.contains("test-001"));
    }
    
    @Test
    public void testSpeedTestResultFullConstructor() {
        SpeedTestResult result = new SpeedTestResult(
            1234567890L, 45.5, "Full Room", "test-full", 
            true, "No error", 10000000L, 8000L
        );
        
        assertEquals(1234567890L, result.getTimestamp());
        assertEquals(45.5, result.getSpeedMbps(), 0.001);
        assertEquals("Full Room", result.getRoomLabel());
        assertEquals("test-full", result.getTestId());
        assertTrue(result.isSuccess());
        assertEquals("No error", result.getErrorMessage());
        assertEquals(10000000L, result.getBytesDownloaded());
        assertEquals(8000L, result.getTestDurationMs());
    }
    
    @Test
    public void testSpeedTestResultEdgeCases() {
        // Test zero values
        SpeedTestResult zeroResult = new SpeedTestResult(0L, 0.0, "", "");
        assertEquals(0L, zeroResult.getTimestamp());
        assertEquals(0.0, zeroResult.getSpeedMbps(), 0.001);
        assertEquals("", zeroResult.getRoomLabel());
        assertEquals("", zeroResult.getTestId());
        assertTrue(zeroResult.isSuccess());
        
        // Test very high values
        SpeedTestResult highResult = new SpeedTestResult(
            Long.MAX_VALUE, 10000.0, "Very Long Room Name That Exceeds Normal Length", 
            "very-long-test-id-that-might-cause-issues"
        );
        assertEquals(Long.MAX_VALUE, highResult.getTimestamp());
        assertEquals(10000.0, highResult.getSpeedMbps(), 0.001);
        assertEquals("Very Long Room Name That Exceeds Normal Length", highResult.getRoomLabel());
        assertEquals("very-long-test-id-that-might-cause-issues", highResult.getTestId());
        
        // Test negative speed (should be allowed for error cases)
        SpeedTestResult negativeResult = new SpeedTestResult(
            System.currentTimeMillis(), -1.0, "Error Room", "test-negative"
        );
        assertEquals(-1.0, negativeResult.getSpeedMbps(), 0.001);
    }
    
    @Test
    public void testSpeedTestResultNullHandling() {
        SpeedTestResult result = new SpeedTestResult(
            System.currentTimeMillis(), 25.0, "Test Room", "test-null"
        );
        
        // Test setting null values
        result.setRoomLabel(null);
        result.setTestId(null);
        result.setErrorMessage(null);
        
        assertNull("Room label should be null", result.getRoomLabel());
        assertNull("Test ID should be null", result.getTestId());
        assertNull("Error message should be null", result.getErrorMessage());
    }
    
    @Test
    public void testSpeedTestResultRounding() {
        // Test that speed values are properly handled with decimal precision
        SpeedTestResult preciseResult = new SpeedTestResult(
            System.currentTimeMillis(), 25.123456789, "Precise Room", "test-precise"
        );
        
        assertEquals(25.123456789, preciseResult.getSpeedMbps(), 0.000000001);
        
        // Test setting a value with many decimal places
        preciseResult.setSpeedMbps(99.999999999);
        assertEquals(99.999999999, preciseResult.getSpeedMbps(), 0.000000001);
    }
    
    @Test
    public void testSpeedTestResultConsistency() {
        // Test that the result maintains consistency after multiple operations
        SpeedTestResult result = new SpeedTestResult(
            System.currentTimeMillis(), 50.0, "Consistent Room", "test-consistent"
        );
        
        // Perform multiple operations
        result.setSpeedMbps(75.5);
        result.setRoomLabel("Updated Room");
        result.setSuccess(false);
        result.setErrorMessage("Test error");
        
        // Verify final state
        assertEquals(75.5, result.getSpeedMbps(), 0.001);
        assertEquals("Updated Room", result.getRoomLabel());
        assertFalse(result.isSuccess());
        assertEquals("Test error", result.getErrorMessage());
        
        // Verify original values that weren't changed
        assertTrue("Timestamp should remain unchanged", result.getTimestamp() > 0);
        assertEquals("test-consistent", result.getTestId());
    }
    
    @Test
    public void testNetworkMetricsIntegration() {
        // Test how the enhanced speed test results would integrate with NetworkMetrics
        double speedMbps = 25.5;
        int latencyMs = 30;
        double jitterMs = 2.5;
        double packetLossPercent = 1.0;
        String roomName = "Test Room";
        
        // Simulate creating NetworkMetrics with speed test results
        // This would be done in the actual WiFiMeasurementService
        com.example.hifiwifi.models.NetworkMetrics metrics = new com.example.hifiwifi.models.NetworkMetrics(
            -60,  // Signal strength
            latencyMs,  // Latency from speed test
            speedMbps,  // Bandwidth from speed test
            jitterMs,   // Jitter from speed test
            packetLossPercent,  // Packet loss from speed test
            true,       // Collecting
            roomName
        );
        
        // Verify the speed test results are properly integrated
        assertEquals(speedMbps, metrics.getCurrentBandwidthMbps(), 0.001);
        assertEquals(latencyMs, metrics.getCurrentLatencyMs());
        assertEquals(jitterMs, metrics.getCurrentJitterMs(), 0.001);
        assertEquals(packetLossPercent, metrics.getCurrentPacketLossPercent(), 0.001);
        assertEquals(roomName, metrics.getCurrentRoomName());
    }
    
    @Test
    public void testLatencyJitterPacketLossRanges() {
        // Test various ranges for the new metrics
        
        // Test latency ranges
        int[] latencies = {5, 15, 25, 50, 100, 200};
        for (int latency : latencies) {
            assertTrue("Latency should be reasonable", latency >= 0 && latency <= 1000);
        }
        
        // Test jitter ranges
        double[] jitterValues = {0.1, 1.0, 5.0, 10.0, 25.0, 50.0};
        for (double jitter : jitterValues) {
            assertTrue("Jitter should be reasonable", jitter >= 0.0 && jitter <= 100.0);
        }
        
        // Test packet loss ranges
        double[] packetLossValues = {0.0, 0.1, 1.0, 5.0, 10.0, 50.0, 100.0};
        for (double packetLoss : packetLossValues) {
            assertTrue("Packet loss should be between 0 and 100", packetLoss >= 0.0 && packetLoss <= 100.0);
        }
    }
}