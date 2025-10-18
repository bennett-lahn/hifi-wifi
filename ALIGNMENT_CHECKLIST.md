# Alignment Checklist: Modelfile ↔ Python Service

This document verifies that the Python service (`ollama_service.py`) is fully aligned with the Modelfile specifications.

## ✅ Model Configuration

| Aspect | Modelfile | Python Service | Aligned? |
|--------|-----------|----------------|----------|
| Base Model | `qwen3:0.6b` | Uses `wifi-assistant` (created from Modelfile) | ✅ |
| Temperature | `0.7` | Default config | ✅ |
| Top P | `0.9` | Default config | ✅ |
| Top K | `40` | Default config | ✅ |

## ✅ Response Format Alignment

### Action Types
| Modelfile | Python Service | Aligned? |
|-----------|----------------|----------|
| `move_location` | `move_location` | ✅ |
| `switch_band` | `switch_band` | ✅ |
| `stay_current` | `stay_current` | ✅ |

### Quality Levels
| Modelfile | Python Service | Aligned? |
|-----------|----------------|----------|
| `excellent` | `excellent` | ✅ |
| `good` | `good` | ✅ |
| `moderate` | `moderate` | ✅ |
| `poor` | `poor` | ✅ |
| `very_poor` | `very_poor` | ✅ |

### Bottleneck Types
| Modelfile | Python Service | Aligned? |
|-----------|----------------|----------|
| `signal_strength` | `signal_strength` | ✅ |
| `latency` | `latency` | ✅ |
| `bandwidth` | `bandwidth` | ✅ |
| `none` | `none` | ✅ |

### Priority Levels
| Modelfile | Python Service | Aligned? |
|-----------|----------------|----------|
| `high` | `high` | ✅ |
| `medium` | `medium` | ✅ |
| `low` | `low` | ✅ |

## ✅ Signal Strength Scale (RSSI)

Both Modelfile and documentation use the same scale:
- `-30 to -50 dBm`: Excellent signal
- `-50 to -60 dBm`: Good signal
- `-60 to -70 dBm`: Fair signal (may have issues)
- `-70 to -80 dBm`: Poor signal (slow speeds, dropouts)
- `-80 to -90 dBm`: Very poor signal (barely usable)

## ✅ Latency Scale

Both aligned on:
- `<20ms`: Excellent for gaming and video calls
- `20-50ms`: Good for most activities
- `50-100ms`: Noticeable lag in gaming/calls
- `>100ms`: Poor, frustrating experience

## ✅ Activity Requirements

| Activity | Modelfile Requirements | Aligned? |
|----------|----------------------|----------|
| Gaming | `<20ms latency, >-60 dBm signal, prefer 5GHz` | ✅ |
| Video calls | `<50ms latency, stable signal >-65 dBm` | ✅ |
| Streaming | `good bandwidth, >-70 dBm signal acceptable` | ✅ |
| Browsing | `less demanding, >-75 dBm usually okay` | ✅ |

## ✅ JSON Response Structure

The Python service prompt exactly matches the Modelfile expected format:

```json
{
  "status": "success",
  "recommendation": {
    "action": "move_location" | "switch_band" | "stay_current",
    "priority": "high" | "medium" | "low",
    "message": "string",
    "target_location": "string" | null,
    "expected_improvements": {
      "rssi_dbm": number,
      "latency_ms": number
    }
  },
  "analysis": {
    "current_quality": "excellent" | "good" | "moderate" | "poor" | "very_poor",
    "signal_rating": 0-10,
    "suitable_for_activity": boolean,
    "bottleneck": "signal_strength" | "latency" | "bandwidth" | "none"
  }
}
```

## ✅ API Communication

| Aspect | Implementation | Aligned? |
|--------|----------------|----------|
| Endpoint | `POST /api/generate` | ✅ |
| Format Request | `"format": "json"` | ✅ |
| Stream Disabled | `"stream": false` | ✅ |
| Model Name | `"wifi-assistant"` | ✅ |

## ✅ Test Scenarios

All test scenarios updated to use correct terminology:
- ✅ `scenario_1_excellent.json` - Uses `stay_current`
- ✅ `scenario_2_poor.json` - Uses `move_location`
- ✅ `scenario_3_moderate.json` - Uses `stay_current`
- ✅ `scenario_4_switch_band.json` - Uses `switch_band`

## Summary

**All components are now fully aligned!** ✅

The Python service (`ollama_service.py`) will:
1. Send WiFi measurements to the Ollama model
2. Request JSON-formatted responses
3. Receive responses matching the exact structure defined in the Modelfile
4. Parse and return clean JSON with proper typing

No mismatches in terminology, data structures, or expected behaviors.

