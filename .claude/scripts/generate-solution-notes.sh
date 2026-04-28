#!/usr/bin/env bash
# Build SOLUTION_NOTES.md from .claude/session-log.jsonl.
# Also print a one-line in-session summary to stderr (visible to the learner).
#
# Runs as part of the Stop hook. Idempotent — overwrites SOLUTION_NOTES.md
# from scratch each time.
#
# To disable: set CLAUDE_DISABLE_SESSION_LOG=1.

set -u
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
LOG="$ROOT_DIR/.claude/session-log.jsonl"
OUT="$ROOT_DIR/SOLUTION_NOTES.md"

if [ "${CLAUDE_DISABLE_SESSION_LOG:-0}" = "1" ]; then
  exit 0
fi

if [ ! -f "$LOG" ]; then
  exit 0
fi

if ! command -v jq >/dev/null 2>&1; then
  echo "[session] jq not installed — skipping SOLUTION_NOTES.md generation" >&2
  exit 0
fi

BRANCH="$(git -C "$ROOT_DIR" branch --show-current 2>/dev/null || echo unknown)"
TIMESTAMP="$(date -u +%Y-%m-%dT%H:%M:%SZ)"

# Filter the log to entries for the current branch (cumulative across sessions on this branch).
FILTERED="$(jq -c --arg b "$BRANCH" 'select(.branch == $b)' "$LOG" 2>/dev/null)"

count_event() {
  printf '%s\n' "$FILTERED" | jq -r --arg e "$1" 'select(.event==$e) | .ts' 2>/dev/null | grep -c . || true
}
count_grep() {
  printf '%s\n' "$BASH_COMMANDS" | grep -cE "$1" || true
}

PROMPT_COUNT=$(count_event prompt)
EDIT_COUNT=$(count_event edit)
EDITED_FILES=$(printf '%s\n' "$FILTERED" | jq -r 'select(.event=="edit") | .payload.tool_input.file_path // .payload.tool_input.path // empty' 2>/dev/null | sort -u | grep -v '^$' || true)
EDITED_FILE_COUNT=$(printf '%s\n' "$EDITED_FILES" | grep -c . || true)
BASH_COMMANDS=$(printf '%s\n' "$FILTERED" | jq -r 'select(.event=="bash") | .payload.tool_input.command // empty' 2>/dev/null || true)
TEST_RUNS=$(count_grep 'mvn\b.*\btest\b')
GREP_RUNS=$(count_grep '\bgrep\b|\brg\b')
GIT_DIFF_RUNS=$(count_grep 'git\s+diff')

# Force numeric (grep -c can return blank on empty stream).
PROMPT_COUNT=${PROMPT_COUNT:-0}
EDIT_COUNT=${EDIT_COUNT:-0}
EDITED_FILE_COUNT=${EDITED_FILE_COUNT:-0}
TEST_RUNS=${TEST_RUNS:-0}
GREP_RUNS=${GREP_RUNS:-0}
GIT_DIFF_RUNS=${GIT_DIFF_RUNS:-0}

PROMPTS_LIST="$(printf '%s\n' "$FILTERED" \
  | jq -r 'select(.event=="prompt") | "- \(.ts) — \((.payload.prompt // "")[0:240])"' 2>/dev/null \
  | sed 's/$//')"

EDITS_LIST="$(printf '%s\n' "$EDITED_FILES" | sed 's|^|- |')"

cat > "$OUT" <<EOF
# Solution notes (auto-generated)

Branch: \`$BRANCH\`
Last updated: $TIMESTAMP

> This file is built automatically by a Stop hook from your Claude Code session
> log. It captures what you actually did — prompts, file edits, commands. You
> can edit it (e.g. add reflections), but the next Claude session will overwrite
> it. To stop generation entirely, set \`CLAUDE_DISABLE_SESSION_LOG=1\`.

## At a glance

| Metric | Value |
| --- | --- |
| Prompts you sent | $PROMPT_COUNT |
| Files Claude edited | $EDITED_FILE_COUNT (across $EDIT_COUNT edit operations) |
| \`mvn test\` invocations | $TEST_RUNS |
| Search-style commands (grep/rg) | $GREP_RUNS |
| \`git diff\` invocations | $GIT_DIFF_RUNS |

## Files Claude edited

$EDITS_LIST

## Your prompts (in order)

$PROMPTS_LIST

---

## Reflection (write your own — Claude can't fake this well)

- One thing in Claude's first plan you rejected, and why:
- One assertion in the failing test that wasn't obvious until you read it:
- One thing you'd do differently next time:
EOF

# In-session feedback to the user.
echo "[session] $PROMPT_COUNT prompts • $EDITED_FILE_COUNT files edited • $TEST_RUNS mvn test runs → SOLUTION_NOTES.md updated" >&2

exit 0
