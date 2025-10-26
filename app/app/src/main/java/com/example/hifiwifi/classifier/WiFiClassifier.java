package com.example.hifiwifi.classifier;

import com.example.hifiwifi.models.NetworkMetrics;
import com.example.hifiwifi.models.RoomMeasurement;

/**
 * Main classifier for WiFi performance based on network metrics and activity type
 */
public class WiFiClassifier {
    
    // Classification thresholds for different metrics
    private static final int EXCELLENT_SIGNAL_DBM = -30;
    private static final int GOOD_SIGNAL_DBM = -50;
    private static final int OKAY_SIGNAL_DBM = -65;
    private static final int BAD_SIGNAL_DBM = -80;
    
    private static final int EXCELLENT_LATENCY_MS = 20;
    private static final int GOOD_LATENCY_MS = 50;
    private static final int OKAY_LATENCY_MS = 100;
    private static final int BAD_LATENCY_MS = 200;
    
    private static final double EXCELLENT_BANDWIDTH_MBPS = 100.0;
    private static final double GOOD_BANDWIDTH_MBPS = 50.0;
    private static final double OKAY_BANDWIDTH_MBPS = 25.0;
    private static final double BAD_BANDWIDTH_MBPS = 10.0;
    
    private static final double EXCELLENT_JITTER_MS = 5.0;
    private static final double GOOD_JITTER_MS = 10.0;
    private static final double OKAY_JITTER_MS = 20.0;
    private static final double BAD_JITTER_MS = 50.0;
    
    private static final double EXCELLENT_PACKET_LOSS_PERCENT = 0.1;
    private static final double GOOD_PACKET_LOSS_PERCENT = 0.5;
    private static final double OKAY_PACKET_LOSS_PERCENT = 1.0;
    private static final double BAD_PACKET_LOSS_PERCENT = 2.0;
    
    /**
     * Classify individual WiFi metrics
     */
    public MetricClassification classifyMetrics(NetworkMetrics metrics) {
        return new MetricClassification(
            classifySignalStrength(metrics.getCurrentSignalDbm()),
            classifyLatency(metrics.getCurrentLatencyMs()),
            classifyBandwidth(metrics.getCurrentBandwidthMbps()),
            classifyJitter(metrics.getCurrentJitterMs()),
            classifyPacketLoss(metrics.getCurrentPacketLossPercent())
        );
    }
    
    /**
     * Classify individual WiFi metrics from RoomMeasurement
     */
    public MetricClassification classifyMetrics(RoomMeasurement measurement) {
        return new MetricClassification(
            classifySignalStrength(measurement.getSignalStrengthDbm()),
            classifyLatency(measurement.getLatencyMs()),
            classifyBandwidth(measurement.getBandwidthMbps()),
            classifyJitter(measurement.getJitterMs()),
            classifyPacketLoss(measurement.getPacketLossPercent())
        );
    }
    
    /**
     * Classify signal strength based on dBm values
     */
    private WiFiClassification classifySignalStrength(int signalDbm) {
        if (signalDbm >= EXCELLENT_SIGNAL_DBM) {
            return WiFiClassification.EXCELLENT;
        } else if (signalDbm >= GOOD_SIGNAL_DBM) {
            return WiFiClassification.GOOD;
        } else if (signalDbm >= OKAY_SIGNAL_DBM) {
            return WiFiClassification.OKAY;
        } else if (signalDbm >= BAD_SIGNAL_DBM) {
            return WiFiClassification.BAD;
        } else {
            return WiFiClassification.MARGINAL;
        }
    }
    
    /**
     * Classify latency based on milliseconds
     */
    private WiFiClassification classifyLatency(int latencyMs) {
        if (latencyMs <= EXCELLENT_LATENCY_MS) {
            return WiFiClassification.EXCELLENT;
        } else if (latencyMs <= GOOD_LATENCY_MS) {
            return WiFiClassification.GOOD;
        } else if (latencyMs <= OKAY_LATENCY_MS) {
            return WiFiClassification.OKAY;
        } else if (latencyMs <= BAD_LATENCY_MS) {
            return WiFiClassification.BAD;
        } else {
            return WiFiClassification.MARGINAL;
        }
    }
    
    /**
     * Classify bandwidth based on Mbps
     */
    private WiFiClassification classifyBandwidth(double bandwidthMbps) {
        if (bandwidthMbps >= EXCELLENT_BANDWIDTH_MBPS) {
            return WiFiClassification.EXCELLENT;
        } else if (bandwidthMbps >= GOOD_BANDWIDTH_MBPS) {
            return WiFiClassification.GOOD;
        } else if (bandwidthMbps >= OKAY_BANDWIDTH_MBPS) {
            return WiFiClassification.OKAY;
        } else if (bandwidthMbps >= BAD_BANDWIDTH_MBPS) {
            return WiFiClassification.BAD;
        } else {
            return WiFiClassification.MARGINAL;
        }
    }
    
    /**
     * Classify jitter based on milliseconds
     */
    private WiFiClassification classifyJitter(double jitterMs) {
        if (jitterMs <= EXCELLENT_JITTER_MS) {
            return WiFiClassification.EXCELLENT;
        } else if (jitterMs <= GOOD_JITTER_MS) {
            return WiFiClassification.GOOD;
        } else if (jitterMs <= OKAY_JITTER_MS) {
            return WiFiClassification.OKAY;
        } else if (jitterMs <= BAD_JITTER_MS) {
            return WiFiClassification.BAD;
        } else {
            return WiFiClassification.MARGINAL;
        }
    }
    
    /**
     * Classify packet loss based on percentage
     */
    private WiFiClassification classifyPacketLoss(double packetLossPercent) {
        if (packetLossPercent <= EXCELLENT_PACKET_LOSS_PERCENT) {
            return WiFiClassification.EXCELLENT;
        } else if (packetLossPercent <= GOOD_PACKET_LOSS_PERCENT) {
            return WiFiClassification.GOOD;
        } else if (packetLossPercent <= OKAY_PACKET_LOSS_PERCENT) {
            return WiFiClassification.OKAY;
        } else if (packetLossPercent <= BAD_PACKET_LOSS_PERCENT) {
            return WiFiClassification.BAD;
        } else {
            return WiFiClassification.MARGINAL;
        }
    }
    
    /**
     * Calculate weighted overall classification based on activity importance
     */
    public WiFiClassification calculateWeightedClassification(MetricClassification metricClassification, 
                                                           ActivityImportance activityImportance) {
        double weightedScore = 0.0;
        double totalWeight = 0.0;
        
        // Calculate weighted score for each metric
        weightedScore += metricClassification.getSignalStrengthClassification().getScore() * activityImportance.getSignalStrengthWeight();
        totalWeight += activityImportance.getSignalStrengthWeight();
        
        weightedScore += metricClassification.getLatencyClassification().getScore() * activityImportance.getLatencyWeight();
        totalWeight += activityImportance.getLatencyWeight();
        
        weightedScore += metricClassification.getBandwidthClassification().getScore() * activityImportance.getBandwidthWeight();
        totalWeight += activityImportance.getBandwidthWeight();
        
        weightedScore += metricClassification.getJitterClassification().getScore() * activityImportance.getJitterWeight();
        totalWeight += activityImportance.getJitterWeight();
        
        weightedScore += metricClassification.getPacketLossClassification().getScore() * activityImportance.getPacketLossWeight();
        totalWeight += activityImportance.getPacketLossWeight();
        
        // Normalize the score
        double normalizedScore = weightedScore / totalWeight;
        
        // Convert to classification
        if (normalizedScore >= 4.5) {
            return WiFiClassification.EXCELLENT;
        } else if (normalizedScore >= 3.5) {
            return WiFiClassification.GOOD;
        } else if (normalizedScore >= 2.5) {
            return WiFiClassification.OKAY;
        } else if (normalizedScore >= 1.5) {
            return WiFiClassification.BAD;
        } else {
            return WiFiClassification.MARGINAL;
        }
    }
    
    /**
     * Get the most critical metric for the given activity
     */
    public String getMostCriticalMetric(MetricClassification metricClassification, 
                                      ActivityImportance activityImportance) {
        String mostImportantMetric = activityImportance.getMostImportantMetric();
        WiFiClassification worstClassification = metricClassification.getWorstClassification();
        
        // Check which metric has the worst classification among the important ones
        WiFiClassification worstImportantClassification = WiFiClassification.EXCELLENT;
        String criticalMetric = mostImportantMetric;
        
        if (mostImportantMetric.equals("signal_strength") && 
            metricClassification.getSignalStrengthClassification().getScore() < worstImportantClassification.getScore()) {
            worstImportantClassification = metricClassification.getSignalStrengthClassification();
            criticalMetric = "signal_strength";
        }
        if (mostImportantMetric.equals("latency") && 
            metricClassification.getLatencyClassification().getScore() < worstImportantClassification.getScore()) {
            worstImportantClassification = metricClassification.getLatencyClassification();
            criticalMetric = "latency";
        }
        if (mostImportantMetric.equals("bandwidth") && 
            metricClassification.getBandwidthClassification().getScore() < worstImportantClassification.getScore()) {
            worstImportantClassification = metricClassification.getBandwidthClassification();
            criticalMetric = "bandwidth";
        }
        if (mostImportantMetric.equals("jitter") && 
            metricClassification.getJitterClassification().getScore() < worstImportantClassification.getScore()) {
            worstImportantClassification = metricClassification.getJitterClassification();
            criticalMetric = "jitter";
        }
        if (mostImportantMetric.equals("packet_loss") && 
            metricClassification.getPacketLossClassification().getScore() < worstImportantClassification.getScore()) {
            worstImportantClassification = metricClassification.getPacketLossClassification();
            criticalMetric = "packet_loss";
        }
        
        return criticalMetric;
    }
}
