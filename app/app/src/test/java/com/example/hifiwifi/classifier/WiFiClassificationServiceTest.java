package com.example.hifiwifi.classifier;

import com.example.hifiwifi.models.RoomMeasurement;
import com.example.hifiwifi.services.WiFiMeasurementService;
import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Test class for integrated WiFi Classification in WiFiMeasurementService
 */
public class WiFiClassificationServiceTest {
    
    @Test
    public void testGamingClassification() throws Exception {
        WiFiMeasurementService service = new WiFiMeasurementService(null);
        
        // Create a room measurement for gaming
        RoomMeasurement measurement = new RoomMeasurement(
            "room1", 
            "Living Room", 
            -45,  // Good signal strength
            25,   // Excellent latency
            75.0, // Good bandwidth
            8.0,  // Good jitter
            0.2,  // Excellent packet loss
            "gaming"
        );
        
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
        
        // Verify the classification
        assertNotNull("Classification result should not be null", capturedResult[0]);
        assertEquals("Living Room", capturedResult[0].getRoomName());
        assertEquals("gaming", capturedResult[0].getActivityType());
        assertTrue("Classification should be at least GOOD for gaming", 
            capturedResult[0].getOverallClassification().getScore() >= 4);
        assertTrue("Should be acceptable for gaming", capturedResult[0].isAcceptableForActivity());
        
        System.out.println("Gaming Classification Result:");
        System.out.println(capturedResult[0].getSummary());
    }
    
    @Test
    public void testStreamingClassification() throws Exception {
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
        RoomMeasurement result = service.createRoomMeasurement("room2", "Bedroom", "streaming");
        
        // Wait a moment for classification to complete
        Thread.sleep(100);
        
        // Verify the classification
        assertNotNull("Classification result should not be null", capturedResult[0]);
        assertEquals("Bedroom", capturedResult[0].getRoomName());
        assertEquals("streaming", capturedResult[0].getActivityType());
        
        System.out.println("\nStreaming Classification Result:");
        System.out.println(capturedResult[0].getSummary());
    }
    
    @Test
    public void testVideoCallClassification() throws Exception {
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
        RoomMeasurement result = service.createRoomMeasurement("room3", "Office", "video_call");
        
        // Wait a moment for classification to complete
        Thread.sleep(100);
        
        // Verify the classification
        assertNotNull("Classification result should not be null", capturedResult[0]);
        assertEquals("Office", capturedResult[0].getRoomName());
        assertEquals("video_call", capturedResult[0].getActivityType());
        assertTrue("Should be acceptable for video calls", capturedResult[0].isAcceptableForActivity());
        
        System.out.println("\nVideo Call Classification Result:");
        System.out.println(capturedResult[0].getSummary());
        System.out.println("Recommendations: " + capturedResult[0].getRecommendations());
    }
    
    @Test
    public void testPoorSignalClassification() throws Exception {
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
        RoomMeasurement result = service.createRoomMeasurement("room4", "Basement", "general");
        
        // Wait a moment for classification to complete
        Thread.sleep(100);
        
        // Verify the classification
        assertNotNull("Classification result should not be null", capturedResult[0]);
        assertEquals("Basement", capturedResult[0].getRoomName());
        assertEquals("general", capturedResult[0].getActivityType());
        assertTrue("Should have poor classification", 
            capturedResult[0].getOverallClassification().getScore() <= 2);
        assertFalse("Should not be acceptable", capturedResult[0].isAcceptableForActivity());
        
        System.out.println("\nPoor Signal Classification Result:");
        System.out.println(capturedResult[0].getSummary());
        System.out.println("Recommendations: " + capturedResult[0].getRecommendations());
    }
}
