package com.example.hifiwifi.models;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Simple JUnit tests for RoomMeasurement model (no Robolectric)
 */
public class RoomMeasurementSimpleTest {
    
    private RoomMeasurement roomMeasurement;
    private long testTimestamp;
    
    @Before
    public void setUp() {
        roomMeasurement = new RoomMeasurement();
        testTimestamp = System.currentTimeMillis();
    }
    
    @Test
    public void testDefaultConstructor() {
        assertNull(roomMeasurement.getRoomId());
        assertNull(roomMeasurement.getRoomName());
        assertEquals(0, roomMeasurement.getTimestamp());
        assertEquals(0, roomMeasurement.getSignalStrengthDbm());
        assertEquals(0, roomMeasurement.getLatencyMs());
        assertEquals(0.0, roomMeasurement.getBandwidthMbps(), 0.001);
        assertEquals(0.0, roomMeasurement.getJitterMs(), 0.001);
        assertEquals(0.0, roomMeasurement.getPacketLossPercent(), 0.001);
        assertNull(roomMeasurement.getActivityType());
    }
    
    @Test
    public void testParameterizedConstructor() {
        String roomId = "room_001";
        String roomName = "Living Room";
        int signalStrength = -60;
        int latency = 20;
        double bandwidth = 75.5;
        String activityType = "gaming";
        
        RoomMeasurement measurement = new RoomMeasurement(
            roomId, roomName, signalStrength, latency, bandwidth, activityType
        );
        
        assertEquals(roomId, measurement.getRoomId());
        assertEquals(roomName, measurement.getRoomName());
        assertTrue(measurement.getTimestamp() > 0);
        assertEquals(signalStrength, measurement.getSignalStrengthDbm());
        assertEquals(latency, measurement.getLatencyMs());
        assertEquals(bandwidth, measurement.getBandwidthMbps(), 0.001);
        assertEquals(0.0, measurement.getJitterMs(), 0.001); // Default value
        assertEquals(0.0, measurement.getPacketLossPercent(), 0.001); // Default value
        assertEquals(activityType, measurement.getActivityType());
    }
    
    @Test
    public void testFullParameterizedConstructor() {
        String roomId = "room_002";
        String roomName = "Office";
        int signalStrength = -55;
        int latency = 25;
        double bandwidth = 100.0;
        double jitter = 3.2;
        double packetLoss = 1.5;
        String activityType = "streaming";
        
        RoomMeasurement measurement = new RoomMeasurement(
            roomId, roomName, signalStrength, latency, bandwidth, jitter, packetLoss, activityType
        );
        
        assertEquals(roomId, measurement.getRoomId());
        assertEquals(roomName, measurement.getRoomName());
        assertTrue(measurement.getTimestamp() > 0);
        assertEquals(signalStrength, measurement.getSignalStrengthDbm());
        assertEquals(latency, measurement.getLatencyMs());
        assertEquals(bandwidth, measurement.getBandwidthMbps(), 0.001);
        assertEquals(jitter, measurement.getJitterMs(), 0.001);
        assertEquals(packetLoss, measurement.getPacketLossPercent(), 0.001);
        assertEquals(activityType, measurement.getActivityType());
    }
    
    @Test
    public void testTimestampIsSetOnCreation() {
        long beforeCreation = System.currentTimeMillis();
        RoomMeasurement measurement = new RoomMeasurement(
            "id", "name", -50, 30, 100.0, "streaming"
        );
        long afterCreation = System.currentTimeMillis();
        
        assertTrue(measurement.getTimestamp() >= beforeCreation);
        assertTrue(measurement.getTimestamp() <= afterCreation);
    }
    
    @Test
    public void testRoomIdSetterAndGetter() {
        String roomId = "test_room_123";
        roomMeasurement.setRoomId(roomId);
        assertEquals(roomId, roomMeasurement.getRoomId());
    }
    
    @Test
    public void testRoomNameSetterAndGetter() {
        String roomName = "Master Bedroom";
        roomMeasurement.setRoomName(roomName);
        assertEquals(roomName, roomMeasurement.getRoomName());
    }
    
    @Test
    public void testTimestampSetterAndGetter() {
        long timestamp = 1234567890L;
        roomMeasurement.setTimestamp(timestamp);
        assertEquals(timestamp, roomMeasurement.getTimestamp());
    }
    
    @Test
    public void testSignalStrengthSetterAndGetter() {
        int signalStrength = -45;
        roomMeasurement.setSignalStrengthDbm(signalStrength);
        assertEquals(signalStrength, roomMeasurement.getSignalStrengthDbm());
    }
    
    @Test
    public void testLatencySetterAndGetter() {
        int latency = 15;
        roomMeasurement.setLatencyMs(latency);
        assertEquals(latency, roomMeasurement.getLatencyMs());
    }
    
    @Test
    public void testBandwidthSetterAndGetter() {
        double bandwidth = 200.75;
        roomMeasurement.setBandwidthMbps(bandwidth);
        assertEquals(bandwidth, roomMeasurement.getBandwidthMbps(), 0.001);
    }
    
    @Test
    public void testActivityTypeSetterAndGetter() {
        String activityType = "video_call";
        roomMeasurement.setActivityType(activityType);
        assertEquals(activityType, roomMeasurement.getActivityType());
    }
    
    @Test
    public void testAllActivityTypes() {
        String[] activityTypes = {"gaming", "streaming", "video_call", "general"};
        
        for (String activityType : activityTypes) {
            roomMeasurement.setActivityType(activityType);
            assertEquals(activityType, roomMeasurement.getActivityType());
        }
    }
    
    @Test
    public void testNegativeSignalStrength() {
        int negativeSignal = -100;
        roomMeasurement.setSignalStrengthDbm(negativeSignal);
        assertEquals(negativeSignal, roomMeasurement.getSignalStrengthDbm());
    }
    
    @Test
    public void testZeroValues() {
        roomMeasurement.setRoomId("");
        roomMeasurement.setRoomName("");
        roomMeasurement.setTimestamp(0);
        roomMeasurement.setSignalStrengthDbm(0);
        roomMeasurement.setLatencyMs(0);
        roomMeasurement.setBandwidthMbps(0.0);
        roomMeasurement.setActivityType("");
        
        assertEquals("", roomMeasurement.getRoomId());
        assertEquals("", roomMeasurement.getRoomName());
        assertEquals(0, roomMeasurement.getTimestamp());
        assertEquals(0, roomMeasurement.getSignalStrengthDbm());
        assertEquals(0, roomMeasurement.getLatencyMs());
        assertEquals(0.0, roomMeasurement.getBandwidthMbps(), 0.001);
        assertEquals("", roomMeasurement.getActivityType());
    }
    
    @Test
    public void testHighBandwidthValue() {
        double highBandwidth = 1000.0;
        roomMeasurement.setBandwidthMbps(highBandwidth);
        assertEquals(highBandwidth, roomMeasurement.getBandwidthMbps(), 0.001);
    }
    
    @Test
    public void testNullValues() {
        roomMeasurement.setRoomId(null);
        roomMeasurement.setRoomName(null);
        roomMeasurement.setActivityType(null);
        
        assertNull(roomMeasurement.getRoomId());
        assertNull(roomMeasurement.getRoomName());
        assertNull(roomMeasurement.getActivityType());
    }
    
    @Test
    public void testJitterSetterAndGetter() {
        double jitter = 4.2;
        roomMeasurement.setJitterMs(jitter);
        assertEquals(jitter, roomMeasurement.getJitterMs(), 0.001);
    }
    
    @Test
    public void testPacketLossSetterAndGetter() {
        double packetLoss = 2.3;
        roomMeasurement.setPacketLossPercent(packetLoss);
        assertEquals(packetLoss, roomMeasurement.getPacketLossPercent(), 0.001);
    }
    
    @Test
    public void testJitterRanges() {
        double[] jitterValues = {0.1, 1.0, 5.0, 10.0, 25.0};
        
        for (double jitter : jitterValues) {
            roomMeasurement.setJitterMs(jitter);
            assertEquals(jitter, roomMeasurement.getJitterMs(), 0.001);
        }
    }
    
    @Test
    public void testPacketLossRanges() {
        double[] packetLossValues = {0.0, 0.1, 1.0, 5.0, 10.0, 50.0, 100.0};
        
        for (double packetLoss : packetLossValues) {
            roomMeasurement.setPacketLossPercent(packetLoss);
            assertEquals(packetLoss, roomMeasurement.getPacketLossPercent(), 0.001);
        }
    }
}
