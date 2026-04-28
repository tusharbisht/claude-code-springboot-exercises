# Exercise 02 — Implement task search

**Type:** implement
**Estimated time:** 30–60 min with Claude Code

## What's missing

`GET /api/tasks/search` is wired up at the controller level but **always responds with HTTP 501 Not Implemented**. The supporting service method and repository query don't exist either — they're left as `TODO(exercise-02)` comments.

The endpoint must accept any subset of these query parameters and return tasks matching all of them (an AND filter):

| param | type | meaning |
| --- | --- | --- |
| `status` | enum (`TODO`, `IN_PROGRESS`, `DONE`) | task status |
| `priority` | enum (`LOW`, `MEDIUM`, `HIGH`) | task priority |
| `assigneeId` | long | task is assigned to this user |
| `dueBefore` | ISO date | due date `<=` value (inclusive) |
| `dueAfter` | ISO date | due date `>=` value (inclusive) |

When **no parameters** are supplied, the endpoint should return **all tasks**. When *some* are supplied, only those filter; the rest are ignored.

## Reproduce

```bash
git checkout exercise/02-implement-search
mvn test
```

The 3 search-related tests in `TaskApiIntegrationTest` fail — they expected 200 with a result list, got 501 (or a stack trace from inside Spring). The test names tell you exactly what shape the answer should take:
- `searchTasks_byStatus_returnsMatching`
- `searchTasks_byAssignee_returnsMatching`
- `searchTasks_byPriority_returnsMatching`

## What "done" looks like

- All 19 visible tests pass — the 3 search tests in particular
- The implementation lives across **three layers**:
  - `TaskRepository` gains a query that takes the five filter parameters
  - `TaskService` gains a `search(...)` method that delegates to the repository and maps to DTOs
  - `TaskController.searchTasks(...)` delegates to the service instead of throwing 501
- No new dependencies, no `Specification` API needed (a JPQL `@Query` with nullable parameters is fine)

## What the hidden grading suite checks

Visible tests prove the basic shape works. The hidden grading suite (`grading/exercise-02-implement-search/HiddenSearchGradingTest.java`) checks edge cases that bite real implementations:

- search with **no params** returns all tasks (not an empty list, not 400)
- `dueBefore` / `dueAfter` are **inclusive** on the boundary day
- multiple filters AND together correctly
- **invalid enum value** returns 400, not a stack trace
- **invalid date format** returns 400
- **no matches** returns `[]`, not `null` or 404

These run automatically in CI when you push the branch (or locally with `./grading/run-grading.sh`).

## See also

[`CLAUDE_INSTRUCTIONS.md`](CLAUDE_INSTRUCTIONS.md) — recommended Claude Code workflow for this exercise.
