#!/usr/bin/env bash
# Copies grading test files for a given exercise into src/test/java/.../grading/
# so Maven picks them up alongside the visible tests.

set -euo pipefail

EXERCISE_DIR="$1"
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
SRC="$ROOT_DIR/grading/$EXERCISE_DIR"
DEST="$ROOT_DIR/src/test/java/com/learning/taskmanager/grading"

if [ ! -d "$SRC" ]; then
  echo "ERROR: grading directory not found: $SRC" >&2
  exit 1
fi

shopt -s nullglob
JAVA_FILES=("$SRC"/*.java)
if [ ${#JAVA_FILES[@]} -eq 0 ]; then
  echo "→ no hidden grading tests for $SRC (visible tests are the source of truth)"
  exit 0
fi
mkdir -p "$DEST"
cp "${JAVA_FILES[@]}" "$DEST/"
echo "→ staged ${#JAVA_FILES[@]} grading test file(s) from $SRC into $DEST"
