package com.example.hifiwifi.classifier;

/**
 * Enum representing WiFi quality classification levels
 */
public enum WiFiClassification {
    EXCELLENT("Excellent"),
    GOOD("Good"),
    OKAY("Okay"),
    BAD("Bad"),
    MARGINAL("Marginal");
    
    private final String displayName;
    
    WiFiClassification(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * Get numeric score for comparison (higher is better)
     */
    public int getScore() {
        switch (this) {
            case EXCELLENT: return 5;
            case GOOD: return 4;
            case OKAY: return 3;
            case BAD: return 2;
            case MARGINAL: return 1;
            default: return 0;
        }
    }
}
