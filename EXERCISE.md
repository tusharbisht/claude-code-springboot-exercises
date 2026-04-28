# Meta 01 — The exercise IS building CLAUDE.md

**Type:** meta-skill (the deliverable is `CLAUDE.md` itself)
**Estimated time:** 60–90 min with Claude Code

## What's missing

Two things:

1. A `CLAUDE.md` at the repo root. The branch deliberately deletes it. Without it, Claude has to rediscover this codebase's conventions on every session — and it usually rediscovers them as the *Spring Boot defaults*, not as what this codebase actually does.
2. The implementation of `GET /api/tasks/by-priority`. The endpoint stub exists and throws 501. Tests want it to return a `Map<TaskPriority, List<TaskDto>>` with all three priority keys present (even if empty) and tasks ordered by id within each.

## Reproduce

```bash
git checkout meta/01-build-claude-md
ls CLAUDE.md           # → no such file
mvn test               # → 1 failing test (TasksByPriorityTest)
```

That's all you see. The deeper structure of this exercise only emerges once you push and run `./grading/run-grading.sh`.

## What "done" looks like

Two things must be true at the same time:

1. The visible test passes — `tasksByPriority` returns the right grouping.
2. The hidden grading suite (`grading/meta-01-build-claude-md/`) passes. It checks for **convention violations** Claude is likely to introduce in the absence of a project-specific CLAUDE.md:
   - `@Autowired` on fields (this codebase uses constructor injection only)
   - Any Lombok import or annotation (this codebase intentionally has no Lombok)
   - Spring Data `Specification<T>` or `JpaSpecificationExecutor` (this codebase uses JPQL `@Query`)
   - `@Transactional` on a controller class or method (must live on services)
   - Controllers depending on repositories directly (must go through services)
3. **`CLAUDE.md` exists at the repo root and mentions all four conventions above.** This is checked separately.

When *all* of those pass, you've built a CLAUDE.md that would have prevented the violations. That's the deliverable.

## How to actually do this

The intended workflow is **iterative**:

1. Try to implement the feature with no CLAUDE.md. Let Claude do whatever it would do by default.
2. Run `./grading/run-grading.sh`. See which conventions Claude violated.
3. For each failing convention test, add a one-line bullet to CLAUDE.md telling Claude not to do that thing.
4. Re-run grading. If new violations appear, add more lines. If old ones stay, your CLAUDE.md isn't being read — make sure you launched Claude from the repo root.

The point isn't to memorize four bullet points. The point is to feel — concretely, on this codebase — that **CLAUDE.md is a load-bearing document**, and to internalize that *every codebase you work on will need its own*.

## A starting CLAUDE.md you should NOT use

Don't just look at how `main` did it on the other branches and copy. The fastest way to actually learn is to:

- Skip looking at `main`'s CLAUDE.md
- Implement the feature, observe what Claude does, write the convention bullet that would have prevented each thing
- Compare your CLAUDE.md to `main`'s only at the end, as a check

This is a skill that compounds. Most engineers who use Claude Code never write a real CLAUDE.md. The ones who do see a quality jump roughly equivalent to switching from a junior developer to a senior one.

## What the hidden grading suite checks (not spoilers — they're listed above)

7 tests. The first asserts CLAUDE.md exists. The next five assert specific patterns are absent from the source code. The last asserts CLAUDE.md mentions all four conventions in some recognizable form.

## See also

[`CLAUDE_INSTRUCTIONS.md`](CLAUDE_INSTRUCTIONS.md) — recommended workflow.
