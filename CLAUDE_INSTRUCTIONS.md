# Solving Meta 02 with Claude Code — faster and better

This is the most "meta" exercise in the course: you're using Claude to help you build the *configuration that makes future Claude sessions faster*. The trap is to do it casually — anything you build here will end up in muscle-memory across every Java project you touch, so it's worth doing well.

## Recommended workflow

### 1. Build /find-controller first — and use it on this codebase

> "Read EXERCISE.md and the existing `.claude/commands/find-failures.md`. Use that as a template. Draft `.claude/commands/find-controller.md` that takes an HTTP verb + path and locates the matching controller method. Use the Explore subagent."

Save the file. Then immediately use it:

```
/find-controller GET /api/users/{id}
```

Did it return the right answer? If not, the prompt is too vague. Tighten until it works on three different paths reliably.

**The discipline:** *every slash command you build, you should test on the actual repo before considering it done.*

### 2. Build /test-changed and use it

> "Draft `.claude/commands/test-changed.md` that runs `git diff main --name-only`, identifies test classes corresponding to changed source files, and runs `mvn -Dtest=Class1,Class2 test`. Don't run the full test suite."

Save it. Modify a source file. Run `/test-changed`. Did it run only the relevant tests?

The instinct to build is: **slash commands should be one-line shortcuts for things you do all the time**. If you find yourself manually typing the same `mvn` invocation a third time, that's a slash command.

### 3. Build the pom-guard hook

This is where it gets concrete. Three sub-steps:

```bash
# 1. write the script
cat > .claude/scripts/guard-pom.sh <<'EOF'
#!/usr/bin/env bash
PAYLOAD="$(cat)"
FP="$(echo "$PAYLOAD" | jq -r '.tool_input.file_path // ""')"
case "$FP" in
  *pom.xml)
    echo "guard: refusing to edit pom.xml — review dependency changes carefully first" >&2
    exit 1
    ;;
  *) exit 0 ;;
esac
EOF
chmod +x .claude/scripts/guard-pom.sh

# 2. wire it in .claude/settings.json under PreToolUse
# (read the existing Stop hooks — don't clobber them. ADD a PreToolUse entry.)

# 3. test by asking Claude to do something pom.xml-touching
```

> "Add `<some dependency>` to pom.xml."

Claude attempts the edit. Hook fires. Claude sees the rejection and reports it. You confirm the system worked end-to-end.

### 4. Verify

```bash
mvn -B test                                # all 4 visible artifact tests green
./grading/run-grading.sh                   # hidden quality checks
```

## The deeper lesson

Claude Code is designed to be **personalized per project**. Most users never personalize past the defaults. The two-cent improvement here, every day, is the difference between someone who *uses* Claude Code and someone who is *fast with* Claude Code.

A useful instinct to build:

> "If I'm typing the same prompt for the third time, I'm doing it wrong. It belongs in a slash command."
> "If I want Claude to *always* X before Y, I'm doing it wrong. It belongs in a hook."

After this exercise, you should never again hand-type `mvn -Dtest=ClassName` because you have a command that does it. You should never again forget to run `mvn test` before committing because you have a hook that does it.

## What NOT to do

- Don't write a slash command that's just `mvn test`. The shell is already that.
- Don't write hooks that fire on EVERY tool call doing log-the-world. They slow the session and the noise drowns out the useful events.
- Don't write a guard hook that just prints a warning and exits 0. If you're not actually blocking, you're not guarding.
- Don't put project-specific settings in `~/.claude/settings.json` (your global config). Use `.claude/settings.json` (project-local), checked into the repo so the whole team gets it.

## When you're stuck

> "My guard-pom.sh runs but doesn't seem to block — Claude still successfully edits pom.xml. Read the script, check what exit code it returns when file_path is pom.xml, and walk me through what Claude Code does when a PreToolUse hook exits non-zero vs. zero."

## After this exercise

Look at `.claude/commands/` and `.claude/settings.json` after you're done. This is now part of your tool. **Copy these patterns to your real work codebases.** That's where the dividends actually compound.
