#!/usr/bin/env python3
"""
Test script for /analyze endpoint with raw WiFi measurements.

This script tests the complete flow:
1. Android sends raw measurements (signal_dbm, link_speed_mbps, latency_ms)
2. Flask API classifies them internally
3. Ollama service analyzes and provides recommendations

Usage:
    python3 test_analyze_endpoint.py
"""

import json
import requests
from pathlib import Path
from typing import Dict, Any


def test_analyze_endpoint(base_url: str = "http://localhost:5000"):
    """Test the /analyze endpoint with raw measurements."""
    
    print("=" * 80)
    print("Testing /analyze Endpoint with Raw Measurements")
    print("=" * 80)
    print()
    
    # Test 1: Excellent setup
    print("Test 1: Excellent Setup (Gaming)")
    print("-" * 80)
    
    measurement1 = {
        "location": "living_room",
        "signal_dbm": -45,
        "link_speed_mbps": 866,
        "latency_ms": 12,
        "frequency": "5GHz",
        "activity": "gaming"
    }
    
    print(f"Input: {json.dumps(measurement1, indent=2)}")
    print("\nSending request...")
    
    try:
        response = requests.post(
            f"{base_url}/analyze",
            json=measurement1,
            headers={"Content-Type": "application/json"},
            timeout=60
        )
        
        print(f"Status Code: {response.status_code}")
        
        if response.status_code == 200:
            result = response.json()
            print(f"\nResponse:")
            print(json.dumps(result, indent=2))
            
            if result.get("status") == "success":
                print("\n✅ Test 1 PASSED")
            else:
                print(f"\n❌ Test 1 FAILED: {result.get('error')}")
        else:
            print(f"\n❌ Test 1 FAILED: HTTP {response.status_code}")
            print(response.text)
            
    except Exception as e:
        print(f"\n❌ Test 1 FAILED: {e}")
    
    print("\n")
    
    # Test 2: Poor signal
    print("Test 2: Poor Signal (Video Call)")
    print("-" * 80)
    
    measurement2 = {
        "location": "bedroom",
        "signal_dbm": -78,
        "link_speed_mbps": 65,
        "latency_ms": 52,
        "frequency": "2.4GHz",
        "activity": "video_call"
    }
    
    print(f"Input: {json.dumps(measurement2, indent=2)}")
    print("\nSending request...")
    
    try:
        response = requests.post(
            f"{base_url}/analyze",
            json=measurement2,
            headers={"Content-Type": "application/json"},
            timeout=60
        )
        
        print(f"Status Code: {response.status_code}")
        
        if response.status_code == 200:
            result = response.json()
            print(f"\nResponse:")
            print(json.dumps(result, indent=2))
            
            if result.get("status") == "success":
                recommendation = result.get("recommendation", {})
                if recommendation.get("action") in ["move_location", "switch_band"]:
                    print("\n✅ Test 2 PASSED (Recommended improvement)")
                else:
                    print(f"\n⚠️  Test 2 WARNING: Expected move/switch, got {recommendation.get('action')}")
            else:
                print(f"\n❌ Test 2 FAILED: {result.get('error')}")
        else:
            print(f"\n❌ Test 2 FAILED: HTTP {response.status_code}")
            print(response.text)
            
    except Exception as e:
        print(f"\n❌ Test 2 FAILED: {e}")
    
    print("\n")
    
    # Test 3: Good signal on 2.4GHz (should suggest switch)
    print("Test 3: Good Signal on 2.4GHz (Streaming)")
    print("-" * 80)
    
    measurement3 = {
        "location": "office",
        "signal_dbm": -55,
        "link_speed_mbps": 144,
        "latency_ms": 18,
        "frequency": "2.4GHz",
        "activity": "streaming"
    }
    
    print(f"Input: {json.dumps(measurement3, indent=2)}")
    print("\nSending request...")
    
    try:
        response = requests.post(
            f"{base_url}/analyze",
            json=measurement3,
            headers={"Content-Type": "application/json"},
            timeout=60
        )
        
        print(f"Status Code: {response.status_code}")
        
        if response.status_code == 200:
            result = response.json()
            print(f"\nResponse:")
            print(json.dumps(result, indent=2))
            
            if result.get("status") == "success":
                print("\n✅ Test 3 PASSED")
            else:
                print(f"\n❌ Test 3 FAILED: {result.get('error')}")
        else:
            print(f"\n❌ Test 3 FAILED: HTTP {response.status_code}")
            print(response.text)
            
    except Exception as e:
        print(f"\n❌ Test 3 FAILED: {e}")
    
    print("\n")
    print("=" * 80)
    print("Testing Complete")
    print("=" * 80)


def test_health_endpoint(base_url: str = "http://localhost:5000"):
    """Test the /health endpoint."""
    print("Testing /health endpoint...")
    
    try:
        response = requests.get(f"{base_url}/health", timeout=5)
        
        if response.status_code == 200:
            result = response.json()
            print(f"✅ Health check passed: {result.get('status')}")
            print(f"   Ollama available: {result.get('ollama_available')}")
            return True
        else:
            print(f"❌ Health check failed: HTTP {response.status_code}")
            return False
            
    except Exception as e:
        print(f"❌ Health check failed: {e}")
        return False


def main():
    """Main test execution."""
    print("\n")
    print("=" * 80)
    print("WiFi Optimization API - /analyze Endpoint Tests")
    print("=" * 80)
    print()
    
    # Check health first
    if not test_health_endpoint():
        print("\n⚠️  Warning: API may not be running properly")
        print("Please ensure:")
        print("  1. Ollama is running: ollama serve")
        print("  2. Flask API is running: python3 simple_api.py")
        print()
        response = input("Continue anyway? (y/n): ")
        if response.lower() != 'y':
            return
    
    print("\n")
    
    # Run tests
    test_analyze_endpoint()


if __name__ == "__main__":
    main()

