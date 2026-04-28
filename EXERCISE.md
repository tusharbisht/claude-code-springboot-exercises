# Exercise 08 — When NOT to use Claude Code

**Type:** anti-exercise (calibration)
**Estimated time:** 60 seconds. No, really.

## What's wrong

Both POST endpoints (`POST /api/users` and `POST /api/tasks`) return HTTP `200 OK` instead of `201 Created`. The fix is one word in two files. The build is broken because 10 visible tests assert `status().isCreated()` and now see 200.

## Reproduce

```bash
git checkout exercise/08-when-not-to-use-claude
mvn test
# Tests run: 19, Failures: 10
```

## The exercise

This branch is not really about fixing the bug. It's about **calibrating when Claude Code is the wrong tool.**

Before you do anything else, do this:
1. Note the time.
2. Open the two failing tests' assertion messages.
3. Open the two controllers.
4. Fix the bug yourself, manually, with your editor.
5. Run `mvn test`.
6. Note the time again.

Then *separately*, in a fresh shell session, do this:
1. Note the time.
2. Launch Claude Code.
3. Compose a prompt that gets Claude to find and fix the bug.
4. Wait for Claude to read files, propose a plan, ask for confirmation, edit the files, run tests, and report.
5. Note the time again.

**For most people, the manual fix is 4–10x faster.**

## What "done" looks like

- `mvn test` is green
- You wrote down two numbers (your manual time, your Claude-mediated time)
- You can articulate, in one sentence, the rule of thumb you'll use to decide between them next time

## Why this exercise exists

Claude Code is excellent at the workflows trained by exercises 01–07: diagnosis from a vague symptom, multi-layer feature work, query optimization, refactoring, test scaffolding, framework migrations. It is *not* excellent at "change OK to CREATED in two files." On those tasks, the prompt overhead — describing the goal, reading the plan, approving the change, waiting for verification — is itself longer than the change.

The cost of using Claude on a task it's good at is small. The cost of using Claude on a task you could have done in 10 seconds is *every time you do it*. Building the instinct to skip Claude for trivial work is what separates someone who *uses* Claude Code from someone who's *fast with* Claude Code.

## A rule of thumb (yours, not mine)

By the end of this exercise, write your own version of: *"If the change is < N lines AND I can name the file before opening it AND no one else needs to review the diff, I do it manually."*

Common choices for N: 5, 10, 25. There's no right answer. There's a wrong answer, which is "always launch Claude."

## See also

[`CLAUDE_INSTRUCTIONS.md`](CLAUDE_INSTRUCTIONS.md) — what TO do with Claude after this exercise.
