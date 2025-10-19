"""
Ollama WiFi Optimization Service

A clean Python interface to communicate with the local Ollama server API
for WiFi network optimization and analysis.
"""

import json
import requests
from typing import Dict, Any, Optional, Union
from dataclasses import dataclass
import time


@dataclass
class OllamaConfig:
    """Configuration for Ollama service connection."""
    base_url: str = "http://localhost:11434"
    model_name: str = "wifi-assistant"  # Created from Modelfile using: ollama create wifi-assistant -f Modelfile
    timeout: int = 30
    max_retries: int = 3


class OllamaService:
    """
    Service class for interfacing with Ollama HTTP API.
    
    Provides methods for WiFi optimization recommendations and natural language queries.
    """
    
    def __init__(self, config: Optional[OllamaConfig] = None):
        """
        Initialize the Ollama service.
        
        Args:
            config: OllamaConfig instance with connection settings
        """
        self.config = config or OllamaConfig()
        self.api_url = f"{self.config.base_url}/api/generate"
        
    def _make_request(
        self, 
        prompt: str, 
        format_json: bool = True,
        stream: bool = False
    ) -> Dict[str, Any]:
        """
        Make a request to the Ollama API.
        
        Args:
            prompt: The prompt text to send
            format_json: Whether to request JSON formatted response
            stream: Whether to stream the response
            
        Returns:
            Dictionary containing the API response
            
        Raises:
            ConnectionError: If unable to connect to Ollama server
            TimeoutError: If request times out
            ValueError: If response is invalid JSON
        """
        payload = {
            "model": self.config.model_name,
            "prompt": prompt,
            "stream": stream
        }
        
        if format_json:
            payload["format"] = "json"
        
        for attempt in range(self.config.max_retries):
            try:
                response = requests.post(
                    self.api_url,
                    json=payload,
                    timeout=self.config.timeout
                )
                response.raise_for_status()
                
                result = response.json()
                return result
                
            except requests.exceptions.ConnectionError as e:
                if attempt == self.config.max_retries - 1:
                    raise ConnectionError(
                        f"Failed to connect to Ollama server at {self.config.base_url}. "
                        f"Please ensure Ollama is running."
                    ) from e
                time.sleep(1)  # Wait before retry
                
            except requests.exceptions.Timeout as e:
                if attempt == self.config.max_retries - 1:
                    raise TimeoutError(
                        f"Request timed out after {self.config.timeout} seconds."
                    ) from e
                time.sleep(1)
                
            except requests.exceptions.HTTPError as e:
                raise ConnectionError(
                    f"HTTP error from Ollama server: {e.response.status_code} - {e.response.text}"
                ) from e
                
            except json.JSONDecodeError as e:
                raise ValueError(
                    f"Invalid JSON response from Ollama server: {e}"
                ) from e
    
    def _parse_response(self, raw_response: Dict[str, Any]) -> Dict[str, Any]:
        """
        Parse the raw Ollama API response.
        
        Args:
            raw_response: Raw response from Ollama API
            
        Returns:
            Parsed response dictionary
            
        Raises:
            ValueError: If response format is invalid
        """
        if not raw_response.get("done", False):
            raise ValueError("Incomplete response from Ollama server")
        
        response_text = raw_response.get("response", "")
        
        if not response_text:
            raise ValueError("Empty response from Ollama server")
        
        try:
            # Parse JSON response
            parsed = json.loads(response_text)
            return parsed
        except json.JSONDecodeError as e:
            # If not JSON, return as plain text
            return {"response": response_text, "error": "Non-JSON response"}
    
    def analyze_wifi_measurement(
        self, 
        measurement: Dict[str, Union[str, int, float]]
    ) -> Dict[str, Any]:
        """
        Analyze WiFi measurement data with pre-classified values.
        
        Args:
            measurement: Dictionary containing WiFi measurement data with keys:
                - location: str (e.g., "living_room")
                - signal_strength: str (e.g., "excellent", "good", "fair", "poor", "very_poor")
                - signal_dbm: int (raw value for reference)
                - latency: str (e.g., "excellent", "good", "fair", "poor")
                - latency_ms: int (raw value for reference)
                - bandwidth: str (e.g., "excellent", "good", "fair", "poor")
                - link_speed_mbps: int (raw value for reference)
                - frequency: str (e.g., "5GHz", "2.4GHz")
                - activity: str (e.g., "gaming", "video_call", "streaming", "browsing")
        
        Returns:
            Dictionary with status, recommendation with reasoning, and detailed analysis
            
        Example:
            >>> measurement = {
            ...     "location": "living_room",
            ...     "signal_strength": "excellent",
            ...     "signal_dbm": -45,
            ...     "latency": "excellent",
            ...     "latency_ms": 12,
            ...     "bandwidth": "excellent",
            ...     "link_speed_mbps": 866,
            ...     "frequency": "5GHz",
            ...     "activity": "gaming"
            ... }
            >>> result = service.analyze_wifi_measurement(measurement)
        """
        try:
            # Construct prompt with classified values only (no raw numbers)
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

            # Make API request
            raw_response = self._make_request(prompt, format_json=True)
            
            # Parse response
            result = self._parse_response(raw_response)
            
            # Validate response structure
            if "status" not in result:
                result = {"status": "success", **result}
            
            return result
            
        except (ConnectionError, TimeoutError, ValueError) as e:
            return {
                "status": "error",
                "error": str(e),
                "recommendation": None,
                "analysis": None
            }
    
    def explain_wifi_recommendation(
        self,
        location: str,
        activity: str,
        measurements: Dict[str, str],
        recommendation: Dict[str, str]
    ) -> Dict[str, Any]:
        """
        Generate a conversational explanation for a WiFi recommendation.
        
        The Android app has already classified measurements and made a decision.
        This method asks the LLM to explain WHY that decision makes sense.
        
        Args:
            location: Room name (e.g., "living_room")
            activity: Current activity (e.g., "gaming", "streaming", "video_call", "browsing")
            measurements: Dict with pre-classified values:
                - signal_strength: "excellent", "good", "fair", "poor", "very_poor"
                - latency: "excellent", "good", "fair", "poor"
                - bandwidth: "excellent", "good", "fair", "poor"
            recommendation: Dict with decision made by Android:
                - action: "stay_current", "move_location", or "switch_band"
                - target_location: Optional suggested location (for move_location)
        
        Returns:
            Dictionary with conversational explanation text
            
        Example:
            >>> result = service.explain_wifi_recommendation(
            ...     "living_room", "gaming",
            ...     {"signal_strength": "excellent", "latency": "excellent", "bandwidth": "excellent"},
            ...     {"action": "stay_current"}
            ... )
            >>> print(result['explanation'])
        """
        try:
            # Build action description
            action = recommendation.get('action', 'stay_current')
            action_desc = {
                "stay_current": "stay where you are",
                "move_location": f"move to {recommendation.get('target_location', 'a different location')}",
                "switch_band": "switch to a faster network option"
            }
            
            # Construct conversational prompt
            prompt = f"""The user is in the {location} and they're {activity}.

Current WiFi status:
- Signal strength: {measurements.get('signal_strength', 'unknown')}
- Response time (latency): {measurements.get('latency', 'unknown')}
- Speed (bandwidth): {measurements.get('bandwidth', 'unknown')}

The recommendation is to {action_desc.get(action, action)}.

Explain to the user in a friendly, conversational way WHY this recommendation makes sense for their situation. Remember to:
- Keep it brief (3-5 sentences)
- Use everyday language, not technical jargon
- Focus on their {activity} experience
- Be positive and encouraging"""

            # Make API request (plain text, not JSON)
            raw_response = self._make_request(prompt, format_json=False)
            
            if raw_response.get("done", False):
                explanation = raw_response.get("response", "").strip()
                return {
                    "status": "success",
                    "explanation": explanation,
                    "metadata": {
                        "location": location,
                        "activity": activity,
                        "recommendation": action
                    }
                }
            else:
                return {
                    "status": "error",
                    "error": "Incomplete response from model",
                    "explanation": None
                }
            
        except (ConnectionError, TimeoutError, ValueError) as e:
            return {
                "status": "error",
                "error": str(e),
                "explanation": None
            }
    
    def chat_query(self, query: str, format_json: bool = False) -> Dict[str, Any]:
        """
        Send a natural language query to the WiFi optimization assistant.
        
        Args:
            query: Natural language question or request
            format_json: Whether to request JSON formatted response
            
        Returns:
            Dictionary containing the response
            
        Example:
            >>> result = service.chat_query("How can I improve my WiFi signal?")
            >>> print(result['response'])
        """
        try:
            raw_response = self._make_request(query, format_json=format_json)
            
            if format_json:
                return self._parse_response(raw_response)
            else:
                # Return plain text response
                return {
                    "status": "success",
                    "response": raw_response.get("response", ""),
                    "done": raw_response.get("done", False)
                }
                
        except (ConnectionError, TimeoutError, ValueError) as e:
            return {
                "status": "error",
                "error": str(e),
                "response": None
            }
    
    def health_check(self) -> bool:
        """
        Check if Ollama server is accessible.
        
        Returns:
            True if server is accessible, False otherwise
        """
        try:
            response = requests.get(
                f"{self.config.base_url}/api/tags",
                timeout=5
            )
            return response.status_code == 200
        except:
            return False


def main():
    """Example usage of OllamaService."""
    
    # Initialize service with custom config
    config = OllamaConfig(
        base_url="http://localhost:11434",
        model_name="wifi-assistant",
        timeout=30
    )
    service = OllamaService(config)
    
    # Check if Ollama server is running
    print("Checking Ollama server health...")
    if not service.health_check():
        print("❌ Error: Ollama server is not accessible at localhost:11434")
        print("Please ensure Ollama is running and the model is loaded.")
        return
    
    print("✅ Ollama server is accessible\n")
    
    # Example 1: Analyze WiFi measurement
    print("=" * 60)
    print("Example 1: WiFi Measurement Analysis")
    print("=" * 60)
    
    test_measurement = {
        "location": "living_room",
        "signal_strength": "excellent",
        "latency": "excellent",
        "bandwidth": "excellent",
        "jitter": "excellent",
        "packet_loss": "excellent",
        "frequency": "5GHz",
        "activity": "gaming"
    }
    
    print(f"\nInput measurement:")
    print(json.dumps(test_measurement, indent=2))
    
    print("\nAnalyzing...")
    result = service.analyze_wifi_measurement(test_measurement)
    
    print(f"\nResult:")
    print(json.dumps(result, indent=2))
    
    # Example 2: Poor signal scenario
    print("\n" + "=" * 60)
    print("Example 2: Poor Signal Scenario")
    print("=" * 60)
    
    poor_signal_measurement = {
        "location": "bedroom",
        "signal_strength": "bad",
        "latency": "good",
        "bandwidth": "okay",
        "jitter": "okay",
        "packet_loss": "good",
        "frequency": "2.4GHz",
        "activity": "video_call"
    }
    
    print(f"\nInput measurement:")
    print(json.dumps(poor_signal_measurement, indent=2))
    
    print("\nAnalyzing...")
    result = service.analyze_wifi_measurement(poor_signal_measurement)
    
    print(f"\nResult:")
    print(json.dumps(result, indent=2))
    
    # Example 3: Natural language chat query
    print("\n" + "=" * 60)
    print("Example 3: Natural Language Query")
    print("=" * 60)
    
    query = "What is the ideal signal strength for video conferencing?"
    print(f"\nQuery: {query}")
    
    print("\nQuerying...")
    result = service.chat_query(query, format_json=False)
    
    print(f"\nResponse:")
    if result["status"] == "success":
        print(result["response"])
    else:
        print(f"Error: {result['error']}")


if __name__ == "__main__":
    main()

