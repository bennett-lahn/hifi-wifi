"""
Simple Flask REST API wrapper for Ollama WiFi Optimization Service

This provides a simple HTTP REST API endpoint for Android apps to send
WiFi measurements and receive AI-powered recommendations.

Usage:
    python3 simple_api.py

The API will be available at http://0.0.0.0:5000/analyze
"""

from flask import Flask, request, jsonify
from flask_cors import CORS
from ollama_service import OllamaService, OllamaConfig
import logging

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

# Initialize Flask app
app = Flask(__name__)
CORS(app)  # Enable CORS for cross-origin requests

# Initialize Ollama service
config = OllamaConfig(
    base_url="http://localhost:11434",
    model_name="wifi-assistant",
    timeout=30
)
service = OllamaService(config)


@app.route('/health', methods=['GET'])
def health_check():
    """
    Health check endpoint to verify service is running.
    
    Returns:
        JSON with service status and Ollama availability
    """
    ollama_healthy = service.health_check()
    
    return jsonify({
        "status": "healthy" if ollama_healthy else "degraded",
        "service": "WiFi Optimization API",
        "ollama_available": ollama_healthy,
        "version": "1.0.0"
    }), 200 if ollama_healthy else 503


@app.route('/analyze', methods=['POST'])
def analyze_wifi():
    """
    Analyze WiFi measurement data and provide recommendations.
    
    Request Body (JSON):
        {
            "location": str,          # Room name (e.g., "living_room")
            "signal_dbm": int,        # RSSI in dBm (-30 to -90)
            "link_speed_mbps": int,   # Link speed in Mbps
            "latency_ms": int,        # Latency in milliseconds
            "frequency": str,         # "2.4GHz" or "5GHz"
            "activity": str           # "gaming", "video_call", etc.
        }
    
    Returns:
        JSON with recommendation and analysis, or error message
    """
    try:
        # Validate request has JSON body
        if not request.is_json:
            logger.warning("Request missing JSON body")
            return jsonify({
                "status": "error",
                "error": "Request must include JSON body with Content-Type: application/json"
            }), 400
        
        measurement = request.json
        
        # Validate required fields (Android sends pre-classified values)
        required_fields = [
            "location", "signal_strength", "latency", "bandwidth",
            "jitter", "packet_loss", "frequency", "activity"
        ]
        
        missing_fields = [field for field in required_fields if field not in measurement]
        if missing_fields:
            logger.warning(f"Missing required fields: {missing_fields}")
            return jsonify({
                "status": "error",
                "error": f"Missing required fields: {', '.join(missing_fields)}"
            }), 400
        
        # Validate classification values
        valid_classifications = ["excellent", "good", "okay", "bad", "marginal"]
        
        try:
            signal_strength = measurement["signal_strength"].lower()
            latency = measurement["latency"].lower()
            bandwidth = measurement["bandwidth"].lower()
            jitter = measurement["jitter"].lower()
            packet_loss = measurement["packet_loss"].lower()
            
            if signal_strength not in valid_classifications:
                raise ValueError(f"signal_strength must be one of: {', '.join(valid_classifications)}")
            if latency not in valid_classifications:
                raise ValueError(f"latency must be one of: {', '.join(valid_classifications)}")
            if bandwidth not in valid_classifications:
                raise ValueError(f"bandwidth must be one of: {', '.join(valid_classifications)}")
            if jitter not in valid_classifications:
                raise ValueError(f"jitter must be one of: {', '.join(valid_classifications)}")
            if packet_loss not in valid_classifications:
                raise ValueError(f"packet_loss must be one of: {', '.join(valid_classifications)}")
                
        except (ValueError, TypeError, AttributeError) as e:
            logger.warning(f"Invalid classification values: {e}")
            return jsonify({
                "status": "error",
                "error": f"Invalid classification values: {str(e)}"
            }), 400
        
        # Use pre-classified measurements from Android
        classified_measurement = {
            "location": measurement["location"],
            "signal_strength": signal_strength,
            "latency": latency,
            "bandwidth": bandwidth,
            "jitter": jitter,
            "packet_loss": packet_loss,
            "frequency": measurement["frequency"],
            "activity": measurement["activity"]
        }
        
        # Log the request
        logger.info(f"Analyzing WiFi: {measurement['location']} - "
                   f"Signal: {signal_strength}, Latency: {latency}, "
                   f"Bandwidth: {bandwidth}, Jitter: {jitter}, "
                   f"Packet Loss: {packet_loss}, Activity: {measurement['activity']}")
        
        # Call Ollama service with classified data
        result = service.analyze_wifi_measurement(classified_measurement)
        
        # Log the result
        if result.get("status") == "success":
            action = result.get("recommendation", {}).get("action", "unknown")
            logger.info(f"Analysis complete: {action}")
        else:
            logger.error(f"Analysis failed: {result.get('error')}")
        
        return jsonify(result), 200
        
    except Exception as e:
        logger.error(f"Unexpected error: {str(e)}", exc_info=True)
        return jsonify({
            "status": "error",
            "error": f"Internal server error: {str(e)}"
        }), 500


@app.route('/explain', methods=['POST'])
def explain_recommendation():
    """
    Generate conversational explanation for a WiFi recommendation.
    Android app has already classified measurements and made a decision.
    This endpoint explains WHY that decision makes sense.
    
    Request Body (JSON):
        {
            "location": str,           # Room name (e.g., "living_room")
            "activity": str,           # What user is doing (e.g., "gaming")
            "measurements": {
                "signal_strength": str,  # "excellent", "good", "fair", "poor", "very_poor"
                "latency": str,          # "excellent", "good", "fair", "poor"
                "bandwidth": str         # "excellent", "good", "fair", "poor"
            },
            "recommendation": {
                "action": str,           # "stay_current", "move_location", "switch_band"
                "target_location": str   # Optional, for move_location
            }
        }
    
    Returns:
        JSON with conversational explanation text
    """
    try:
        if not request.is_json:
            logger.warning("Request missing JSON body")
            return jsonify({
                "status": "error",
                "error": "Request must include JSON body with Content-Type: application/json"
            }), 400
        
        data = request.json
        
        # Validate required fields
        required = ["location", "activity", "measurements", "recommendation"]
        missing = [f for f in required if f not in data]
        if missing:
            logger.warning(f"Missing required fields: {missing}")
            return jsonify({
                "status": "error",
                "error": f"Missing required fields: {', '.join(missing)}"
            }), 400
        
        # Validate measurements
        measurements = data["measurements"]
        required_measurements = ["signal_strength", "latency", "bandwidth"]
        missing_measurements = [f for f in required_measurements if f not in measurements]
        if missing_measurements:
            logger.warning(f"Missing measurement fields: {missing_measurements}")
            return jsonify({
                "status": "error",
                "error": f"Missing measurement fields: {', '.join(missing_measurements)}"
            }), 400
        
        # Validate recommendation
        recommendation = data["recommendation"]
        if "action" not in recommendation:
            logger.warning("Missing action in recommendation")
            return jsonify({
                "status": "error",
                "error": "Missing 'action' in recommendation"
            }), 400
        
        # Log the request
        logger.info(f"Explaining: {data['location']} - {data['activity']} - {recommendation['action']}")
        
        # Call service to generate explanation
        result = service.explain_wifi_recommendation(
            location=data["location"],
            activity=data["activity"],
            measurements=measurements,
            recommendation=recommendation
        )
        
        # Log the result
        if result.get("status") == "success":
            logger.info(f"Explanation generated: {len(result.get('explanation', ''))} chars")
        else:
            logger.error(f"Explanation failed: {result.get('error')}")
        
        return jsonify(result), 200
        
    except Exception as e:
        logger.error(f"Unexpected error in /explain: {str(e)}", exc_info=True)
        return jsonify({
            "status": "error",
            "error": f"Internal server error: {str(e)}"
        }), 500


@app.route('/chat', methods=['POST'])
def chat_query():
    """
    Send a natural language query about WiFi optimization.
    
    Request Body (JSON):
        {
            "query": str,              # Natural language question
            "format_json": bool        # Optional, default False
        }
    
    Returns:
        JSON with response text or structured JSON
    """
    try:
        if not request.is_json:
            return jsonify({
                "status": "error",
                "error": "Request must include JSON body"
            }), 400
        
        data = request.json
        query = data.get("query")
        format_json = data.get("format_json", False)
        
        if not query:
            return jsonify({
                "status": "error",
                "error": "Missing 'query' field"
            }), 400
        
        logger.info(f"Chat query: {query[:50]}...")
        
        result = service.chat_query(query, format_json=format_json)
        
        return jsonify(result), 200
        
    except Exception as e:
        logger.error(f"Chat query error: {str(e)}", exc_info=True)
        return jsonify({
            "status": "error",
            "error": str(e)
        }), 500


@app.errorhandler(404)
def not_found(error):
    """Handle 404 errors."""
    return jsonify({
        "status": "error",
        "error": "Endpoint not found",
        "available_endpoints": [
            "GET /health",
            "POST /analyze",
            "POST /explain",
            "POST /chat"
        ]
    }), 404


@app.errorhandler(500)
def internal_error(error):
    """Handle 500 errors."""
    logger.error(f"Internal server error: {error}")
    return jsonify({
        "status": "error",
        "error": "Internal server error"
    }), 500


if __name__ == '__main__':
    # Check Ollama service availability on startup
    logger.info("Starting WiFi Optimization API...")
    
    if service.health_check():
        logger.info("✅ Ollama service is accessible")
    else:
        logger.warning("⚠️  Ollama service is not accessible at localhost:11434")
        logger.warning("Please ensure Ollama is running: ollama serve")
    
    # Start Flask server
    logger.info("Starting Flask server on http://0.0.0.0:5000")
    logger.info("API endpoints:")
    logger.info("  GET  /health  - Health check")
    logger.info("  POST /analyze - WiFi analysis (accepts classified measurements)")
    logger.info("  POST /explain - Get friendly explanation for recommendation")
    logger.info("  POST /chat    - Natural language queries")
    
    app.run(
        host='0.0.0.0',  # Accessible from any device on network
        port=5000,
        debug=False,      # Set to True for development
        threaded=True     # Handle multiple requests
    )

