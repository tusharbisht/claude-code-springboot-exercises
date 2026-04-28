# When to skip Claude Code — and what it's actually for

This document is the inverse of the others. The other branches walk through how to *use* Claude Code well. This branch is about when *not* to.

## The trap

Claude Code is fast and reliable enough that it becomes the default. "Whatever I'm about to do, let me prompt Claude to do it instead." The cost is invisible per-instance — a few seconds of latency, a few tokens — and it adds up to a meaningful fraction of your day if you're not careful.

For trivial work, the prompt overhead exceeds the work. That's the whole exercise.

## When Claude is the wrong tool

| Situation | Why skip Claude |
| --- | --- |
| Single-character / single-word fix | typing the prompt takes longer than the fix |
| You already know the file and the line | nothing to discover |
| You know the answer but want to "double check" | this is what tests are for |
| Routine tooling commands (`git status`, `mvn test`, `ls`) | use the shell |
| Format/lint/whitespace cleanup | your editor does this for free |
| Reading a familiar file end-to-end | open it; Claude can't read faster than you can scan |

## When Claude is the right tool

| Situation | Why Claude wins |
| --- | --- |
| You don't know which file to open | search + read in one delegation |
| The change spans 3+ files | mechanical edits at scale |
| The diagnosis isn't obvious | hypothesis + confirmation loop |
| You'd otherwise be reading a wall of logs / SQL | summarization is Claude's strongest move |
| You'd write the same prompt three times this week | save it as a slash command |
| Migration / framework upgrade | release notes + scoped edits |
| Generating tests for an existing class | scaffolding velocity is the win |
| Refactoring across many files | step-by-step with tests as guardrails |

## A tiny piece of advice that compounds

Before you launch Claude, try the question: **"Could I tell someone what the change is in fewer words than I'd need to prompt Claude?"** If yes, you can probably also *make* the change in less time than you'd spend prompting.

If no — that's a signal the task is rich enough to delegate.

## After this exercise

If you've worked through exercises 01–07, you already know what Claude is great at. Now you also know what it isn't. Use both.

The goal isn't "use Claude maximally." The goal is **shortest path to correct, reviewable code**. Some days that's 30 prompts. Some days it's a one-character edit you make in five seconds.
