# Meta 04 — Build an open-ended feature graded by an LLM walkthrough

**Type:** open-ended feature, LLM-judge graded
**Estimated time:** 90–180 min with Claude Code

## What's missing

A real feature request: **task labels**. See [`FEATURE_REQUEST.md`](FEATURE_REQUEST.md) for the spec — it deliberately leaves design decisions to you.

## Reproduce

```bash
git checkout meta/04-open-ended-feature
mvn test            # all 19 existing tests pass — there's no failing test for this exercise
cat FEATURE_REQUEST.md
```

There is **no failing test** because the feature is open-ended. Your output is graded by an LLM judge that walks through your API as a user would and scores against [`grading/meta-04-open-ended-feature/rubric.md`](grading/meta-04-open-ended-feature/rubric.md).

## What "done" looks like

- Existing 19 tests still pass
- The feature implements the must-haves in `FEATURE_REQUEST.md`
- The judge gives you ≥ 36/60 on the rubric (60% threshold; passing-grade)
- You'd be willing to put your name on the PR

To run the judge locally:

```bash
export ANTHROPIC_API_KEY=sk-...
./grading/meta-04-open-ended-feature/run-judge.sh
```

This boots the app, has Claude walk through your API as a strict user, then writes `grading-result.json` with scores and a written critique.

## Why this exercise exists

Every exercise before this one had a test that pinned the answer. That's the easy case — Claude can iterate against a clear oracle. **Real product work doesn't have that.** A feature request lands, you design the shape, and the only feedback loop is "does the user think this works?"

This exercise simulates that. The LLM judge plays the user. It will:

- Walk through happy paths (create label, attach, list)
- Probe edge cases (duplicate names, missing IDs, malformed colors)
- Test REST hygiene (right verbs, right status codes)
- Check error responses are useful
- Compare your work against the codebase's existing conventions

It's strict, it cites evidence, and it can't be gamed by passing pre-built tests because there are no pre-built tests.

## What you can do that the judge can't

- Read existing code (the judge only sees your API surface)
- Catch issues before they go live (the judge tests an already-running app)
- Push back on the spec (the judge takes the spec as given)

## See also

- [`FEATURE_REQUEST.md`](FEATURE_REQUEST.md) — what the product team asked for
- [`grading/meta-04-open-ended-feature/rubric.md`](grading/meta-04-open-ended-feature/rubric.md) — what the judge scores against (it's not a secret; shape your work to it)
- [`CLAUDE_INSTRUCTIONS.md`](CLAUDE_INSTRUCTIONS.md) — recommended workflow
