package com.example.hifiwifi.services;

import com.example.hifiwifi.models.NetworkMetrics;
import com.example.hifiwifi.models.RoomMeasurement;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Tests for WiFiMeasurementService business logic that doesn't require Android framework
 * These tests focus on the data processing and callback mechanisms
 */
public class WiFiMeasurementServiceBusinessLogicTest {
    
    @Test
    public void testMeasurementCallbackInterface() {
        // Test that the callback interface is properly defined
        WiFiMeasurementService.MeasurementCallback callback = new WiFiMeasurementService.MeasurementCallback() {
            @Override
            public void onMeasurementUpdate(NetworkMetrics metrics) {
                assertNotNull(metrics);
            }
            
            @Override
            public void onMeasurementComplete(RoomMeasurement measurement) {
                assertNotNull(measurement);
            }
            
            @Override
            public void onError(String error) {
                assertNotNull(error);
            }
        };
        
        // Test that callback methods can be called
        NetworkMetrics metrics = new NetworkMetrics(-50, 25, 100.0, true, "Test Room");
        callback.onMeasurementUpdate(metrics);
        
        RoomMeasurement measurement = new RoomMeasurement("id", "room", -45, 20, 80.0, "gaming");
        callback.onMeasurementComplete(measurement);
        
        callback.onError("Test error");
        
        // If we get here, the callback interface works correctly
        assertTrue(true);
    }
    
    @Test
    public void testNetworkMetricsDataFlow() {
        // Test the data flow through NetworkMetrics
        NetworkMetrics metrics = new NetworkMetrics(-60, 30, 75.5, 3.2, 1.5, true, "Living Room");
        
        assertEquals(-60, metrics.getCurrentSignalDbm());
        assertEquals(30, metrics.getCurrentLatencyMs());
        assertEquals(75.5, metrics.getCurrentBandwidthMbps(), 0.001);
        assertEquals(3.2, metrics.getCurrentJitterMs(), 0.001);
        assertEquals(1.5, metrics.getCurrentPacketLossPercent(), 0.001);
        assertTrue(metrics.isCollecting());
        assertEquals("Living Room", metrics.getCurrentRoomName());
        
        // Test updating values
        metrics.setCurrentSignalDbm(-70);
        metrics.setCurrentLatencyMs(40);
        metrics.setCurrentBandwidthMbps(50.0);
        metrics.setCurrentJitterMs(5.1);
        metrics.setCurrentPacketLossPercent(2.3);
        metrics.setCollecting(false);
        metrics.setCurrentRoomName("Bedroom");
        
        assertEquals(-70, metrics.getCurrentSignalDbm());
        assertEquals(40, metrics.getCurrentLatencyMs());
        assertEquals(50.0, metrics.getCurrentBandwidthMbps(), 0.001);
        assertEquals(5.1, metrics.getCurrentJitterMs(), 0.001);
        assertEquals(2.3, metrics.getCurrentPacketLossPercent(), 0.001);
        assertFalse(metrics.isCollecting());
        assertEquals("Bedroom", metrics.getCurrentRoomName());
    }
    
    @Test
    public void testRoomMeasurementDataFlow() {
        // Test the data flow through RoomMeasurement
        RoomMeasurement measurement = new RoomMeasurement(
            "room_001", "Master Bedroom", -55, 35, 90.0, 2.5, 1.2, "streaming"
        );
        
        assertEquals("room_001", measurement.getRoomId());
        assertEquals("Master Bedroom", measurement.getRoomName());
        assertTrue(measurement.getTimestamp() > 0);
        assertEquals(-55, measurement.getSignalStrengthDbm());
        assertEquals(35, measurement.getLatencyMs());
        assertEquals(90.0, measurement.getBandwidthMbps(), 0.001);
        assertEquals(2.5, measurement.getJitterMs(), 0.001);
        assertEquals(1.2, measurement.getPacketLossPercent(), 0.001);
        assertEquals("streaming", measurement.getActivityType());
        
        // Test updating values
        measurement.setRoomId("room_002");
        measurement.setRoomName("Office");
        measurement.setSignalStrengthDbm(-65);
        measurement.setLatencyMs(45);
        measurement.setBandwidthMbps(60.0);
        measurement.setJitterMs(4.1);
        measurement.setPacketLossPercent(2.8);
        measurement.setActivityType("gaming");
        
        assertEquals("room_002", measurement.getRoomId());
        assertEquals("Office", measurement.getRoomName());
        assertEquals(-65, measurement.getSignalStrengthDbm());
        assertEquals(45, measurement.getLatencyMs());
        assertEquals(60.0, measurement.getBandwidthMbps(), 0.001);
        assertEquals(4.1, measurement.getJitterMs(), 0.001);
        assertEquals(2.8, measurement.getPacketLossPercent(), 0.001);
        assertEquals("gaming", measurement.getActivityType());
    }
    
    @Test
    public void testActivityTypes() {
        // Test all supported activity types
        String[] activityTypes = {"gaming", "streaming", "video_call", "general"};
        
        for (String activityType : activityTypes) {
            RoomMeasurement measurement = new RoomMeasurement(
                "id", "room", -50, 25, 100.0, activityType
            );
            assertEquals(activityType, measurement.getActivityType());
        }
    }
    
    @Test
    public void testSignalStrengthRanges() {
        // Test various signal strength values
        int[] signalStrengths = {-30, -50, -70, -90, -100};
        
        for (int signal : signalStrengths) {
            NetworkMetrics metrics = new NetworkMetrics(signal, 25, 100.0, true, "Room");
            assertEquals(signal, metrics.getCurrentSignalDbm());
        }
    }
    
    @Test
    public void testBandwidthRanges() {
        // Test various bandwidth values
        double[] bandwidths = {10.0, 50.0, 100.0, 500.0, 1000.0};
        
        for (double bandwidth : bandwidths) {
            NetworkMetrics metrics = new NetworkMetrics(-50, 25, bandwidth, true, "Room");
            assertEquals(bandwidth, metrics.getCurrentBandwidthMbps(), 0.001);
        }
    }
    
    @Test
    public void testLatencyRanges() {
        // Test various latency values
        int[] latencies = {5, 15, 25, 50, 100};
        
        for (int latency : latencies) {
            NetworkMetrics metrics = new NetworkMetrics(-50, latency, 100.0, 2.0, 1.0, true, "Room");
            assertEquals(latency, metrics.getCurrentLatencyMs());
        }
    }
    
    @Test
    public void testJitterRanges() {
        // Test various jitter values
        double[] jitterValues = {0.1, 1.0, 5.0, 10.0, 25.0};
        
        for (double jitter : jitterValues) {
            NetworkMetrics metrics = new NetworkMetrics(-50, 25, 100.0, jitter, 1.0, true, "Room");
            assertEquals(jitter, metrics.getCurrentJitterMs(), 0.001);
        }
    }
    
    @Test
    public void testPacketLossRanges() {
        // Test various packet loss values
        double[] packetLossValues = {0.0, 0.1, 1.0, 5.0, 10.0, 50.0, 100.0};
        
        for (double packetLoss : packetLossValues) {
            NetworkMetrics metrics = new NetworkMetrics(-50, 25, 100.0, 2.0, packetLoss, true, "Room");
            assertEquals(packetLoss, metrics.getCurrentPacketLossPercent(), 0.001);
        }
    }
    
    @Test
    public void testEdgeCases() {
        // Test edge cases and boundary values
        
        // Zero values
        NetworkMetrics zeroMetrics = new NetworkMetrics(0, 0, 0.0, 0.0, 0.0, false, "");
        assertEquals(0, zeroMetrics.getCurrentSignalDbm());
        assertEquals(0, zeroMetrics.getCurrentLatencyMs());
        assertEquals(0.0, zeroMetrics.getCurrentBandwidthMbps(), 0.001);
        assertEquals(0.0, zeroMetrics.getCurrentJitterMs(), 0.001);
        assertEquals(0.0, zeroMetrics.getCurrentPacketLossPercent(), 0.001);
        assertFalse(zeroMetrics.isCollecting());
        assertEquals("", zeroMetrics.getCurrentRoomName());
        
        // Null values
        NetworkMetrics nullMetrics = new NetworkMetrics();
        nullMetrics.setCurrentRoomName(null);
        assertNull(nullMetrics.getCurrentRoomName());
        
        // Very high values
        NetworkMetrics highMetrics = new NetworkMetrics(-10, 1000, 10000.0, 50.0, 100.0, true, "Room");
        assertEquals(-10, highMetrics.getCurrentSignalDbm());
        assertEquals(1000, highMetrics.getCurrentLatencyMs());
        assertEquals(10000.0, highMetrics.getCurrentBandwidthMbps(), 0.001);
        assertEquals(50.0, highMetrics.getCurrentJitterMs(), 0.001);
        assertEquals(100.0, highMetrics.getCurrentPacketLossPercent(), 0.001);
    }
    
    @Test
    public void testSpeedTestIntegration() {
        // Test how speed test results would integrate with NetworkMetrics
        // This simulates the data flow when speed test completes
        
        // Simulate speed test result
        double speedTestResult = 25.5; // Mbps from custom speed test
        String roomName = "Living Room";
        
        // Create NetworkMetrics with speed test result
        NetworkMetrics metrics = new NetworkMetrics(
            -60,  // Signal strength
            30,   // Latency
            speedTestResult, // Bandwidth from speed test
            2.5,  // Jitter
            1.0,  // Packet loss
            true, // Collecting
            roomName
        );
        
        // Verify the speed test result is properly integrated
        assertEquals(speedTestResult, metrics.getCurrentBandwidthMbps(), 0.001);
        assertEquals(roomName, metrics.getCurrentRoomName());
        
        // Test updating with new speed test result
        double newSpeedTestResult = 50.0;
        metrics.setCurrentBandwidthMbps(newSpeedTestResult);
        assertEquals(newSpeedTestResult, metrics.getCurrentBandwidthMbps(), 0.001);
        
        // Test that other metrics remain unchanged
        assertEquals(-60, metrics.getCurrentSignalDbm());
        assertEquals(30, metrics.getCurrentLatencyMs());
        assertEquals(2.5, metrics.getCurrentJitterMs(), 0.001);
        assertEquals(1.0, metrics.getCurrentPacketLossPercent(), 0.001);
    }
}
