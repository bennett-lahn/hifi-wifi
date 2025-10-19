package com.example.hifiwifi.models;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Simple JUnit tests for NetworkMetrics model (no Robolectric)
 */
public class NetworkMetricsSimpleTest {
    
    private NetworkMetrics networkMetrics;
    
    @Before
    public void setUp() {
        networkMetrics = new NetworkMetrics();
    }
    
    @Test
    public void testDefaultConstructor() {
        assertEquals(0, networkMetrics.getCurrentSignalDbm());
        assertEquals(0, networkMetrics.getCurrentLatencyMs());
        assertEquals(0.0, networkMetrics.getCurrentBandwidthMbps(), 0.001);
        assertEquals(0.0, networkMetrics.getCurrentJitterMs(), 0.001);
        assertEquals(0.0, networkMetrics.getCurrentPacketLossPercent(), 0.001);
        assertFalse(networkMetrics.isCollecting());
        assertEquals("", networkMetrics.getCurrentRoomName());
    }
    
    @Test
    public void testParameterizedConstructor() {
        int signalDbm = -50;
        int latencyMs = 25;
        double bandwidthMbps = 100.5;
        boolean isCollecting = true;
        String roomName = "Living Room";
        
        NetworkMetrics metrics = new NetworkMetrics(signalDbm, latencyMs, bandwidthMbps, isCollecting, roomName);
        
        assertEquals(signalDbm, metrics.getCurrentSignalDbm());
        assertEquals(latencyMs, metrics.getCurrentLatencyMs());
        assertEquals(bandwidthMbps, metrics.getCurrentBandwidthMbps(), 0.001);
        assertEquals(0.0, metrics.getCurrentJitterMs(), 0.001); // Default value
        assertEquals(0.0, metrics.getCurrentPacketLossPercent(), 0.001); // Default value
        assertTrue(metrics.isCollecting());
        assertEquals(roomName, metrics.getCurrentRoomName());
    }
    
    @Test
    public void testFullParameterizedConstructor() {
        int signalDbm = -60;
        int latencyMs = 30;
        double bandwidthMbps = 75.5;
        double jitterMs = 5.2;
        double packetLossPercent = 2.1;
        boolean isCollecting = true;
        String roomName = "Office";
        
        NetworkMetrics metrics = new NetworkMetrics(signalDbm, latencyMs, bandwidthMbps, jitterMs, packetLossPercent, isCollecting, roomName);
        
        assertEquals(signalDbm, metrics.getCurrentSignalDbm());
        assertEquals(latencyMs, metrics.getCurrentLatencyMs());
        assertEquals(bandwidthMbps, metrics.getCurrentBandwidthMbps(), 0.001);
        assertEquals(jitterMs, metrics.getCurrentJitterMs(), 0.001);
        assertEquals(packetLossPercent, metrics.getCurrentPacketLossPercent(), 0.001);
        assertTrue(metrics.isCollecting());
        assertEquals(roomName, metrics.getCurrentRoomName());
    }
    
    @Test
    public void testSignalStrengthSetterAndGetter() {
        int signalStrength = -75;
        networkMetrics.setCurrentSignalDbm(signalStrength);
        assertEquals(signalStrength, networkMetrics.getCurrentSignalDbm());
    }
    
    @Test
    public void testLatencySetterAndGetter() {
        int latency = 30;
        networkMetrics.setCurrentLatencyMs(latency);
        assertEquals(latency, networkMetrics.getCurrentLatencyMs());
    }
    
    @Test
    public void testBandwidthSetterAndGetter() {
        double bandwidth = 50.25;
        networkMetrics.setCurrentBandwidthMbps(bandwidth);
        assertEquals(bandwidth, networkMetrics.getCurrentBandwidthMbps(), 0.001);
    }
    
    @Test
    public void testCollectingSetterAndGetter() {
        networkMetrics.setCollecting(true);
        assertTrue(networkMetrics.isCollecting());
        
        networkMetrics.setCollecting(false);
        assertFalse(networkMetrics.isCollecting());
    }
    
    @Test
    public void testRoomNameSetterAndGetter() {
        String roomName = "Bedroom";
        networkMetrics.setCurrentRoomName(roomName);
        assertEquals(roomName, networkMetrics.getCurrentRoomName());
    }
    
    @Test
    public void testNegativeSignalStrength() {
        int negativeSignal = -100;
        networkMetrics.setCurrentSignalDbm(negativeSignal);
        assertEquals(negativeSignal, networkMetrics.getCurrentSignalDbm());
    }
    
    @Test
    public void testZeroValues() {
        NetworkMetrics metrics = new NetworkMetrics(0, 0, 0.0, false, "");
        
        assertEquals(0, metrics.getCurrentSignalDbm());
        assertEquals(0, metrics.getCurrentLatencyMs());
        assertEquals(0.0, metrics.getCurrentBandwidthMbps(), 0.001);
        assertFalse(metrics.isCollecting());
        assertEquals("", metrics.getCurrentRoomName());
    }
    
    @Test
    public void testHighBandwidthValue() {
        double highBandwidth = 1000.0;
        networkMetrics.setCurrentBandwidthMbps(highBandwidth);
        assertEquals(highBandwidth, networkMetrics.getCurrentBandwidthMbps(), 0.001);
    }
    
    @Test
    public void testNullRoomName() {
        networkMetrics.setCurrentRoomName(null);
        assertNull(networkMetrics.getCurrentRoomName());
    }
    
    @Test
    public void testJitterSetterAndGetter() {
        double jitter = 3.5;
        networkMetrics.setCurrentJitterMs(jitter);
        assertEquals(jitter, networkMetrics.getCurrentJitterMs(), 0.001);
    }
    
    @Test
    public void testPacketLossSetterAndGetter() {
        double packetLoss = 1.2;
        networkMetrics.setCurrentPacketLossPercent(packetLoss);
        assertEquals(packetLoss, networkMetrics.getCurrentPacketLossPercent(), 0.001);
    }
    
    @Test
    public void testJitterRanges() {
        double[] jitterValues = {0.1, 1.0, 5.0, 10.0, 50.0};
        
        for (double jitter : jitterValues) {
            networkMetrics.setCurrentJitterMs(jitter);
            assertEquals(jitter, networkMetrics.getCurrentJitterMs(), 0.001);
        }
    }
    
    @Test
    public void testPacketLossRanges() {
        double[] packetLossValues = {0.0, 0.1, 1.0, 5.0, 10.0, 50.0, 100.0};
        
        for (double packetLoss : packetLossValues) {
            networkMetrics.setCurrentPacketLossPercent(packetLoss);
            assertEquals(packetLoss, networkMetrics.getCurrentPacketLossPercent(), 0.001);
        }
    }
}
