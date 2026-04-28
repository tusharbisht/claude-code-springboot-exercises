# Meta 03 — Build a custom subagent for the legacy module

**Type:** meta-skill (the deliverable is a custom Claude Code subagent)
**Estimated time:** 60–90 min with Claude Code

## What's missing

This branch has a `src/main/java/com/learning/taskmanager/legacy/` package — three classes following pre-2018 conventions that diverge from the rest of the codebase:

- `LegacyAuditEntry` — JPA entity with snake_case columns, `Long occurredAtMillis` instead of `Instant`, getter names that don't match field names.
- `LegacyAuditDao` — handwritten interface (no `extends JpaRepository`) with a contract: callers must use `recordEvent(...)`, never construct `LegacyAuditEntry` directly.
- `LegacyAuditDaoImpl` — `@PersistenceContext EntityManager`, field injection, transactional methods that round timestamps to second precision.

Your **integration task** is small: when a task is deleted via `TaskService.deleteTask`, write a `TASK_DELETED` audit entry through `LegacyAuditDao.recordEvent()`.

The integration alone is ~5 lines. **The exercise is not the integration. The exercise is building a custom subagent that knows how to navigate the legacy module so that this integration — and every future integration with the legacy code — happens correctly the first time.**

## Reproduce

```bash
git checkout meta/03-custom-subagent
mvn test
# 2 visible test failures:
#   - LegacyNavigatorAgentTest.agentFileExists
#   - LegacyAuditIntegrationTest.deletingTask_writesLegacyAuditEntry
```

## What "done" looks like

1. `.claude/agents/legacy-navigator.md` exists with proper YAML frontmatter (`name`, `description`).
2. The agent body documents the legacy module's conventions — at minimum `LegacyAuditDao`, `recordEvent`, and either the upper-case kind convention or the EntityManager pattern.
3. `LegacyAuditIntegrationTest.deletingTask_writesLegacyAuditEntry` passes — deleting a task writes a TASK_DELETED entry.
4. The hidden grading suite (`grading/meta-03-custom-subagent/`) passes — the integration uses the canonical kind, goes through the DAO (not direct entity construction), and writes exactly one entry per delete.

## Custom subagents in Claude Code

A custom subagent is a markdown file at `.claude/agents/<name>.md`. Format:

```markdown
---
name: legacy-navigator
description: Use this when the user is integrating with code in the legacy/ package. Knows the pre-2018 conventions and warns against rewriting them.
---

You are the navigator for the legacy/ package. The conventions are:
- `LegacyAuditDao` is the only safe write path. Callers MUST use `recordEvent(kind, subjectId, actor)`. Never construct `LegacyAuditEntry` directly.
- The `kind` field is canonical upper_snake_case (`TASK_DELETED`, `USER_BANNED`).
- Timestamps are millis-since-epoch (Long), rounded to the second by the DAO.
- Don't migrate this package to JpaRepository — external systems depend on the column-name wire format.

When asked to do anything in legacy/:
1. Confirm the integration uses recordEvent, not direct persistence.
2. Confirm kind is upper-case.
3. Don't propose modernizing the code unless explicitly asked.
```

When invoked, Claude can dispatch this agent for legacy-related work — the agent runs in its own context window, so the main session doesn't bloat with legacy spelunking.

## Why this exercise exists

For most Java teams there's a "legacy" zone — a module written by someone who left, with conventions nobody remembers, that you have to interact with carefully. Without a custom subagent, every Claude session has to rediscover these. *With* one, the institutional knowledge gets encoded once and carried forward.

This is one of the highest-ROI moves on a legacy-heavy codebase. Most engineers don't do it. The 5 minutes to define a subagent saves hours over a year.

## See also

[`CLAUDE_INSTRUCTIONS.md`](CLAUDE_INSTRUCTIONS.md) — recommended workflow.
