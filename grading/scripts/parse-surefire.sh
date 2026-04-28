#!/usr/bin/env bash
# Parses target/surefire-reports/*.xml and emits a single-line JSON summary
# to stdout. Designed to be machine-readable for CI and Claude Code hooks.
#
# Usage: parse-surefire.sh <branch> <maven-exit-code>

set -euo pipefail

BRANCH="${1:-unknown}"
MVN_EXIT="${2:-0}"
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
REPORTS="$ROOT_DIR/target/surefire-reports"

TOTAL=0
FAILURES=0
ERRORS=0
SKIPPED=0

if [ -d "$REPORTS" ]; then
  for f in "$REPORTS"/TEST-*.xml; do
    [ -f "$f" ] || continue
    # Extract attributes from the <testsuite ...> opening tag.
    LINE="$(grep -m1 -E '<testsuite ' "$f" || true)"
    [ -z "$LINE" ] && continue
    T=$(echo "$LINE"  | sed -nE 's/.*tests="([0-9]+)".*/\1/p')
    F=$(echo "$LINE"  | sed -nE 's/.*failures="([0-9]+)".*/\1/p')
    E=$(echo "$LINE"  | sed -nE 's/.*errors="([0-9]+)".*/\1/p')
    S=$(echo "$LINE"  | sed -nE 's/.*skipped="([0-9]+)".*/\1/p')
    TOTAL=$((TOTAL + ${T:-0}))
    FAILURES=$((FAILURES + ${F:-0}))
    ERRORS=$((ERRORS + ${E:-0}))
    SKIPPED=$((SKIPPED + ${S:-0}))
  done
fi

PASSED=$((TOTAL - FAILURES - ERRORS - SKIPPED))
STATUS="passed"
if [ "$MVN_EXIT" -ne 0 ] || [ "$FAILURES" -gt 0 ] || [ "$ERRORS" -gt 0 ]; then
  STATUS="failed"
fi

TIMESTAMP="$(date -u +%Y-%m-%dT%H:%M:%SZ)"

cat <<EOF
{"branch":"$BRANCH","status":"$STATUS","total":$TOTAL,"passed":$PASSED,"failures":$FAILURES,"errors":$ERRORS,"skipped":$SKIPPED,"mavenExitCode":$MVN_EXIT,"timestamp":"$TIMESTAMP"}
EOF
