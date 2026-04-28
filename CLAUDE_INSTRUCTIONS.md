# Solving Meta 03 with Claude Code — faster and better

This exercise is about **encoding institutional knowledge** so it survives across sessions. The integration task is trivial. The lesson is the agent.

## Recommended workflow

### 1. Read the legacy module without an agent first

> "Read every file in `src/main/java/com/learning/taskmanager/legacy/` and tell me three things that are different from the rest of the codebase. Don't write any code yet."

The point is to *feel* the friction. You'll watch Claude burn context on a small package, retroactively understand why this matters, and have something concrete to encode in the agent.

### 2. Draft the agent file

> "Based on what you just read, draft `.claude/agents/legacy-navigator.md`. Include YAML frontmatter with `name` and `description`. The body should warn future Claude sessions about: (a) the canonical write path is `LegacyAuditDao.recordEvent`, (b) the kind is upper-case, (c) don't construct `LegacyAuditEntry` directly, (d) don't migrate this to JpaRepository. Keep it under 30 lines."

Read the draft critically. Common smells:
- Vague language ("be careful with the legacy module") — replace with concrete directives ("never call `em.persist(LegacyAuditEntry)` directly; use `LegacyAuditDao.recordEvent(...)`").
- Missing the *why* — agents are more durable when they say *why* a convention exists. ("...because the DAO rounds timestamps to second precision so replicas dedupe.")
- Listing the obvious — "this code is in Java" is not useful. Drop generics; keep specifics.

### 3. Test the agent before using it for real

```
@legacy-navigator how do I record a "USER_BANNED" event for user 42?
```

Did the agent answer with `recordEvent("USER_BANNED", 42L, ...)` and warn against direct entity construction? If yes, ship. If it gave a generic "look at the code" answer, the agent body is too vague.

### 4. Use the agent for the actual integration

> "Use the legacy-navigator subagent to plan the integration: when a task is deleted via TaskService, write a TASK_DELETED audit entry through the legacy module. The plan should be 3 bullets and reference the conventions the agent flagged."

Then implement based on that plan. The total integration is ~5 lines added to `TaskService.deleteTask`:

```java
// inside deleteTask, after the actual delete:
auditDao.recordEvent("TASK_DELETED", task.getId(), "system");
```

(Plus a constructor parameter to inject `LegacyAuditDao`.)

### 5. Verify

```bash
mvn -B test                                      # all visible tests pass
./grading/run-grading.sh                         # hidden integration quality
```

If the hidden test `usesLegacyAuditDao` fails (millis isn't second-aligned), it means the integration bypassed the DAO and constructed the entity directly. The agent should have flagged that. Update the agent body to be more emphatic, then redo the integration.

## Why subagents earn their keep

| Without a custom agent | With one |
| --- | --- |
| Every Claude session re-discovers legacy/ | The agent encapsulates "what to look for" once |
| Main session bloats with legacy-spelunking | Agent runs in its own context — main stays clean |
| New team members get the wrong answer first | The agent is the authoritative reference |
| Conventions drift as people forget them | The agent is checked into git — institutional memory |

This is the highest-ROI feature of Claude Code that almost nobody uses.

## What NOT to do

- Don't make the agent's body long. 20-40 lines is the sweet spot. Anything longer means the agent is doing too much.
- Don't write the agent as descriptive prose ("This module was written in 2017..."). Write it as directives Claude will follow.
- Don't skip the test-the-agent-with-a-fake-question step. An agent you haven't validated is worse than no agent — you'll trust it next time.
- Don't spread the legacy conventions across multiple agents. One legacy module → one agent.

## When you're stuck

> "I built `legacy-navigator` and the integration test passes, but the hidden test `usesLegacyAuditDao` fails. The agent body says 'use recordEvent', but I'm using a `JpaRepository` I added. Re-read the agent body — should it have been more emphatic? Update it AND fix the integration."

## After this exercise

Look at `.claude/agents/legacy-navigator.md`. This is what a piece of institutional knowledge looks like as Claude Code config. **Carry the pattern back to your real codebase**: every "here be dragons" zone deserves an agent.
