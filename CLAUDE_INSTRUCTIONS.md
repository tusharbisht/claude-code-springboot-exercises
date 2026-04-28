# Solving Meta 01 with Claude Code — faster and better

This branch is the only one in the course where the **deliverable is the Claude Code configuration**, not the Java. The Java is the vehicle for surfacing what's missing.

## The intended loop

```
   ┌──────────────────────────────────────┐
   │ 1. Implement the feature with Claude │
   └─────────────┬────────────────────────┘
                 │
                 ▼
   ┌──────────────────────────────────────┐
   │ 2. Run ./grading/run-grading.sh      │
   └─────────────┬────────────────────────┘
                 │
        any tests fail?
            yes ▼                 no ▼
┌───────────────────────┐    ┌──────────┐
│ 3. Read the failure   │    │ Done.    │
│    message — it tells │    └──────────┘
│    you exactly what   │
│    CLAUDE.md needs.   │
└───────────┬───────────┘
            │
            ▼
┌───────────────────────────┐
│ 4. Add one bullet to      │
│    CLAUDE.md, re-run      │
│    Claude, restart the    │
│    feature implementation │
└───────────┬───────────────┘
            └──────────► back to step 2
```

Each pass should make CLAUDE.md *less generic* and *more this-codebase-specific*. By the end you have a CLAUDE.md that would prevent all four convention violations.

## Recommended workflow

### 1. First pass: implement WITHOUT writing CLAUDE.md

> "Read EXERCISE.md and TasksByPriorityTest. Implement `GET /api/tasks/by-priority` to make the test pass. Run mvn test."

Don't mention conventions. Don't pre-install guard rails. Let Claude do what Claude does by default. This is the *baseline*.

### 2. Run the grading suite

```bash
./grading/run-grading.sh
```

You'll see something like:

```
HiddenConventionGradingTest.noLombok — FAILED
HiddenConventionGradingTest.noFieldAutowired — FAILED
HiddenConventionGradingTest.controllersDontImportRepositories — FAILED
HiddenConventionGradingTest.claudeMdExists — FAILED
```

Read each failure's `as(...)` message — they're written as instructions for what to add to CLAUDE.md.

### 3. Build CLAUDE.md from those failure messages

Open a new file `CLAUDE.md` and add the conventions that would have prevented each violation. Aim for 5-10 lines, not 50. Each line should be a directive, not a description:

```markdown
# Project context for Claude Code

## Conventions

- **Constructor injection only.** No `@Autowired` on fields or setters.
- **No Lombok.** Boilerplate is intentional. Don't add `lombok.*` imports.
- **No Spring Data Specifications.** Use JPQL `@Query` with nullable parameters for filtering.
- **`@Transactional` lives on service methods.** Never on controllers.
- **Controllers are thin.** They depend on services, never on repositories.
```

### 4. Restart the feature implementation, this time WITH CLAUDE.md

Claude reads CLAUDE.md on every new session. So:
- Exit the existing Claude session (`/exit` or `Ctrl+C`)
- Discard your previous attempt: `git checkout src/`
- Re-launch `claude` and try again with the same prompt

If Claude now stops reaching for `@Autowired` on fields, your CLAUDE.md is doing its job.

### 5. Iterate

If the grading suite still has failures, the corresponding CLAUDE.md bullet is too vague or missing. Tighten it. Re-run.

## What you're learning

Three things, all of which compound across every other Claude Code project you work on:

1. **CLAUDE.md is a load-bearing document.** A weak CLAUDE.md == an LLM coworker who keeps making the same mistakes you've corrected three times.
2. **Conventions worth writing down are the ones Claude *gets wrong by default*.** Generic Spring Boot tutorials use `@Autowired` fields. So Claude reaches for them. Your job is to override that prior.
3. **CLAUDE.md grows with the codebase.** Every time you find yourself correcting Claude on a project-specific habit, that's a candidate CLAUDE.md bullet.

## Anti-patterns

- Don't write a 200-line CLAUDE.md. Claude reads it on every session — long files are expensive context. Keep it short and *imperative*.
- Don't write descriptive prose ("This codebase generally prefers..."). Use directives ("Do this. Don't do that. Why: ...").
- Don't list things Claude already does correctly by default. CLAUDE.md is for *correcting priors*, not restating them.
- Don't copy main's CLAUDE.md before working through the failures. You'll skip the lesson.

## When you're stuck

> "I added 'no @Autowired fields' to CLAUDE.md but the noFieldAutowired test still fails. Read CLAUDE.md, then read the file the test is grepping in. Tell me whether (a) my CLAUDE.md isn't being read at all, or (b) it's being read but Claude is ignoring it, and what to do about either."
