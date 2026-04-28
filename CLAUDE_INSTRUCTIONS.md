# Solving Meta 06 with Claude Code — faster and better

This exercise is the most directly transferable to your day job. Master these four tactics and you'll ship features 30–50% faster, with fewer bugs, on every codebase.

## Stage 1 — Spec-first

**Goal:** produce `SPEC.md` *before* writing any code.

> "Read FEATURE_REQUEST.md. Don't write code yet. Draft a SPEC.md that answers each of the open questions explicitly. For each question, give me at least two options, the trade-offs, and your recommendation. The spec sections I want:
>   - Overview (what we're building, why)
>   - Endpoints (paths, verbs, request/response shapes)
>   - Data model (entity, columns, indexes)
>   - Constraints / non-goals
>   - Open questions (resolved with a recommendation each)"

Read the spec carefully. Common smells:
- "We could go either way" — bad. The whole point of the spec is to pick.
- Generic recommendations without trade-off analysis — bad. "Use snapshot because it's simpler" is too thin.
- Missing endpoints — bad. If the FEATURE_REQUEST.md mentions four endpoints, the spec needs four.

Push back until the spec is *defensible* — you could hand it to another engineer and they could implement.

**Commit it.** This is the spec-first deliverable.

## Stage 2 — Test-first

**Goal:** translate the spec into a failing test, *before* implementing.

> "Based on SPEC.md, extend `TaskHistoryFeatureTest` to pin every behaviour the spec promises. Don't implement the feature in src/main yet — just write the assertions and let them fail."

```bash
mvn -B -Dtest=TaskHistoryFeatureTest test    # should fail with "history endpoint not found"
```

Commit the test. Now your spec has a mechanical check — when the test passes, you've matched the spec.

## Stage 3 — Adversarial

**Goal:** before implementing, get Claude to find issues with its own plan.

> "Plan the implementation. Then BEFORE writing code, find three things wrong with your plan. Argue against your own approach. Consider:
>   - What happens at the boundary case I didn't think of?
>   - Is there a simpler approach that the visible tests would also pass?
>   - Does this plan introduce any pattern that contradicts CLAUDE.md?"

A good Claude response will surface things you didn't think of. Examples on this feature:
- "What if a task has zero updates — does its history endpoint return [], 404, or just the create entry?"
- "If we store snapshots, we duplicate the task data ~10x for an active task. Is that OK?"
- "If we store diffs, fetching the current state means replaying — but the source-of-truth is still the Task table. Why both?"

**Update the spec or revise the plan based on what comes out.**

## Stage 4 — Constraint-shaped

**Goal:** after the feature works, re-prompt under hard constraints.

> "All tests pass. Now I want to constrain the design:
>   - At most ONE new entity (no separate "field change" join tables)
>   - No new dependencies in pom.xml
>   - No changes to existing TaskService public method signatures
>   - The history-write logic must be ≤ 30 lines total
> Show me the diff to make this work, or tell me which constraint is impossible to honour and why."

This is the highest-leverage of the four tactics. The constraint-shaped re-prompt **almost always produces a smaller, sharper implementation** than the first pass. Engineers who do this routinely ship 20–30% less code per feature.

## After all four stages

```bash
mvn -B test                            # all green (existing 19 + new tests)
./grading/run-grading.sh               # hidden suite passes
cat SOLUTION_NOTES.md                  # auto-generated; should show the prompts you used
```

The session log will (ideally) show prompts containing words like "wrong with", "argue against", "at most", "no new" — the markers of the tactics. The hidden grading suite includes a (non-failing) check for these — it'll print what it found.

## Why this works

The four tactics impose **friction at the right places**:

| Tactic | What it prevents |
| --- | --- |
| Spec-first | implementation drift, scope creep, "wait what were we building?" |
| Test-first | over-build, gold-plating, features that don't actually verify |
| Adversarial | accepting Claude's first plan when the second would be better |
| Constraint-shaped | bloat, premature abstraction, "while we're here..." additions |

Most LLM-coding workflows skip all four. Time saved up-front, multiplied by larger PRs and slower review.

## What NOT to do

- Don't merge the four stages into one prompt ("write a spec, write tests, find issues, then implement under constraints"). The friction comes from doing them in sequence, with you reviewing each output.
- Don't skip stage 1 because "the spec is obvious." Specs are for the cases where you discover, during writing, that the spec ISN'T obvious.
- Don't accept "I can't think of three issues" from Claude in stage 3. That's fine the first time; push back twice. There are always issues with a non-trivial plan.
- Don't try every constraint at once in stage 4. Start with the strictest one.

## When you're stuck

> "I have all visible tests passing but my SPEC.md says deletion returns 404 and my code returns 200 with history. Reconcile this — should the spec change or the code? Don't pick the easier one; pick the right one."

## After this exercise

The four tactics compose. **Use them on every non-trivial feature you build, in this codebase or your real one.** The gain is reliable, repeatable, and not contingent on Claude being any smarter than today.
