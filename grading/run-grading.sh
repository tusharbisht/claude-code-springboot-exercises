#!/usr/bin/env bash
# Run the hidden grading suite for the current (or specified) exercise branch.
#
# Usage:
#   ./grading/run-grading.sh                       # auto-detect current branch
#   ./grading/run-grading.sh exercise/01-fix-validation-bug
#
# Exits 0 if all grading tests pass, non-zero otherwise.
# Always emits grading-result.json in the project root.

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

BRANCH="${1:-$(git rev-parse --abbrev-ref HEAD 2>/dev/null || echo unknown)}"

case "$BRANCH" in
  exercise/01-fix-validation-bug|01-fix-validation-bug)
    EXERCISE_DIR="exercise-01-fix-validation-bug"
    ;;
  exercise/02-implement-search|02-implement-search)
    EXERCISE_DIR="exercise-02-implement-search"
    ;;
  exercise/03-optimize-n-plus-one|03-optimize-n-plus-one)
    EXERCISE_DIR="exercise-03-optimize-n-plus-one"
    ;;
  exercise/04-refactor-fat-controller|04-refactor-fat-controller)
    EXERCISE_DIR="exercise-04-refactor-fat-controller"
    ;;
  main)
    echo "Branch is 'main' — nothing to grade. Switch to an exercise/* branch."
    exit 0
    ;;
  *)
    echo "ERROR: branch '$BRANCH' is not a recognized exercise branch."
    exit 2
    ;;
esac

echo "→ grading branch: $BRANCH"
echo "→ exercise dir:   grading/$EXERCISE_DIR"

bash "$ROOT_DIR/grading/scripts/stage-tests.sh" "$EXERCISE_DIR"

# Make sure we always unstage, even if mvn fails.
trap 'bash "$ROOT_DIR/grading/scripts/unstage-tests.sh" || true' EXIT

set +e
mvn -B -q test
MVN_EXIT=$?
set -e

bash "$ROOT_DIR/grading/scripts/parse-surefire.sh" "$BRANCH" "$MVN_EXIT" > grading-result.json

echo "→ result written to grading-result.json"
cat grading-result.json
exit $MVN_EXIT
