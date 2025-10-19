#!/usr/bin/env python3
"""
Quick test script for Raspberry Pi to verify Ollama is working.
Tests the wifi-assistant model with a sample scenario.

Usage:
    python3 test_ollama_direct.py
"""

import json
import requests
import sys

# Test measurement (classified values only)
TEST_MEASUREMENT = {
    "location": "living_room",
    "signal_strength": "excellent",
    "latency": "excellent", 
    "bandwidth": "excellent",
    "jitter": "excellent",
    "packet_loss": "excellent",
    "frequency": "5GHz",
    "activity": "gaming"
}

def test_ollama_health():
    """Check if Ollama server is running."""
    print("=" * 60)
    print("1. Checking Ollama Server Health")
    print("=" * 60)
    
    try:
        response = requests.get("http://localhost:11434/api/tags", timeout=5)
        if response.status_code == 200:
            print("✅ Ollama server is running")
            data = response.json()
            models = [m["name"] for m in data.get("models", [])]
            print(f"   Available models: {', '.join(models)}")
            
            if "wifi-assistant" in models or "wifi-assistant:latest" in models:
                print("✅ wifi-assistant model found")
                return True
            else:
                print("❌ wifi-assistant model NOT found")
                print("   Run: ollama create wifi-assistant -f models/Modelfile")
                return False
        else:
            print(f"❌ Ollama server returned status {response.status_code}")
            return False
    except Exception as e:
        print(f"❌ Cannot connect to Ollama server: {e}")
        print("   Run: ollama serve")
        return False

def test_wifi_assistant():
    """Test the wifi-assistant model with a sample measurement."""
    print("\n" + "=" * 60)
    print("2. Testing wifi-assistant Model")
    print("=" * 60)
    
    print("\nInput measurement:")
    print(json.dumps(TEST_MEASUREMENT, indent=2))
    
    # Construct prompt (classification-only, no raw numbers)
    prompt = f"""Analyze this WiFi situation and provide recommendations:

Location: {TEST_MEASUREMENT['location']}
Signal Strength: {TEST_MEASUREMENT['signal_strength']}
Latency: {TEST_MEASUREMENT['latency']}
Bandwidth: {TEST_MEASUREMENT['bandwidth']}
Jitter: {TEST_MEASUREMENT['jitter']}
Packet Loss: {TEST_MEASUREMENT['packet_loss']}
Frequency Band: {TEST_MEASUREMENT['frequency']}
Current Activity: {TEST_MEASUREMENT['activity']}

Based on these classifications, provide a JSON response with your recommendation."""
    
    payload = {
        "model": "wifi-assistant",
        "prompt": prompt,
        "stream": False,
        "format": "json"
    }
    
    print("\n🤖 Sending request to Ollama...")
    print("   (This may take 5-15 seconds on Raspberry Pi)")
    
    try:
        response = requests.post(
            "http://localhost:11434/api/generate",
            json=payload,
            timeout=60
        )
        response.raise_for_status()
        
        result = response.json()
        
        if result.get("done", False):
            response_text = result.get("response", "")
            print("\n✅ Model responded successfully!\n")
            print("=" * 60)
            print("LLM Response:")
            print("=" * 60)
            
            try:
                parsed = json.loads(response_text)
                print(json.dumps(parsed, indent=2))
                
                # Show key fields in chatbot format
                if "recommendation" in parsed:
                    rec = parsed["recommendation"]
                    print("\n" + "=" * 60)
                    print("Chatbot Output:")
                    print("=" * 60)
                    print(f"📍 Location: {TEST_MEASUREMENT['location']}")
                    print(f"🎮 Activity: {TEST_MEASUREMENT['activity']}")
                    print(f"✅ Action: {rec.get('action', 'unknown')}")
                    print(f"💬 Message: {rec.get('message', 'No message')}")
                    
                if "analysis" in parsed:
                    analysis = parsed["analysis"]
                    print(f"\n📊 Quality: {analysis.get('current_quality', 'unknown')}")
                    print(f"⭐ Rating: {analysis.get('signal_rating', '?')}/10")
                    print(f"✓ Suitable: {analysis.get('suitable_for_activity', '?')}")
                
                return True
                
            except json.JSONDecodeError:
                print("⚠️  Response is not valid JSON:")
                print(response_text[:500])
                return False
        else:
            print("❌ Incomplete response from model")
            return False
            
    except requests.exceptions.Timeout:
        print("❌ Request timed out (model may be slow)")
        return False
    except Exception as e:
        print(f"❌ Error: {e}")
        return False

def main():
    print("\n" + "=" * 60)
    print("WiFi Assistant - Raspberry Pi Quick Test")
    print("=" * 60)
    print()
    
    # Test 1: Check Ollama health
    if not test_ollama_health():
        print("\n❌ Ollama health check failed. Fix issues and try again.")
        sys.exit(1)
    
    # Test 2: Test wifi-assistant model
    if not test_wifi_assistant():
        print("\n❌ Model test failed.")
        sys.exit(1)
    
    print("\n" + "=" * 60)
    print("✅ All tests passed! Ollama is working correctly.")
    print("=" * 60)
    print("\nNext steps:")
    print("  1. Start Flask API: python3 simple_api.py")
    print("  2. Test from Android or another device")
    print()

if __name__ == "__main__":
    main()

