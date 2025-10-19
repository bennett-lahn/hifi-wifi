package com.example.hifiwifi.classifier;

/**
 * Class representing detailed information about a specific metric classification
 */
public class MetricDetail {
    private String metricType;
    private WiFiClassification classification;
    private double importance;
    private String reasoning;
    
    public MetricDetail() {
        this.metricType = "";
        this.classification = WiFiClassification.MARGINAL;
        this.importance = 1.0;
        this.reasoning = "";
    }
    
    public MetricDetail(String metricType, WiFiClassification classification, 
                       double importance, String reasoning) {
        this.metricType = metricType;
        this.classification = classification;
        this.importance = importance;
        this.reasoning = reasoning;
    }
    
    // Getters and Setters
    public String getMetricType() {
        return metricType;
    }
    
    public void setMetricType(String metricType) {
        this.metricType = metricType;
    }
    
    public WiFiClassification getClassification() {
        return classification;
    }
    
    public void setClassification(WiFiClassification classification) {
        this.classification = classification;
    }
    
    public double getImportance() {
        return importance;
    }
    
    public void setImportance(double importance) {
        this.importance = importance;
    }
    
    public String getReasoning() {
        return reasoning;
    }
    
    public void setReasoning(String reasoning) {
        this.reasoning = reasoning;
    }
    
    /**
     * Get the display name for the metric type
     */
    public String getDisplayName() {
        return metricType.replace("_", " ").toUpperCase();
    }
    
    /**
     * Check if this metric is performing well (Good or Excellent)
     */
    public boolean isPerformingWell() {
        return classification.getScore() >= 4;
    }
    
    /**
     * Check if this metric is performing poorly (Bad or Marginal)
     */
    public boolean isPerformingPoorly() {
        return classification.getScore() <= 2;
    }
    
    /**
     * Check if this metric is critical for the activity (high importance)
     */
    public boolean isCritical() {
        return importance >= 0.8;
    }
    
    /**
     * Get a summary of this metric's performance
     */
    public String getSummary() {
        return String.format("%s: %s (importance: %.1f)", 
            getDisplayName(), 
            classification.getDisplayName(), 
            importance);
    }
    
    @Override
    public String toString() {
        return String.format("MetricDetail{type='%s', classification=%s, importance=%.2f, reasoning='%s'}", 
            metricType, classification.getDisplayName(), importance, reasoning);
    }
}
