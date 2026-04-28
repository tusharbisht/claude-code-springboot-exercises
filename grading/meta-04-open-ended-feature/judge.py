"""
LLM-driven grading judge for meta/04-open-ended-feature.

Acts as a user walking through the task labels feature, exercises happy
paths and adversarial cases, then scores the implementation against
rubric.md. Writes grading-result.json.

Usage:
  ANTHROPIC_API_KEY=sk-... python3 judge.py [--base-url http://localhost:8080]

Requires:
  pip install anthropic httpx

Designed to be runnable in CI when ANTHROPIC_API_KEY is set as a secret.
"""

from __future__ import annotations

import argparse
import json
import os
import sys
from datetime import datetime, timezone
from pathlib import Path

try:
    import anthropic
except ImportError:
    sys.stderr.write("error: anthropic package not installed. run: pip install anthropic httpx\n")
    sys.exit(2)

try:
    import httpx
except ImportError:
    sys.stderr.write("error: httpx package not installed. run: pip install anthropic httpx\n")
    sys.exit(2)


JUDGE_MODEL = os.environ.get("JUDGE_MODEL", "claude-sonnet-4-5")
MAX_TURNS = int(os.environ.get("JUDGE_MAX_TURNS", "30"))
HERE = Path(__file__).resolve().parent


def load_rubric() -> str:
    return (HERE / "rubric.md").read_text(encoding="utf-8")


def load_feature_request() -> str:
    repo_root = HERE.parent.parent
    fr = repo_root / "FEATURE_REQUEST.md"
    if fr.exists():
        return fr.read_text(encoding="utf-8")
    return "(FEATURE_REQUEST.md not found — judge will fall back to generic label-feature probing)"


def build_tools() -> list[dict]:
    return [
        {
            "name": "make_request",
            "description": (
                "Make an HTTP request against the running Spring Boot app at the configured "
                "base URL. Returns status code, headers, and parsed body."
            ),
            "input_schema": {
                "type": "object",
                "properties": {
                    "method": {"type": "string", "enum": ["GET", "POST", "PUT", "PATCH", "DELETE"]},
                    "path": {
                        "type": "string",
                        "description": "Path beginning with '/'. Will be appended to the base URL.",
                    },
                    "body": {
                        "type": ["object", "array", "null"],
                        "description": "JSON body for POST/PUT/PATCH; omit or null for GET/DELETE.",
                    },
                },
                "required": ["method", "path"],
            },
        },
        {
            "name": "submit_grade",
            "description": (
                "Submit the final grade. Call this exactly once at the end of the session, "
                "after you've probed enough of the feature to score confidently."
            ),
            "input_schema": {
                "type": "object",
                "properties": {
                    "scores": {
                        "type": "object",
                        "properties": {
                            "functional_correctness": {"type": "integer", "minimum": 0, "maximum": 10},
                            "rest_hygiene": {"type": "integer", "minimum": 0, "maximum": 10},
                            "validation": {"type": "integer", "minimum": 0, "maximum": 10},
                            "error_handling": {"type": "integer", "minimum": 0, "maximum": 10},
                            "codebase_consistency": {"type": "integer", "minimum": 0, "maximum": 10},
                            "edge_case_design": {"type": "integer", "minimum": 0, "maximum": 10},
                        },
                        "required": [
                            "functional_correctness",
                            "rest_hygiene",
                            "validation",
                            "error_handling",
                            "codebase_consistency",
                            "edge_case_design",
                        ],
                    },
                    "critique": {
                        "type": "string",
                        "description": (
                            "Multi-paragraph critique citing specific evidence. Each paragraph "
                            "should reference a request you made, the response observed, and "
                            "what it implies for the score."
                        ),
                    },
                },
                "required": ["scores", "critique"],
            },
        },
    ]


def make_request(client: httpx.Client, base_url: str, method: str, path: str, body) -> dict:
    url = base_url.rstrip("/") + path
    try:
        if method in ("GET", "DELETE"):
            r = client.request(method, url, timeout=10.0)
        else:
            r = client.request(method, url, json=body, timeout=10.0)
    except httpx.HTTPError as exc:
        return {"error": f"http error: {type(exc).__name__}: {exc}"}

    try:
        parsed = r.json()
    except ValueError:
        parsed = r.text

    return {
        "status": r.status_code,
        "content_type": r.headers.get("content-type", ""),
        "body": parsed,
    }


def run_judge(base_url: str) -> dict:
    api_key = os.environ.get("ANTHROPIC_API_KEY")
    if not api_key:
        return {
            "status": "skipped",
            "reason": "ANTHROPIC_API_KEY not set — manual review needed",
            "timestamp": datetime.now(timezone.utc).isoformat(),
        }

    rubric = load_rubric()
    feature_request = load_feature_request()
    client = anthropic.Anthropic(api_key=api_key)
    http = httpx.Client()

    system = (
        "You are a strict senior reviewer grading a junior engineer's implementation of a "
        "task-labels feature on a Spring Boot REST API. Your job is to walk through the "
        "feature as a real user would (curl-style, via the make_request tool), probe edge "
        "cases, and produce a grade against the rubric.\n\n"
        "Be skeptical. Common mistakes (silent failures, 500 instead of 400, non-idempotent "
        "writes, inconsistent error shapes) should cost points. Cite specific evidence in "
        "your critique — each paragraph should reference an actual request and response.\n\n"
        "End the session by calling submit_grade exactly once."
    )

    user_intro = (
        f"# Feature spec\n\n{feature_request}\n\n"
        f"# Rubric\n\n{rubric}\n\n"
        f"# Your task\n\n"
        f"The app is running at the base URL configured in your tools. Walk through the "
        f"feature as a user, probe edge cases, then call submit_grade.\n\n"
        f"You have at most {MAX_TURNS} tool-call turns. Use them efficiently."
    )

    messages = [{"role": "user", "content": user_intro}]
    tools = build_tools()
    turns_used = 0
    final_grade = None

    while turns_used < MAX_TURNS:
        turns_used += 1
        resp = client.messages.create(
            model=JUDGE_MODEL,
            max_tokens=4096,
            system=system,
            tools=tools,
            messages=messages,
        )
        # Append assistant message
        messages.append({"role": "assistant", "content": resp.content})

        if resp.stop_reason == "end_turn":
            break

        if resp.stop_reason != "tool_use":
            break

        tool_results = []
        for block in resp.content:
            if block.type != "tool_use":
                continue
            if block.name == "make_request":
                result = make_request(
                    http,
                    base_url,
                    block.input.get("method", "GET"),
                    block.input.get("path", "/"),
                    block.input.get("body"),
                )
                tool_results.append({
                    "type": "tool_result",
                    "tool_use_id": block.id,
                    "content": json.dumps(result)[:8000],
                })
                print(f"  → {block.input.get('method')} {block.input.get('path')} → "
                      f"{result.get('status', result.get('error', '?'))}", file=sys.stderr)
            elif block.name == "submit_grade":
                final_grade = block.input
                tool_results.append({
                    "type": "tool_result",
                    "tool_use_id": block.id,
                    "content": "grade recorded",
                })
        messages.append({"role": "user", "content": tool_results})
        if final_grade is not None:
            break

    if final_grade is None:
        return {
            "status": "incomplete",
            "reason": "judge did not call submit_grade within max turns",
            "turns_used": turns_used,
            "timestamp": datetime.now(timezone.utc).isoformat(),
        }

    scores = final_grade["scores"]
    total = sum(scores.values())

    return {
        "status": "graded",
        "total": total,
        "max_total": 60,
        "scores": scores,
        "critique": final_grade["critique"],
        "turns_used": turns_used,
        "timestamp": datetime.now(timezone.utc).isoformat(),
    }


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--base-url", default=os.environ.get("APP_BASE_URL", "http://localhost:8080"))
    parser.add_argument("--out", default="grading-result.json")
    args = parser.parse_args()

    print(f"→ judge model: {JUDGE_MODEL}", file=sys.stderr)
    print(f"→ base URL:    {args.base_url}", file=sys.stderr)
    print(f"→ max turns:   {MAX_TURNS}", file=sys.stderr)

    result = run_judge(args.base_url)
    Path(args.out).write_text(json.dumps(result, indent=2), encoding="utf-8")
    print(f"\n→ result written to {args.out}", file=sys.stderr)
    print(json.dumps(result, indent=2))

    if result.get("status") == "graded":
        sys.exit(0 if result["total"] >= 36 else 1)  # 60% threshold
    elif result.get("status") == "skipped":
        sys.exit(0)
    else:
        sys.exit(1)


if __name__ == "__main__":
    main()
