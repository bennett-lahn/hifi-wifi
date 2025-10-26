package com.example.hifiwifi.classifier;

/**
 * Class defining the importance weights for different WiFi metrics based on activity type
 */
public class ActivityImportance {
    private String activityType;
    private double signalStrengthWeight;
    private double latencyWeight;
    private double bandwidthWeight;
    private double jitterWeight;
    private double packetLossWeight;
    
    public ActivityImportance() {
        this.activityType = "general";
        this.signalStrengthWeight = 1.0;
        this.latencyWeight = 1.0;
        this.bandwidthWeight = 1.0;
        this.jitterWeight = 1.0;
        this.packetLossWeight = 1.0;
    }
    
    public ActivityImportance(String activityType, double signalStrengthWeight,
                            double latencyWeight, double bandwidthWeight,
                            double jitterWeight, double packetLossWeight) {
        this.activityType = activityType;
        this.signalStrengthWeight = signalStrengthWeight;
        this.latencyWeight = latencyWeight;
        this.bandwidthWeight = bandwidthWeight;
        this.jitterWeight = jitterWeight;
        this.packetLossWeight = packetLossWeight;
    }
    
    // Getters and Setters
    public String getActivityType() {
        return activityType;
    }
    
    public void setActivityType(String activityType) {
        this.activityType = activityType;
    }
    
    public double getSignalStrengthWeight() {
        return signalStrengthWeight;
    }
    
    public void setSignalStrengthWeight(double signalStrengthWeight) {
        this.signalStrengthWeight = signalStrengthWeight;
    }
    
    public double getLatencyWeight() {
        return latencyWeight;
    }
    
    public void setLatencyWeight(double latencyWeight) {
        this.latencyWeight = latencyWeight;
    }
    
    public double getBandwidthWeight() {
        return bandwidthWeight;
    }
    
    public void setBandwidthWeight(double bandwidthWeight) {
        this.bandwidthWeight = bandwidthWeight;
    }
    
    public double getJitterWeight() {
        return jitterWeight;
    }
    
    public void setJitterWeight(double jitterWeight) {
        this.jitterWeight = jitterWeight;
    }
    
    public double getPacketLossWeight() {
        return packetLossWeight;
    }
    
    public void setPacketLossWeight(double packetLossWeight) {
        this.packetLossWeight = packetLossWeight;
    }
    
    /**
     * Get the most important metric for this activity
     */
    public String getMostImportantMetric() {
        double maxWeight = Math.max(Math.max(Math.max(Math.max(
            signalStrengthWeight, latencyWeight), bandwidthWeight), jitterWeight), packetLossWeight);
        
        if (maxWeight == signalStrengthWeight) return "signal_strength";
        if (maxWeight == latencyWeight) return "latency";
        if (maxWeight == bandwidthWeight) return "bandwidth";
        if (maxWeight == jitterWeight) return "jitter";
        return "packet_loss";
    }
    
    /**
     * Get the least important metric for this activity
     */
    public String getLeastImportantMetric() {
        double minWeight = Math.min(Math.min(Math.min(Math.min(
            signalStrengthWeight, latencyWeight), bandwidthWeight), jitterWeight), packetLossWeight);
        
        if (minWeight == signalStrengthWeight) return "signal_strength";
        if (minWeight == latencyWeight) return "latency";
        if (minWeight == bandwidthWeight) return "bandwidth";
        if (minWeight == jitterWeight) return "jitter";
        return "packet_loss";
    }
}
