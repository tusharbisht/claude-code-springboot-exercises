#!/usr/bin/env bash
# Run the hidden grading suite for the current (or specified) exercise branch.
#
# Usage:
#   ./grading/run-grading.sh                       # auto-detect current branch
#   ./grading/run-grading.sh exercise/01-fix-validation-bug
#
# Branch -> grading dir is derived automatically: an `exercise/<slug>` branch
# loads tests from `grading/exercise-<slug>/`. Drop a new directory in and the
# runner picks it up — no edit here needed.
#
# Exits 0 if all grading tests pass, non-zero otherwise.
# Always emits grading-result.json in the project root.

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

BRANCH="${1:-$(git branch --show-current 2>/dev/null || echo unknown)}"
[ -z "$BRANCH" ] && BRANCH="unknown"

if [ "$BRANCH" = "main" ]; then
  echo "Branch is 'main' — nothing to grade. Switch to an exercise/* branch."
  exit 0
fi

# Map branch -> directory by replacing '/' with '-'.
#   exercise/05-foo  -> exercise-05-foo
#   meta/01-bar      -> meta-01-bar
EXERCISE_DIR="${BRANCH//\//-}"

if [ ! -d "$ROOT_DIR/grading/$EXERCISE_DIR" ]; then
  echo "ERROR: no grading directory for branch '$BRANCH' (looked for grading/$EXERCISE_DIR)" >&2
  echo "Available exercises:" >&2
  for d in "$ROOT_DIR"/grading/*/; do
    name="$(basename "$d")"
    [ "$name" = "scripts" ] && continue
    [ -d "$d" ] && echo "  - $name" >&2
  done
  exit 2
fi

echo "→ grading branch: $BRANCH"
echo "→ exercise dir:   grading/$EXERCISE_DIR"

bash "$ROOT_DIR/grading/scripts/stage-tests.sh" "$EXERCISE_DIR"

# Make sure we always unstage, even if mvn fails.
trap 'bash "$ROOT_DIR/grading/scripts/unstage-tests.sh" || true' EXIT

# Clear stale surefire reports so we count only this run.
rm -rf "$ROOT_DIR/target/surefire-reports" 2>/dev/null

set +e
mvn -B -q test
MVN_EXIT=$?
set -e

bash "$ROOT_DIR/grading/scripts/parse-surefire.sh" "$BRANCH" "$MVN_EXIT" > grading-result.json

echo "→ result written to grading-result.json"
cat grading-result.json
exit $MVN_EXIT
