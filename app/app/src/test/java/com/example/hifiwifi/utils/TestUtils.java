package com.example.hifiwifi.utils;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

import org.mockito.Mockito;

import static org.mockito.Mockito.when;

/**
 * Utility class for creating test mocks and helpers
 */
public class TestUtils {
    
    /**
     * Creates a mock WifiManager with specified signal strength
     */
    public static WifiManager createMockWifiManager(Context context, int signalStrength) {
        WifiManager wifiManager = Mockito.mock(WifiManager.class);
        WifiInfo wifiInfo = Mockito.mock(WifiInfo.class);
        
        when(wifiManager.getConnectionInfo()).thenReturn(wifiInfo);
        when(wifiInfo.getRssi()).thenReturn(signalStrength);
        
        return wifiManager;
    }
    
    /**
     * Creates a mock WifiManager that returns null connection info
     */
    public static WifiManager createMockWifiManagerWithNullInfo(Context context) {
        WifiManager wifiManager = Mockito.mock(WifiManager.class);
        when(wifiManager.getConnectionInfo()).thenReturn(null);
        return wifiManager;
    }
    
    /**
     * Creates a mock Context
     */
    public static Context createMockContext() {
        return Mockito.mock(Context.class);
    }
    
    /**
     * Sleeps for a short duration to simulate async operations
     */
    public static void sleepForAsyncOperation() {
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * Sleeps for a longer duration to simulate network operations
     */
    public static void sleepForNetworkOperation() {
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
