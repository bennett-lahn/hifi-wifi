#!/usr/bin/env python3
"""
Independent test script for WiFi optimization scenarios using Ollama.

This script directly tests the wifi-assistant model (based on qwen3:0.6b)
against all test scenarios and compares results with expected outcomes.
"""

import json
import requests
import sys
from pathlib import Path
from typing import Dict, Any
from datetime import datetime


class OllamaDirectTester:
    """Direct Ollama API tester without using ollama_service wrapper."""
    
    def __init__(self, base_url: str = "http://localhost:11434"):
        self.base_url = base_url
        self.api_url = f"{base_url}/api/generate"
        self.model_name = "wifi-assistant"
        
    def check_ollama_health(self) -> bool:
        """Check if Ollama server is running."""
        try:
            response = requests.get(f"{self.base_url}/api/tags", timeout=5)
            return response.status_code == 200
        except:
            return False
    
    def check_model_exists(self) -> bool:
        """Check if wifi-assistant model exists."""
        try:
            response = requests.get(f"{self.base_url}/api/tags", timeout=5)
            if response.status_code == 200:
                data = response.json()
                models = [m["name"] for m in data.get("models", [])]
                return self.model_name in models or f"{self.model_name}:latest" in models
            return False
        except:
            return False
    
    def test_scenario(self, measurement: Dict[str, Any]) -> Dict[str, Any]:
        """Test a scenario directly with Ollama API using pre-classified values."""
        
        # Construct the prompt with classifications only (no raw numbers)
        prompt = f"""Analyze this WiFi situation and provide recommendations:

Location: {measurement.get('location', 'unknown')}
Signal Strength: {measurement.get('signal_strength', 'N/A')}
Latency: {measurement.get('latency', 'N/A')}
Bandwidth: {measurement.get('bandwidth', 'N/A')}
Jitter: {measurement.get('jitter', 'N/A')}
Packet Loss: {measurement.get('packet_loss', 'N/A')}
Frequency Band: {measurement.get('frequency', 'N/A')}
Current Activity: {measurement.get('activity', 'general use')}

Based on these classifications, provide a JSON response with your recommendation."""
        
        payload = {
            "model": self.model_name,
            "prompt": prompt,
            "stream": False,
            "format": "json"
        }
        
        try:
            response = requests.post(
                self.api_url,
                json=payload,
                timeout=60  # Longer timeout for model inference
            )
            response.raise_for_status()
            
            result = response.json()
            
            # Parse the response
            if result.get("done", False):
                response_text = result.get("response", "")
                if response_text:
                    try:
                        parsed = json.loads(response_text)
                        return {
                            "success": True,
                            "result": parsed,
                            "raw_response": response_text
                        }
                    except json.JSONDecodeError as e:
                        return {
                            "success": False,
                            "error": f"Invalid JSON response: {e}",
                            "raw_response": response_text
                        }
            
            return {
                "success": False,
                "error": "Incomplete response from model",
                "result": result
            }
            
        except requests.exceptions.Timeout:
            return {
                "success": False,
                "error": "Request timed out (model may be slow or overloaded)"
            }
        except requests.exceptions.ConnectionError:
            return {
                "success": False,
                "error": "Could not connect to Ollama server"
            }
        except Exception as e:
            return {
                "success": False,
                "error": f"Unexpected error: {str(e)}"
            }


def load_test_scenarios(test_dir: Path) -> list:
    """Load all test scenarios from the test-scenarios directory."""
    scenarios = []
    
    scenario_files = sorted(test_dir.glob("scenario_*.json"))
    
    for file_path in scenario_files:
        try:
            with open(file_path, 'r') as f:
                scenario_data = json.load(f)
                scenarios.append({
                    "file": file_path.name,
                    "data": scenario_data
                })
        except Exception as e:
            print(f"⚠️  Error loading {file_path.name}: {e}")
    
    return scenarios


def compare_results(actual: Dict, expected: Dict) -> Dict[str, Any]:
    """Compare actual results with expected outcomes, including reasoning quality."""
    comparison = {
        "matches": {},
        "mismatches": {},
        "reasoning_present": False,
        "reasoning_quality": "none",
        "score": 0,
        "total_checks": 0
    }
    
    recommendation = actual.get("recommendation", {})
    # Handle both cases: analysis at top level or nested in recommendation
    analysis = actual.get("analysis", recommendation.get("analysis", {}))
    
    # Check current_quality
    comparison["total_checks"] += 1
    expected_quality = expected.get("current_quality")
    actual_quality = analysis.get("current_quality")
    if actual_quality == expected_quality:
        comparison["matches"]["current_quality"] = actual_quality
        comparison["score"] += 1
    else:
        comparison["mismatches"]["current_quality"] = {
            "expected": expected_quality,
            "actual": actual_quality
        }
    
    # Check suitable_for_activity
    comparison["total_checks"] += 1
    expected_suitable = expected.get("suitable_for_activity")
    actual_suitable = analysis.get("suitable_for_activity")
    if actual_suitable == expected_suitable:
        comparison["matches"]["suitable_for_activity"] = actual_suitable
        comparison["score"] += 1
    else:
        comparison["mismatches"]["suitable_for_activity"] = {
            "expected": expected_suitable,
            "actual": actual_suitable
        }
    
    # Check recommended_action
    comparison["total_checks"] += 1
    expected_action = expected.get("recommended_action")
    actual_action = recommendation.get("action")
    if actual_action == expected_action:
        comparison["matches"]["recommended_action"] = actual_action
        comparison["score"] += 1
    else:
        comparison["mismatches"]["recommended_action"] = {
            "expected": expected_action,
            "actual": actual_action
        }
    
    # Check if reasoning is present and meaningful
    if "reasoning" in recommendation:
        reasoning = recommendation["reasoning"]
        comparison["reasoning_present"] = True
        # Check if reasoning is substantial (>50 characters)
        if len(reasoning) > 50:
            comparison["reasoning_quality"] = "good"
            comparison["reasoning_text"] = reasoning[:200] + "..." if len(reasoning) > 200 else reasoning
        else:
            comparison["reasoning_quality"] = "too_brief"
            comparison["reasoning_text"] = reasoning
    
    # Check for why_suitable in analysis
    if "why_suitable" in analysis:
        comparison["has_why_suitable"] = True
        comparison["why_suitable_text"] = analysis["why_suitable"][:150] + "..." if len(analysis.get("why_suitable", "")) > 150 else analysis.get("why_suitable", "")
    
    # Calculate percentage
    comparison["percentage"] = (comparison["score"] / comparison["total_checks"] * 100) if comparison["total_checks"] > 0 else 0
    
    return comparison


def print_separator(char="=", length=80):
    """Print a separator line."""
    print(char * length)


def main():
    """Main test execution."""
    print_separator()
    print("WiFi Optimization Assistant - Scenario Testing")
    print(f"Test started: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
    print_separator()
    print()
    
    # Initialize tester
    tester = OllamaDirectTester()
    
    # Check Ollama health
    print("🔍 Checking Ollama server...")
    if not tester.check_ollama_health():
        print("❌ ERROR: Ollama server is not running or not accessible at localhost:11434")
        print("\nPlease start Ollama with: ollama serve")
        sys.exit(1)
    print("✅ Ollama server is running")
    
    # Check model exists
    print("\n🔍 Checking for wifi-assistant model...")
    if not tester.check_model_exists():
        print("❌ ERROR: wifi-assistant model not found")
        print("\nPlease create the model with:")
        print("  cd models/")
        print("  ollama create wifi-assistant -f Modelfile")
        sys.exit(1)
    print("✅ wifi-assistant model is available")
    print()
    
    # Load test scenarios
    test_dir = Path(__file__).parent / "test-scenarios"
    scenarios = load_test_scenarios(test_dir)
    
    if not scenarios:
        print("❌ No test scenarios found in test-scenarios/")
        sys.exit(1)
    
    print(f"📋 Found {len(scenarios)} test scenarios\n")
    
    # Run tests
    results = []
    
    for i, scenario in enumerate(scenarios, 1):
        scenario_data = scenario["data"]
        scenario_name = scenario_data.get("scenario_name", "Unknown")
        description = scenario_data.get("description", "No description")
        measurement = scenario_data["measurement"]
        expected = scenario_data["expected_outcome"]
        
        print_separator("-")
        print(f"Test {i}/{len(scenarios)}: {scenario_name}")
        print_separator("-")
        print(f"Description: {description}")
        print(f"Location: {measurement['location']}")
        print(f"Signal: {measurement.get('signal_strength', 'N/A')}, "
              f"Latency: {measurement.get('latency', 'N/A')}, "
              f"Bandwidth: {measurement.get('bandwidth', 'N/A')}")
        print(f"Frequency: {measurement['frequency']}, Activity: {measurement['activity']}")
        print()
        
        print("🤖 Running inference with wifi-assistant model...")
        test_result = tester.test_scenario(measurement)
        
        if not test_result["success"]:
            print(f"❌ Test failed: {test_result['error']}")
            if "raw_response" in test_result:
                print(f"\nRaw response:\n{test_result['raw_response'][:500]}...")
            results.append({
                "scenario": scenario_name,
                "success": False,
                "error": test_result["error"]
            })
            print()
            continue
        
        # Get actual result
        actual_result = test_result["result"]
        
        print("✅ Model responded successfully")
        print()
        print("📊 Model Output:")
        print(json.dumps(actual_result, indent=2))
        print()
        
        # Compare with expected
        comparison = compare_results(actual_result, expected)
        
        print("🎯 Comparison with Expected Outcome:")
        print(f"Score: {comparison['score']}/{comparison['total_checks']} ({comparison['percentage']:.1f}%)")
        
        if comparison["matches"]:
            print("\n✅ Matches:")
            for key, value in comparison["matches"].items():
                print(f"  • {key}: {value}")
        
        if comparison["mismatches"]:
            print("\n⚠️  Mismatches:")
            for key, details in comparison["mismatches"].items():
                print(f"  • {key}:")
                print(f"    Expected: {details['expected']}")
                print(f"    Actual:   {details['actual']}")
        
        # Show reasoning quality
        print(f"\n💭 Reasoning Quality: {comparison['reasoning_quality']}")
        if comparison["reasoning_present"] and "reasoning_text" in comparison:
            print(f"   {comparison['reasoning_text']}")
        
        if comparison.get("has_why_suitable"):
            print(f"\n📝 Why Suitable Explanation:")
            print(f"   {comparison.get('why_suitable_text', 'N/A')}")
        
        results.append({
            "scenario": scenario_name,
            "success": True,
            "comparison": comparison,
            "actual_result": actual_result
        })
        
        print()
    
    # Print summary
    print_separator()
    print("📈 TEST SUMMARY")
    print_separator()
    
    total_tests = len(results)
    successful_tests = sum(1 for r in results if r["success"])
    failed_tests = total_tests - successful_tests
    
    if successful_tests > 0:
        avg_score = sum(r["comparison"]["percentage"] for r in results if r["success"]) / successful_tests
    else:
        avg_score = 0
    
    print(f"\nTotal scenarios tested: {total_tests}")
    print(f"Successful: {successful_tests}")
    print(f"Failed: {failed_tests}")
    
    if successful_tests > 0:
        print(f"\nAverage accuracy: {avg_score:.1f}%")
        
        print("\nDetailed Results:")
        for result in results:
            if result["success"]:
                comp = result["comparison"]
                status = "✅" if comp["percentage"] >= 75 else "⚠️"
                print(f"{status} {result['scenario']}: {comp['percentage']:.1f}% "
                      f"({comp['score']}/{comp['total_checks']})")
            else:
                print(f"❌ {result['scenario']}: Failed - {result['error']}")
    
    print()
    print_separator()
    print(f"Test completed: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
    print_separator()
    
    # Exit with appropriate code
    if failed_tests > 0:
        sys.exit(1)
    elif avg_score < 75:
        print("\n⚠️  Warning: Average accuracy below 75%")
        sys.exit(1)
    else:
        print("\n🎉 All tests passed!")
        sys.exit(0)


if __name__ == "__main__":
    main()

