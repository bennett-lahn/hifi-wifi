package com.example.hifiwifi.classifier;

import java.util.ArrayList;
import java.util.List;

/**
 * Class representing the final classification result with detailed reasoning
 */
public class ClassificationResult {
    private String roomId;
    private String roomName;
    private String activityType;
    private WiFiClassification overallClassification;
    private MetricClassification metricClassification;
    private ActivityImportance activityImportance;
    private String reasoning;
    private String mostCriticalMetric;
    private List<String> recommendations;
    private long timestamp;
    private String frequencyBand; // "2.4GHz" or "5GHz"
    
    public ClassificationResult() {
        this.recommendations = new ArrayList<>();
        this.timestamp = System.currentTimeMillis();
        this.frequencyBand = "Unknown";
    }
    
    public ClassificationResult(String roomId, String roomName, String activityType, 
                              WiFiClassification overallClassification,
                              MetricClassification metricClassification,
                              ActivityImportance activityImportance) {
        this.roomId = roomId;
        this.roomName = roomName;
        this.activityType = activityType;
        this.overallClassification = overallClassification;
        this.metricClassification = metricClassification;
        this.activityImportance = activityImportance;
        this.recommendations = new ArrayList<>();
        this.timestamp = System.currentTimeMillis();
        generateReasoning();
        generateRecommendations();
    }
    
    // Getters and Setters
    public String getRoomId() {
        return roomId;
    }
    
    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }
    
    public String getRoomName() {
        return roomName;
    }
    
    public void setRoomName(String roomName) {
        this.roomName = roomName;
    }
    
    public String getActivityType() {
        return activityType;
    }
    
    public void setActivityType(String activityType) {
        this.activityType = activityType;
    }
    
    public WiFiClassification getOverallClassification() {
        return overallClassification;
    }
    
    public void setOverallClassification(WiFiClassification overallClassification) {
        this.overallClassification = overallClassification;
    }
    
    public MetricClassification getMetricClassification() {
        return metricClassification;
    }
    
    public void setMetricClassification(MetricClassification metricClassification) {
        this.metricClassification = metricClassification;
    }
    
    public ActivityImportance getActivityImportance() {
        return activityImportance;
    }
    
    public void setActivityImportance(ActivityImportance activityImportance) {
        this.activityImportance = activityImportance;
    }
    
    public String getReasoning() {
        return reasoning;
    }
    
    public void setReasoning(String reasoning) {
        this.reasoning = reasoning;
    }
    
    public String getMostCriticalMetric() {
        return mostCriticalMetric;
    }
    
    public void setMostCriticalMetric(String mostCriticalMetric) {
        this.mostCriticalMetric = mostCriticalMetric;
    }
    
    public List<String> getRecommendations() {
        return recommendations;
    }
    
    public void setRecommendations(List<String> recommendations) {
        this.recommendations = recommendations;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
    
    public String getFrequencyBand() {
        return frequencyBand;
    }
    
    public void setFrequencyBand(String frequencyBand) {
        this.frequencyBand = frequencyBand;
    }
    
    /**
     * Generate human-readable reasoning for the classification
     */
    private void generateReasoning() {
        StringBuilder reasonBuilder = new StringBuilder();
        
        reasonBuilder.append("WiFi classification for ").append(roomName)
                    .append(" during ").append(activityType).append(" activity: ");
        
        // Overall classification
        reasonBuilder.append(overallClassification.getDisplayName()).append(" (");
        
        // Add reasoning based on most critical metrics
        String mostImportantMetric = activityImportance.getMostImportantMetric();
        WiFiClassification mostImportantClassification = getMetricClassificationByType(mostImportantMetric);
        
        reasonBuilder.append(mostImportantMetric.replace("_", " ")).append(" is ")
                    .append(mostImportantClassification.getDisplayName().toLowerCase());
        
        // Add context about importance
        if (mostImportantMetric.equals("latency") && activityType.equals("gaming")) {
            reasonBuilder.append(" and latency is very important for gaming");
        } else if (mostImportantMetric.equals("bandwidth") && activityType.equals("streaming")) {
            reasonBuilder.append(" and bandwidth is very important for streaming");
        } else if (mostImportantMetric.equals("jitter") && activityType.equals("video_call")) {
            reasonBuilder.append(" and jitter is very important for video calls");
        }
        
        reasonBuilder.append(")");
        
        this.reasoning = reasonBuilder.toString();
    }
    
    /**
     * Generate recommendations based on the classification
     */
    private void generateRecommendations() {
        recommendations.clear();
        
        // Signal strength recommendations
        if (metricClassification.getSignalStrengthClassification().getScore() <= 2) {
            recommendations.add("Move closer to the router or consider a WiFi extender");
            recommendations.add("Check for physical obstructions between device and router");
        }
        
        // Latency recommendations
        if (metricClassification.getLatencyClassification().getScore() <= 2) {
            recommendations.add("Check for network congestion or switch to 5GHz band");
            recommendations.add("Consider using a wired connection for better latency");
        }
        
        // Bandwidth recommendations
        if (metricClassification.getBandwidthClassification().getScore() <= 2) {
            recommendations.add("Upgrade your internet plan or check for bandwidth limits");
            recommendations.add("Close unnecessary applications using bandwidth");
        }
        
        // Jitter recommendations
        if (metricClassification.getJitterClassification().getScore() <= 2) {
            recommendations.add("Check for network interference or switch to a less congested channel");
            recommendations.add("Consider using QoS settings on your router");
        }
        
        // Packet loss recommendations
        if (metricClassification.getPacketLossClassification().getScore() <= 2) {
            recommendations.add("Check for network interference or hardware issues");
            recommendations.add("Try restarting your router and modem");
        }
        
        // Activity-specific recommendations
        if (activityType.equals("gaming") && overallClassification.getScore() <= 3) {
            recommendations.add("Consider using a gaming router with QoS features");
            recommendations.add("Use a wired connection for the best gaming experience");
        } else if (activityType.equals("streaming") && overallClassification.getScore() <= 3) {
            recommendations.add("Lower video quality settings if available");
            recommendations.add("Schedule streaming during off-peak hours");
        } else if (activityType.equals("video_call") && overallClassification.getScore() <= 3) {
            recommendations.add("Close other applications during video calls");
            recommendations.add("Use a wired connection for important meetings");
        }
    }
    
    /**
     * Get metric classification by type
     */
    private WiFiClassification getMetricClassificationByType(String metricType) {
        switch (metricType) {
            case "signal_strength":
                return metricClassification.getSignalStrengthClassification();
            case "latency":
                return metricClassification.getLatencyClassification();
            case "bandwidth":
                return metricClassification.getBandwidthClassification();
            case "jitter":
                return metricClassification.getJitterClassification();
            case "packet_loss":
                return metricClassification.getPacketLossClassification();
            default:
                return WiFiClassification.MARGINAL;
        }
    }
    
    /**
     * Get detailed classification information for a specific metric
     */
    public MetricDetail getMetricDetail(String metricType) {
        WiFiClassification classification = getMetricClassificationByType(metricType);
        double importance = getImportanceForMetric(metricType);
        String reasoning = getReasoningForMetric(metricType, classification, importance);
        
        return new MetricDetail(metricType, classification, importance, reasoning);
    }
    
    /**
     * Get all metric details for comprehensive analysis
     */
    public MetricDetail[] getAllMetricDetails() {
        return new MetricDetail[]{
            getMetricDetail("signal_strength"),
            getMetricDetail("latency"),
            getMetricDetail("bandwidth"),
            getMetricDetail("jitter"),
            getMetricDetail("packet_loss")
        };
    }
    
    /**
     * Get importance weight for a specific metric
     */
    private double getImportanceForMetric(String metricType) {
        switch (metricType) {
            case "signal_strength":
                return activityImportance.getSignalStrengthWeight();
            case "latency":
                return activityImportance.getLatencyWeight();
            case "bandwidth":
                return activityImportance.getBandwidthWeight();
            case "jitter":
                return activityImportance.getJitterWeight();
            case "packet_loss":
                return activityImportance.getPacketLossWeight();
            default:
                return 1.0;
        }
    }
    
    /**
     * Generate reasoning for a specific metric
     */
    private String getReasoningForMetric(String metricType, WiFiClassification classification, double importance) {
        StringBuilder reason = new StringBuilder();
        
        reason.append(metricType.replace("_", " ")).append(" is ")
              .append(classification.getDisplayName().toLowerCase());
        
        // Add importance context
        if (importance >= 0.9) {
            reason.append(" and is very important for ").append(activityType);
        } else if (importance >= 0.7) {
            reason.append(" and is important for ").append(activityType);
        } else if (importance >= 0.5) {
            reason.append(" and is moderately important for ").append(activityType);
        } else {
            reason.append(" and is less important for ").append(activityType);
        }
        
        return reason.toString();
    }
    
    /**
     * Get metrics that are performing well (Good or Excellent)
     */
    public String[] getWellPerformingMetrics() {
        java.util.List<String> wellPerforming = new java.util.ArrayList<>();
        
        if (metricClassification.getSignalStrengthClassification().getScore() >= 4) {
            wellPerforming.add("signal_strength");
        }
        if (metricClassification.getLatencyClassification().getScore() >= 4) {
            wellPerforming.add("latency");
        }
        if (metricClassification.getBandwidthClassification().getScore() >= 4) {
            wellPerforming.add("bandwidth");
        }
        if (metricClassification.getJitterClassification().getScore() >= 4) {
            wellPerforming.add("jitter");
        }
        if (metricClassification.getPacketLossClassification().getScore() >= 4) {
            wellPerforming.add("packet_loss");
        }
        
        return wellPerforming.toArray(new String[0]);
    }
    
    /**
     * Get metrics that are performing poorly (Bad or Marginal)
     */
    public String[] getPoorlyPerformingMetrics() {
        java.util.List<String> poorlyPerforming = new java.util.ArrayList<>();
        
        if (metricClassification.getSignalStrengthClassification().getScore() <= 2) {
            poorlyPerforming.add("signal_strength");
        }
        if (metricClassification.getLatencyClassification().getScore() <= 2) {
            poorlyPerforming.add("latency");
        }
        if (metricClassification.getBandwidthClassification().getScore() <= 2) {
            poorlyPerforming.add("bandwidth");
        }
        if (metricClassification.getJitterClassification().getScore() <= 2) {
            poorlyPerforming.add("jitter");
        }
        if (metricClassification.getPacketLossClassification().getScore() <= 2) {
            poorlyPerforming.add("packet_loss");
        }
        
        return poorlyPerforming.toArray(new String[0]);
    }
    
    /**
     * Get the most important metric that is performing poorly
     */
    public String getMostImportantPoorMetric() {
        String[] poorMetrics = getPoorlyPerformingMetrics();
        String mostImportantPoor = null;
        double highestImportance = 0.0;
        
        for (String metric : poorMetrics) {
            double importance = getImportanceForMetric(metric);
            if (importance > highestImportance) {
                highestImportance = importance;
                mostImportantPoor = metric;
            }
        }
        
        return mostImportantPoor;
    }
    
    /**
     * Get a summary of the classification result
     */
    public String getSummary() {
        return String.format("%s WiFi in %s for %s: %s", 
            overallClassification.getDisplayName(), 
            roomName, 
            activityType, 
            reasoning);
    }
    
    /**
     * Check if the classification is acceptable for the activity
     */
    public boolean isAcceptableForActivity() {
        // Define minimum acceptable classifications for different activities
        int minScore = 3; // OKAY by default
        
        if (activityType.equals("gaming") || activityType.equals("video_call")) {
            minScore = 4; // GOOD or better
        } else if (activityType.equals("streaming")) {
            minScore = 3; // OKAY or better
        } else if (activityType.equals("general")) {
            minScore = 2; // BAD or better
        }
        
        return overallClassification.getScore() >= minScore;
    }
}
