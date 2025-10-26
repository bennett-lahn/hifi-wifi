#!/usr/bin/env python3
"""
Explainer Scenario Tests

Runs the updated test-scenarios against the explainer flow using
OllamaService.explain_wifi_recommendation and validates:
- Contains key phrases (should_contain)
- Avoids jargon (should_avoid)
- Keeps it concise (max_sentences)

Usage:
  python3 test_explainer_scenarios.py
"""

import json
from pathlib import Path
import re
from typing import Dict, Any, List

from ollama_service import OllamaService, OllamaConfig

ROOT = Path(__file__).parent
SCENARIOS_DIR = ROOT / "test-scenarios"


def split_sentences(text: str) -> List[str]:
    # Simple sentence splitter
    parts = re.split(r"(?<=[.!?])\s+", text.strip())
    return [p.strip() for p in parts if p.strip()]


def validate_explanation(text: str, expected: Dict[str, Any]) -> Dict[str, Any]:
    results = {"contains": [], "missing": [], "avoided": [], "violations": [], "sentences": 0}
    lower = text.lower()

    # Contains checks
    for phrase in expected.get("should_contain", []):
        if phrase.lower() in lower:
            results["contains"].append(phrase)
        else:
            results["missing"].append(phrase)

    # Avoid checks
    for phrase in expected.get("should_avoid", []):
        if phrase.lower() in lower:
            results["violations"].append(phrase)
        else:
            results["avoided"].append(phrase)

    # Sentence count
    results["sentences"] = len(split_sentences(text))
    max_sentences = expected.get("max_sentences")
    if isinstance(max_sentences, int) and results["sentences"] > max_sentences:
        results["violations"].append(f"too_long:{results['sentences']}>)")

    return results


def run_scenario(service: OllamaService, scenario_path: Path) -> Dict[str, Any]:
    data = json.loads(scenario_path.read_text())
    name = data.get("scenario_name", scenario_path.name)
    scenario_input = data.get("input")
    expected = data.get("expected_explanation", {})

    if not scenario_input:
        return {"name": name, "status": "error", "error": "Missing 'input' in scenario"}

    location = scenario_input.get("location")
    activity = scenario_input.get("activity")
    measurements = scenario_input.get("measurements", {})
    recommendation = scenario_input.get("recommendation", {})

    result = service.explain_wifi_recommendation(
        location=location,
        activity=activity,
        measurements=measurements,
        recommendation=recommendation,
    )

    if result.get("status") != "success":
        return {"name": name, "status": "error", "error": result.get("error", "unknown")}

    explanation = result.get("explanation", "")
    checks = validate_explanation(explanation, expected)

    passed = (len(checks["missing"]) == 0 and len(checks["violations"]) == 0)

    return {
        "name": name,
        "status": "passed" if passed else "failed",
        "explanation": explanation,
        "checks": checks,
    }


ess = OllamaService(OllamaConfig())

def main():
    scenarios = sorted(SCENARIOS_DIR.glob("scenario_*.json"))
    if not scenarios:
        print("No scenarios found.")
        return

    print("=" * 80)
    print("WiFi Assistant - Explainer Scenario Tests")
    print("=" * 80)

    results = []
    for path in scenarios:
        print("-" * 80)
        print(f"Running: {path.name}")
        res = run_scenario(ess, path)
        results.append(res)

        if res["status"] == "error":
            print(f"❌ Error: {res['error']}")
            continue

        print(f"Status: {res['status']}")
        print(f"Explanation:\n{res['explanation']}")
        checks = res["checks"]
        print(f"Contains: {checks['contains']}")
        if checks["missing"]:
            print(f"Missing: {checks['missing']}")
        if checks["violations"]:
            print(f"Violations: {checks['violations']}")
        print(f"Sentences: {checks['sentences']}")

    # Summary
    print("\n" + "=" * 80)
    total = len(results)
    passed = sum(1 for r in results if r["status"] == "passed")
    failed = total - passed
    print(f"Summary: {passed}/{total} passed, {failed} failed")

    # Exit code
    import sys
    sys.exit(0 if failed == 0 else 1)


if __name__ == "__main__":
    main()
