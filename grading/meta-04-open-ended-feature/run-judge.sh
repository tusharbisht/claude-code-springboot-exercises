#!/usr/bin/env bash
# Boot the Spring Boot app, run the LLM judge against it, tear down.
#
# Required env:
#   ANTHROPIC_API_KEY  — your Anthropic API key (or judge will skip with status=skipped)
# Optional:
#   APP_BASE_URL       — defaults to http://localhost:8080
#   JUDGE_MODEL        — defaults to claude-sonnet-4-5
#   JUDGE_MAX_TURNS    — defaults to 30

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$ROOT_DIR"

if ! command -v python3 >/dev/null 2>&1; then
  echo "ERROR: python3 not found" >&2
  exit 2
fi

# Ensure deps are installed (idempotent).
if ! python3 -c 'import anthropic, httpx' 2>/dev/null; then
  echo "→ installing judge dependencies (anthropic, httpx)..." >&2
  pip install --quiet anthropic httpx || {
    echo "ERROR: failed to install Python dependencies" >&2
    exit 2
  }
fi

# Boot the app in the background.
echo "→ building..." >&2
mvn -B -q -DskipTests package
JAR="$(ls target/task-manager-*.jar 2>/dev/null | head -1 || true)"
if [ -z "$JAR" ]; then
  echo "ERROR: could not find built jar in target/" >&2
  exit 2
fi

echo "→ booting Spring Boot app..." >&2
java -jar "$JAR" > .judge-app.log 2>&1 &
APP_PID=$!
trap 'echo "→ shutting down app (pid $APP_PID)" >&2; kill $APP_PID 2>/dev/null || true; wait $APP_PID 2>/dev/null || true' EXIT

# Wait for app to be ready (max 60s).
APP_BASE_URL="${APP_BASE_URL:-http://localhost:8080}"
for i in $(seq 1 60); do
  if curl -fsS "$APP_BASE_URL/api/tasks" > /dev/null 2>&1; then
    echo "→ app up at $APP_BASE_URL after ${i}s" >&2
    break
  fi
  sleep 1
  if [ "$i" = "60" ]; then
    echo "ERROR: app did not start within 60s" >&2
    tail -50 .judge-app.log >&2
    exit 2
  fi
done

# Run the judge.
echo "→ running judge..." >&2
python3 "$HERE/judge.py" --base-url "$APP_BASE_URL" --out grading-result.json
JUDGE_EXIT=$?

echo "→ done (judge exit code $JUDGE_EXIT)" >&2
exit $JUDGE_EXIT
