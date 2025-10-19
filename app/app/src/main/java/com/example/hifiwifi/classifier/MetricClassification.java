package com.example.hifiwifi.classifier;

/**
 * Class representing the classification of individual WiFi metrics
 */
public class MetricClassification {
    private WiFiClassification signalStrengthClassification;
    private WiFiClassification latencyClassification;
    private WiFiClassification bandwidthClassification;
    private WiFiClassification jitterClassification;
    private WiFiClassification packetLossClassification;
    
    public MetricClassification() {
        this.signalStrengthClassification = WiFiClassification.MARGINAL;
        this.latencyClassification = WiFiClassification.MARGINAL;
        this.bandwidthClassification = WiFiClassification.MARGINAL;
        this.jitterClassification = WiFiClassification.MARGINAL;
        this.packetLossClassification = WiFiClassification.MARGINAL;
    }
    
    public MetricClassification(WiFiClassification signalStrengthClassification,
                              WiFiClassification latencyClassification,
                              WiFiClassification bandwidthClassification,
                              WiFiClassification jitterClassification,
                              WiFiClassification packetLossClassification) {
        this.signalStrengthClassification = signalStrengthClassification;
        this.latencyClassification = latencyClassification;
        this.bandwidthClassification = bandwidthClassification;
        this.jitterClassification = jitterClassification;
        this.packetLossClassification = packetLossClassification;
    }
    
    // Getters and Setters
    public WiFiClassification getSignalStrengthClassification() {
        return signalStrengthClassification;
    }
    
    public void setSignalStrengthClassification(WiFiClassification signalStrengthClassification) {
        this.signalStrengthClassification = signalStrengthClassification;
    }
    
    public WiFiClassification getLatencyClassification() {
        return latencyClassification;
    }
    
    public void setLatencyClassification(WiFiClassification latencyClassification) {
        this.latencyClassification = latencyClassification;
    }
    
    public WiFiClassification getBandwidthClassification() {
        return bandwidthClassification;
    }
    
    public void setBandwidthClassification(WiFiClassification bandwidthClassification) {
        this.bandwidthClassification = bandwidthClassification;
    }
    
    public WiFiClassification getJitterClassification() {
        return jitterClassification;
    }
    
    public void setJitterClassification(WiFiClassification jitterClassification) {
        this.jitterClassification = jitterClassification;
    }
    
    public WiFiClassification getPacketLossClassification() {
        return packetLossClassification;
    }
    
    public void setPacketLossClassification(WiFiClassification packetLossClassification) {
        this.packetLossClassification = packetLossClassification;
    }
    
    /**
     * Get the worst classification among all metrics
     */
    public WiFiClassification getWorstClassification() {
        WiFiClassification worst = WiFiClassification.EXCELLENT;
        
        for (WiFiClassification classification : new WiFiClassification[]{
            signalStrengthClassification,
            latencyClassification,
            bandwidthClassification,
            jitterClassification,
            packetLossClassification
        }) {
            if (classification.getScore() < worst.getScore()) {
                worst = classification;
            }
        }
        
        return worst;
    }
    
    /**
     * Get the best classification among all metrics
     */
    public WiFiClassification getBestClassification() {
        WiFiClassification best = WiFiClassification.MARGINAL;
        
        for (WiFiClassification classification : new WiFiClassification[]{
            signalStrengthClassification,
            latencyClassification,
            bandwidthClassification,
            jitterClassification,
            packetLossClassification
        }) {
            if (classification.getScore() > best.getScore()) {
                best = classification;
            }
        }
        
        return best;
    }
}
