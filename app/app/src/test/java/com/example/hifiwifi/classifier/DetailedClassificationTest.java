package com.example.hifiwifi.classifier;

import com.example.hifiwifi.models.RoomMeasurement;
import com.example.hifiwifi.services.WiFiMeasurementService;
import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Test class demonstrating detailed metric classification functionality
 */
public class DetailedClassificationTest {
    
    @Test
    public void testDetailedMetricAnalysis() throws Exception {
        WiFiMeasurementService service = new WiFiMeasurementService(null);
        
        // Set up callback to capture classification result
        final ClassificationResult[] capturedResult = {null};
        service.setCallback(new WiFiMeasurementService.MeasurementCallback() {
            @Override
            public void onMeasurementUpdate(com.example.hifiwifi.models.NetworkMetrics metrics) {}
            
            @Override
            public void onMeasurementComplete(RoomMeasurement measurement) {}
            
            @Override
            public void onClassificationComplete(ClassificationResult result) {
                capturedResult[0] = result;
            }
            
            @Override
            public void onError(String error) {}
        });
        
        // Create room measurement which will trigger classification
        RoomMeasurement result = service.createRoomMeasurement("room1", "Living Room", "gaming");
        
        // Wait a moment for classification to complete
        Thread.sleep(100);
        
        ClassificationResult classificationResult = capturedResult[0];
        
        // Test metric details
        MetricDetail latencyDetail = classificationResult.getMetricDetail("latency");
        assertNotNull("Latency detail should not be null", latencyDetail);
        assertTrue("Latency should be critical for gaming", latencyDetail.isCritical());
        
        MetricDetail jitterDetail = classificationResult.getMetricDetail("jitter");
        assertNotNull("Jitter detail should not be null", jitterDetail);
        assertTrue("Jitter should be critical for gaming", jitterDetail.isCritical());
        
        // Test performance analysis
        String[] wellPerforming = classificationResult.getWellPerformingMetrics();
        String[] poorlyPerforming = classificationResult.getPoorlyPerformingMetrics();
        
        // Test most important poor metric
        String mostImportantPoor = classificationResult.getMostImportantPoorMetric();
        
        System.out.println("=== Detailed Analysis Test ===");
        System.out.println("Classification Result: " + classificationResult.getSummary());
        System.out.println("Well Performing: " + String.join(", ", wellPerforming));
        System.out.println("Poorly Performing: " + String.join(", ", poorlyPerforming));
        System.out.println("Most Important Poor: " + mostImportantPoor);
    }
    
    @Test
    public void testJsonOutputWithDetailedMetrics() throws Exception {
        WiFiMeasurementService service = new WiFiMeasurementService(null);
        
        // Set up callback to capture classification result
        final ClassificationResult[] capturedResult = {null};
        service.setCallback(new WiFiMeasurementService.MeasurementCallback() {
            @Override
            public void onMeasurementUpdate(com.example.hifiwifi.models.NetworkMetrics metrics) {}
            
            @Override
            public void onMeasurementComplete(RoomMeasurement measurement) {}
            
            @Override
            public void onClassificationComplete(ClassificationResult result) {
                capturedResult[0] = result;
            }
            
            @Override
            public void onError(String error) {}
        });
        
        // Create room measurement which will trigger classification
        RoomMeasurement result = service.createRoomMeasurement("room2", "Office", "video_call");
        
        // Wait a moment for classification to complete
        Thread.sleep(100);
        
        ClassificationResult classificationResult = capturedResult[0];
        JSONObject json = createJsonOutput(classificationResult);
        
        // Test detailed metrics in JSON
        assertTrue("JSON should contain detailed_metrics", json.has("detailed_metrics"));
        JSONObject detailedMetrics = json.getJSONObject("detailed_metrics");
        
        // Test latency details
        assertTrue("Should have latency details", detailedMetrics.has("latency"));
        JSONObject latencyDetails = detailedMetrics.getJSONObject("latency");
        assertTrue("Latency should be performing well", latencyDetails.getBoolean("is_performing_well"));
        assertTrue("Latency should be critical", latencyDetails.getBoolean("is_critical"));
        assertTrue("Latency reasoning should contain video_call", 
            latencyDetails.getString("reasoning").contains("video_call"));
        
        // Test performance analysis in JSON
        assertTrue("JSON should contain performance_analysis", json.has("performance_analysis"));
        JSONObject performance = json.getJSONObject("performance_analysis");
        assertTrue("Should have well_performing_metrics", performance.has("well_performing_metrics"));
        assertTrue("Should have poorly_performing_metrics", performance.has("poorly_performing_metrics"));
        
        System.out.println("=== JSON Output Test ===");
        System.out.println("Full JSON: " + json.toString(2));
    }
    
    @Test
    public void testMetricsNeedingAttention() throws Exception {
        WiFiMeasurementService service = new WiFiMeasurementService(null);
        
        // Set up callback to capture classification result
        final ClassificationResult[] capturedResult = {null};
        service.setCallback(new WiFiMeasurementService.MeasurementCallback() {
            @Override
            public void onMeasurementUpdate(com.example.hifiwifi.models.NetworkMetrics metrics) {}
            
            @Override
            public void onMeasurementComplete(RoomMeasurement measurement) {}
            
            @Override
            public void onClassificationComplete(ClassificationResult result) {
                capturedResult[0] = result;
            }
            
            @Override
            public void onError(String error) {}
        });
        
        // Create room measurement which will trigger classification
        RoomMeasurement result = service.createRoomMeasurement("room3", "Basement", "gaming");
        
        // Wait a moment for classification to complete
        Thread.sleep(100);
        
        ClassificationResult classificationResult = capturedResult[0];
        
        // Test metrics needing attention
        String[] needsAttention = getMetricsNeedingAttention(classificationResult);
        assertTrue("Should have multiple metrics needing attention", needsAttention.length > 1);
        
        System.out.println("=== Metrics Needing Attention Test ===");
        System.out.println("Metrics needing attention: " + String.join(", ", needsAttention));
        for (String metric : needsAttention) {
            MetricDetail detail = classificationResult.getMetricDetail(metric);
            System.out.println("- " + metric + ": " + detail.getSummary() + " - " + detail.getReasoning());
        }
    }
    
    @Test
    public void testActivitySpecificAnalysis() throws Exception {
        WiFiMeasurementService service = new WiFiMeasurementService(null);
        
        // Set up callback to capture classification result
        final ClassificationResult[] capturedResult = {null};
        service.setCallback(new WiFiMeasurementService.MeasurementCallback() {
            @Override
            public void onMeasurementUpdate(com.example.hifiwifi.models.NetworkMetrics metrics) {}
            
            @Override
            public void onMeasurementComplete(RoomMeasurement measurement) {}
            
            @Override
            public void onClassificationComplete(ClassificationResult result) {
                capturedResult[0] = result;
            }
            
            @Override
            public void onError(String error) {}
        });
        
        // Create room measurement which will trigger classification
        RoomMeasurement result = service.createRoomMeasurement("room4", "Bedroom", "streaming");
        
        // Wait a moment for classification to complete
        Thread.sleep(100);
        
        ClassificationResult streamingResult = capturedResult[0];
        
        // Bandwidth should be critical and performing well
        MetricDetail bandwidthDetail = streamingResult.getMetricDetail("bandwidth");
        assertTrue("Bandwidth should be critical for streaming", bandwidthDetail.isCritical());
        
        // Latency should be less important
        MetricDetail latencyDetail = streamingResult.getMetricDetail("latency");
        assertFalse("Latency should not be critical for streaming", latencyDetail.isCritical());
        
        System.out.println("=== Activity-Specific Analysis Test ===");
        System.out.println("Streaming Analysis:");
        System.out.println("Classification Result: " + streamingResult.getSummary());
    }
    
    /**
     * Helper method to get metrics needing attention
     */
    private String[] getMetricsNeedingAttention(ClassificationResult result) {
        java.util.List<String> needsAttention = new java.util.ArrayList<>();
        MetricDetail[] details = result.getAllMetricDetails();
        
        for (MetricDetail detail : details) {
            if (detail.isPerformingPoorly() && detail.isCritical()) {
                needsAttention.add(detail.getMetricType());
            }
        }
        
        return needsAttention.toArray(new String[0]);
    }
    
    /**
     * Create JSON output for classification result
     */
    private static JSONObject createJsonOutput(ClassificationResult result) throws Exception {
        JSONObject json = new JSONObject();
        
        // Basic information
        json.put("room_name", result.getRoomName());
        json.put("activity_type", result.getActivityType());
        json.put("overall_classification", result.getOverallClassification().getDisplayName());
        json.put("overall_score", result.getOverallClassification().getScore());
        json.put("reasoning", result.getReasoning());
        json.put("most_critical_metric", result.getMostCriticalMetric());
        json.put("is_acceptable", result.isAcceptableForActivity());
        json.put("timestamp", result.getTimestamp());
        
        // Individual metric classifications
        JSONObject metrics = new JSONObject();
        metrics.put("signal_strength", result.getMetricClassification().getSignalStrengthClassification().getDisplayName());
        metrics.put("latency", result.getMetricClassification().getLatencyClassification().getDisplayName());
        metrics.put("bandwidth", result.getMetricClassification().getBandwidthClassification().getDisplayName());
        metrics.put("jitter", result.getMetricClassification().getJitterClassification().getDisplayName());
        metrics.put("packet_loss", result.getMetricClassification().getPacketLossClassification().getDisplayName());
        json.put("metric_classifications", metrics);
        
        // Detailed metric information
        JSONObject detailedMetrics = new JSONObject();
        MetricDetail[] metricDetails = result.getAllMetricDetails();
        for (MetricDetail detail : metricDetails) {
            JSONObject metricInfo = new JSONObject();
            metricInfo.put("classification", detail.getClassification().getDisplayName());
            metricInfo.put("score", detail.getClassification().getScore());
            metricInfo.put("importance", detail.getImportance());
            metricInfo.put("reasoning", detail.getReasoning());
            metricInfo.put("is_performing_well", detail.isPerformingWell());
            metricInfo.put("is_performing_poorly", detail.isPerformingPoorly());
            metricInfo.put("is_critical", detail.isCritical());
            detailedMetrics.put(detail.getMetricType(), metricInfo);
        }
        json.put("detailed_metrics", detailedMetrics);
        
        // Performance analysis
        JSONObject performance = new JSONObject();
        performance.put("well_performing_metrics", new JSONArray(result.getWellPerformingMetrics()));
        performance.put("poorly_performing_metrics", new JSONArray(result.getPoorlyPerformingMetrics()));
        performance.put("most_important_poor_metric", result.getMostImportantPoorMetric());
        json.put("performance_analysis", performance);
        
        // Recommendations
        JSONArray recommendations = new JSONArray();
        for (String recommendation : result.getRecommendations()) {
            recommendations.put(recommendation);
        }
        json.put("recommendations", recommendations);
        
        return json;
    }
}
