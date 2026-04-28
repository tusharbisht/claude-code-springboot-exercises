# Meta 02 — Streamline your workflow with hooks and slash commands

**Type:** meta-skill (the deliverable is your Claude Code configuration)
**Estimated time:** 60–90 min with Claude Code

## What's missing

This branch ships no failing Java tests. What's "wrong" is your workflow:

- Every time you want to find which controller handles `GET /api/users/{id}`, you grep around manually.
- Every time you want to run only the tests touching files you've changed, you copy-paste class names.
- When Claude is about to edit `pom.xml`, you'd like it to pause and confirm — dependency changes deserve attention.

These are exactly the kinds of small frictions that **slash commands and hooks were designed to remove**. The deliverable for this exercise is the configuration files that remove them.

## Reproduce

```bash
git checkout meta/02-hooks-and-commands
mvn test                  # 4 failures in ClaudeCodeArtifactsTest
```

Each failure points at a specific artifact you need to create.

## What "done" looks like

Four artifacts in this branch's working tree:

1. **`.claude/commands/find-controller.md`** — a slash command that takes a URL path or HTTP verb+path and finds the controller method handling it. Use the Explore subagent (this is exactly what it's for).
2. **`.claude/commands/test-changed.md`** — a slash command that detects files changed since `main` and runs the *targeted* tests (`mvn -Dtest=ClassName test`), not the full suite.
3. **`.claude/scripts/guard-pom.sh`** — a shell script that reads a Claude Code hook payload from stdin (JSON) and prints a warning to stderr if the edit target is `pom.xml`. Exiting non-zero blocks the edit.
4. **`.claude/settings.json` updated** — wire `guard-pom.sh` as a `PreToolUse` hook with matcher `Edit|Write|MultiEdit`. (Don't disturb the existing `Stop` hooks for progress reporting.)

When all 4 visible artifact tests pass, push. The hidden grading suite checks the artifacts are *good*, not just present (e.g., `/test-changed` actually targets tests, not just runs the full suite).

## What you're learning

| Skill | Why it matters |
| --- | --- |
| Slash commands as **prompts you've already written** | the third time you write a prompt, save it. Saves time, prevents drift. |
| Hooks as **reflexes the harness has, not you** | hooks fire automatically on tool events — they're how you encode "always do X before/after Y" without remembering. |
| Subagent delegation in commands | `/find-controller` should delegate the search to the Explore agent; that's where it shines. |
| Guarding sensitive files with PreToolUse | `pom.xml`, secrets, migration files — hooks are how you say "stop and confirm" without hand-vigilance. |

## What `/find-controller` should look like (sketch only)

```markdown
---
description: Find the controller method that handles a given URL path
argument-hint: <HTTP verb> <path>
---

You have been given the request:  $ARGUMENTS

Use the Explore subagent to find:
1. Which @RestController class contains a method matching this verb+path
2. The full method signature
3. The path-variable bindings

Format the answer as:  <file>:<line>  <method signature>
Do not edit any files.
```

Don't copy this verbatim — it's not enough. Tighten it as you build.

## What the hook script should do

Receive Claude's hook payload on stdin. The relevant field is `tool_input.file_path`. If it's `pom.xml` (relative or absolute path), print a warning to stderr and exit 1 (blocks). Otherwise exit 0 (allows).

```bash
# rough shape
PAYLOAD="$(cat)"
FP="$(echo "$PAYLOAD" | jq -r '.tool_input.file_path // ""')"
case "$FP" in
  *pom.xml) echo "guard: refusing to edit pom.xml without confirmation" >&2; exit 1 ;;
  *) exit 0 ;;
esac
```

## See also

[`CLAUDE_INSTRUCTIONS.md`](CLAUDE_INSTRUCTIONS.md) — the workflow.
