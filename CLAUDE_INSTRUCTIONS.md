# Solving Exercise 04 with Claude Code — faster and better

Refactors are where Claude Code really earns its keep. The mechanics are tedious; the discipline (small steps, run tests between each, never let behaviour drift) is what makes them safe. This exercise is about turning a fat controller back into a thin one *without* breaking any of the 19 behavioural tests.

## Recommended workflow

### 1. Read both files in parallel before deciding anything

> "Read `TaskController.java` and `TaskService.java` and tell me: which controller methods could call which service methods? Are there any service methods missing? Don't change anything yet."

This single read often reveals that the service is already complete and the controller can simply delegate. If the service is missing methods, you'll know what's missing **before** you start moving code.

### 2. Plan the refactor in small, verifiable steps

Press `Shift+Tab` for plan mode:

> "Plan the refactor as a sequence of small commits. Each step must keep all 19 behavioural tests green. The end state: TaskController has one field (TaskService), each endpoint is a one-liner that delegates."

A good plan looks like:
1. Wire `TaskService` into the controller (add field, constructor — don't remove repos yet)
2. Replace `createTask` body with `taskService.createTask(request)` — run tests
3. Replace `getTask` — run tests
4. Replace `listTasks` — run tests
5. Replace `updateTask` — run tests
6. Replace `deleteTask` — run tests
7. Replace `searchTasks` — run tests
8. Remove `TaskRepository` and `UserRepository` fields and imports
9. Remove `@Transactional` annotations from the controller
10. Run all tests + structural tests

A bad plan rewrites everything in one pass. Push back if Claude proposes that.

### 3. Execute step by step, running tests every time

For each step:

> "Apply step N. Then run `mvn test` and report the result before moving on."

If a step turns red, **stop and read the error before patching**. Often it's a missing method on the service, which is one small addition rather than a series of guesses.

### 4. Do the structural cleanup last

Only once every endpoint delegates to the service:
- Remove the `TaskRepository` and `UserRepository` constructor parameters and fields
- Remove unused imports (Claude can do this — `mvn compile` will catch any missed)
- Remove `@Transactional` from controller methods (the service already declares them)

After cleanup the structural tests should turn green.

### 5. Verify nothing drifted

```bash
mvn test                 # all 21 green
git diff main -- src/main/java/com/learning/taskmanager/controller/TaskController.java
```

The diff should be **dramatically smaller** than the starting state — short delegating methods, one collaborator, no transaction annotations.

## Claude Code techniques that pay off here

| Technique | Why it matters |
| --- | --- |
| **Plan mode for multi-step refactors** | catches "rewrite everything at once" before you regret it |
| **One commit per endpoint, run tests in between** | a refactor that breaks a test mid-flight is salvageable; one that breaks 5 tests across 6 changes is not |
| **Read the existing TaskService first** | it's already 80% there — don't reinvent it |
| **`git diff main` after each step** | proves the change is mechanical, not design-y |
| **Imports cleanup as a separate step** | mixed-concern commits are harder to review and harder to revert |

## What NOT to do

- Don't introduce a new abstraction (`TaskFacade`, `TaskOrchestrator`, `TaskCommandHandler`). The lesson is fewer layers, not more.
- Don't change DTO shapes. The behavioural tests are your contract; if a DTO field changes, you've drifted.
- Don't merge `UserController` and `TaskController`. They're separate by URL prefix and should stay so.
- Don't move `@Valid` off the controller — validation lives at the HTTP boundary.
- Don't add `@Transactional` at the class level on `TaskController`. That's still a controller-side transaction. Move it to the service.

## Stuck? Try this prompt

> "I removed the TaskRepository field from TaskController and now `createTask` won't compile. Walk me through what TaskService methods exist and which one createTask should call."
