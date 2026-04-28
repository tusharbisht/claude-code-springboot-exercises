# Exercise 04 — Refactor the fat controller

**Type:** refactor
**Estimated time:** 30–60 min with Claude Code

## What's wrong

`TaskController` has grown into a **fat controller**:
- It depends on `TaskRepository` and `UserRepository` directly.
- It contains business logic (default values, assignee lookup, partial-update field mapping).
- It manages its own transactions with `@Transactional`.

`TaskService` exists with all the right method signatures, but the controller bypasses it entirely. This is the most common decay pattern in Spring Boot codebases — controllers start as thin pass-throughs and slowly absorb logic until they're untestable, untouchable monoliths.

The behavioural tests pass. The lesson is *structural*.

## Reproduce

```bash
git checkout exercise/04-refactor-fat-controller
mvn test
```

The two failing tests are in `TaskControllerStructureTest`:

```
TaskControllerStructureTest.taskController_doesNotDependOnRepositories — FAIL
TaskControllerStructureTest.taskController_dependsOnTaskService       — FAIL
```

The 19 behavioural tests still pass — that's the constraint of a refactor: **behaviour stays identical, structure changes**.

## What "done" looks like

- `TaskController` has **one** collaborator: `TaskService`. No repository fields.
- Every endpoint in `TaskController` is a one-liner that delegates to a `TaskService` method.
- `@Transactional` annotations live in `TaskService`, not in the controller.
- All 21 visible tests pass — the 19 behavioural ones plus the 2 structural ones.

## Why this matters

Tests as written can pass with logic in any layer. But:
- **Reusability** — service methods can be called from a scheduler, a CLI, or another endpoint; a controller method can't.
- **Transaction boundaries** — `@Transactional` on a controller method ties the transaction to the HTTP request lifecycle, which is wrong as soon as you have any non-HTTP caller.
- **Test depth** — service tests can use `@DataJpaTest` or unit-mock the repos. Controller tests are forced to be `@SpringBootTest` because they own logic *and* HTTP wiring.

## What the hidden grading suite checks

Beyond the visible structural tests, the hidden suite (`grading/exercise-04-refactor-fat-controller/`) verifies via reflection:
- `TaskController` has exactly **one** non-primitive field (the service)
- `TaskService` exposes all six operations (`createTask`, `updateTask`, `deleteTask`, `getTask`, `listTasks*`, `search`)

These run automatically in CI on push.

## See also

[`CLAUDE_INSTRUCTIONS.md`](CLAUDE_INSTRUCTIONS.md) — recommended Claude Code workflow.
