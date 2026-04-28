#!/usr/bin/env bash
# Claude Code Stop hook — runs after each Claude Code session ends.
#
# Behavior:
#   1. Detects the current exercise branch.
#   2. Runs `mvn -q test` (visible tests only — does NOT stage hidden grading tests,
#      so this stays fast and doesn't reveal the grading suite to the learner).
#   3. Counts pass/fail from target/surefire-reports/*.xml.
#   4. POSTs a JSON payload to $EVAL_WEBHOOK_URL if set.
#   5. Always writes .claude/last-progress.json so the learner can inspect it.
#
# This hook NEVER fails the Claude Code session — exit 0 always.
# To disable: delete .claude/settings.json or set CLAUDE_DISABLE_PROGRESS_HOOK=1.

set -u
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$ROOT_DIR"

if [ "${CLAUDE_DISABLE_PROGRESS_HOOK:-0}" = "1" ]; then
  exit 0
fi

{
  BRANCH="$(git branch --show-current 2>/dev/null)"
  [ -z "$BRANCH" ] && BRANCH="unknown"
  START_EPOCH=$(date +%s)

  # Clear previous surefire reports so we count only this run.
  rm -rf target/surefire-reports 2>/dev/null

  mvn -B -q test > .claude/last-mvn.log 2>&1
  MVN_EXIT=$?
  DURATION=$(( $(date +%s) - START_EPOCH ))

  BASE_JSON="$(bash grading/scripts/parse-surefire.sh "$BRANCH" "$MVN_EXIT" 2>/dev/null \
                || echo "{\"branch\":\"$BRANCH\",\"status\":\"unknown\",\"mavenExitCode\":$MVN_EXIT}")"

  # Insert durationSeconds before the closing brace.
  RESULT_JSON="${BASE_JSON%\}},\"durationSeconds\":${DURATION}}"

  echo "$RESULT_JSON" > .claude/last-progress.json
  echo "[claude-progress] $RESULT_JSON" >&2

  if [ -n "${EVAL_WEBHOOK_URL:-}" ]; then
    curl -fsS --max-time 5 -X POST "$EVAL_WEBHOOK_URL" \
      -H 'Content-Type: application/json' \
      -d "$RESULT_JSON" > /dev/null 2>&1 \
      && echo "[claude-progress] reported to webhook" >&2 \
      || echo "[claude-progress] webhook POST failed (non-fatal)" >&2
  else
    echo "[claude-progress] EVAL_WEBHOOK_URL not set — skipping webhook" >&2
  fi
} || true

exit 0
