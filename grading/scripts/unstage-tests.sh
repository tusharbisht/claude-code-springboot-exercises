#!/usr/bin/env bash
# Removes the staged grading directory so the working tree is clean.
set -euo pipefail
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
DEST="$ROOT_DIR/src/test/java/com/learning/taskmanager/grading"
if [ -d "$DEST" ]; then
  rm -rf "$DEST"
  echo "→ unstaged grading tests"
fi
