# Exercise 09 — When Claude is confident but wrong

**Type:** adversarial — Claude's first answer passes the visible test but is wrong
**Estimated time:** 30–60 min with Claude Code

## What's missing

`POST /api/tasks/bulk` is wired up at the controller level but throws `501 Not Implemented`. It accepts a `BulkCreateTasksRequest { tasks: List<CreateTaskRequest> }` and is supposed to return a `BulkCreateTasksResponse { count, tasks }`.

The contract is **atomic bulk-create**: either every task in the request is persisted, or none are. If any single task fails validation, references a non-existent assignee, or the database errors out — the whole batch must roll back.

## Reproduce

```bash
git checkout exercise/09-confident-but-wrong
mvn test
```

`BulkCreateHappyPathTest.bulkCreate_allValid_createsAllTasks` fails. That's the *only* visible test for this endpoint. **It only covers the happy path.** This is deliberate, and is the entire lesson of the exercise.

## What "done" looks like

- All 20 visible tests pass — including the happy-path bulk test
- Hidden grading suite passes (atomicity contract: invalid entry rolls back the batch, missing assignee rolls back, empty list rejected, oversized batch rejected, retry after failure works cleanly)
- The implementation lives in `TaskService` (with `@Transactional` declared correctly), called by the controller method that currently throws

## Why this is the most important exercise in the course

Most Claude-Code use ends here:

> Learner: *"Implement bulkCreate."*
> Claude: *implements it as a `tasks.forEach(taskService::createTask)` loop*
> Learner: *runs `mvn test`* — happy-path test is green
> Learner: *commits, pushes, ships*

That implementation is wrong. It satisfies the visible test (all tasks valid → all tasks created) but not the contract (any invalid task → none created). When task #5 throws on the production database, tasks 1–4 are already committed. The user sees 4 mystery tasks, the system reports failure, and the inconsistency is yours to debug at 2 AM.

The visible test is permissive on purpose. The hidden grading suite encodes the *full* contract. The exercise is about practicing the move that prevents this in production: **when Claude proposes a fix that passes the test, ask "what's the unspoken contract this doesn't cover?"** — *before* you ship.

## See also

[`CLAUDE_INSTRUCTIONS.md`](CLAUDE_INSTRUCTIONS.md) — the workflow that catches this kind of bug *before* the hidden grading suite does.
