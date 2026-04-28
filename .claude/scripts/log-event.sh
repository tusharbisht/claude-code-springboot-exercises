#!/usr/bin/env bash
# Append a single Claude Code hook event to .claude/session-log.jsonl.
#
# Usage:
#   bash log-event.sh prompt   # for UserPromptSubmit hooks
#   bash log-event.sh edit     # for PostToolUse Edit/Write/MultiEdit
#   bash log-event.sh bash     # for PostToolUse Bash
#
# Reads the hook's JSON payload from stdin.
# Designed to be very fast and never block the session.
#
# To disable: set CLAUDE_DISABLE_SESSION_LOG=1 in your shell.

set -u
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
LOG="$ROOT_DIR/.claude/session-log.jsonl"

if [ "${CLAUDE_DISABLE_SESSION_LOG:-0}" = "1" ]; then
  exit 0
fi

EVENT="${1:-unknown}"
mkdir -p "$(dirname "$LOG")"

# Cap stdin at 64k to keep this fast and bounded.
PAYLOAD="$(head -c 65536)"
TIMESTAMP="$(date -u +%Y-%m-%dT%H:%M:%SZ)"
BRANCH="$(git -C "$ROOT_DIR" branch --show-current 2>/dev/null || echo unknown)"

if command -v jq >/dev/null 2>&1; then
  echo "$PAYLOAD" | jq -c \
      --arg ts "$TIMESTAMP" \
      --arg event "$EVENT" \
      --arg branch "$BRANCH" \
      '{ts: $ts, event: $event, branch: $branch, payload: .}' \
      >> "$LOG" 2>/dev/null || echo "{\"ts\":\"$TIMESTAMP\",\"event\":\"$EVENT\",\"branch\":\"$BRANCH\",\"raw\":\"jq-failed\"}" >> "$LOG"
else
  # No jq — store the payload as a single escaped JSON string.
  ESCAPED="$(python3 -c 'import json,sys; print(json.dumps(sys.stdin.read()))' <<<"$PAYLOAD" 2>/dev/null || echo '"<unparsed>"')"
  echo "{\"ts\":\"$TIMESTAMP\",\"event\":\"$EVENT\",\"branch\":\"$BRANCH\",\"raw\":$ESCAPED}" >> "$LOG"
fi

exit 0
