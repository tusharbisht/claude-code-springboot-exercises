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

mkdir -p "$DEST"
cp "$SRC"/*.java "$DEST/"
echo "→ staged grading tests from $SRC into $DEST"
