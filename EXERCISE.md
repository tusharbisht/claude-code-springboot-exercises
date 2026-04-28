# Exercise 01 — Fix the validation bug

**Type:** fix
**Estimated time:** 10–20 min with Claude Code

## What's wrong

Several integration tests that should reject malformed input are now failing. The DTOs (`CreateUserRequest`, `CreateTaskRequest`, `UpdateTaskRequest`) carry the right Bean Validation annotations (`@NotBlank`, `@Email`, `@Size`), but **the constraints aren't being enforced**. Invalid requests slip through to the service layer and either get silently saved or blow up with a 500 instead of a clean 400.

## Reproduce

```bash
git checkout exercise/01-fix-validation-bug
mvn test
```

You should see something like:

```
[ERROR] Tests run: 19, Failures: 4, Errors: 1
  in com.learning.taskmanager.integration.UserApiIntegrationTest
  in com.learning.taskmanager.integration.TaskApiIntegrationTest
```

The failing tests are the ones whose names contain `_returns400` or describe boundary conditions.

## What "done" looks like

- `mvn test` passes — all 19 visible tests green
- The fix is **minimal** — don't rewrite the controllers, don't add custom exception handling, don't change the DTOs. Find what's missing and put it back.
- `git diff main` should show changes in only **one or two files** in `src/main/java/com/learning/taskmanager/controller/`.

## Hints (look only if stuck for 10+ minutes)

<details>
<summary>Hint 1 — where to look</summary>

The DTOs already have `@NotBlank`, `@Email`, etc. So the validation rules are present. The question is: who tells Spring to *enforce* them?
</details>

<details>
<summary>Hint 2 — the missing piece</summary>

Spring MVC only runs Bean Validation on a `@RequestBody` parameter when that parameter is annotated with `@Valid` (or `@Validated`). Compare the `@RequestBody` parameters in the controllers with the imports they currently use.
</details>

<details>
<summary>Hint 3 — the fix</summary>

Add `@Valid` to every `@RequestBody` parameter in `UserController` and `TaskController`. Re-import `jakarta.validation.Valid` if it was removed.
</details>

## Going further (after the visible tests pass)

The hidden grading suite (`grading/exercise-01-fix-validation-bug/`) has additional cases — whitespace-only username, malformed-but-not-empty email, exactly-200-char title, oversized description. Pushing your branch triggers them via CI. Run them locally:

```bash
./grading/run-grading.sh
```

See [`CLAUDE_INSTRUCTIONS.md`](CLAUDE_INSTRUCTIONS.md) for the recommended Claude Code workflow.
