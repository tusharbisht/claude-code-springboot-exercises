# Exercise 06 — Write tests from scratch (and find the bugs they uncover)

**Type:** test-writing + bug-fixing
**Estimated time:** 45–75 min with Claude Code

## What's missing — and why this is the most realistic exercise

`NotificationService` is a brand-new class that ships with `0%` test coverage. It looks fine on a code review. It has subtle bugs you won't see by reading it. The exercise:

1. Use Claude Code to **write tests** for it (pick a method, write a test, repeat).
2. Some of those tests **will fail** because the production code has bugs.
3. **Read the failures critically.** A failing test you wrote is a chance to learn whether the *test* is wrong or the *code* is wrong.
4. Fix the bugs.

This is a faithful reproduction of how 60% of Java work actually goes: the code "works on my machine" until someone writes a test for it.

## Reproduce

```bash
git checkout exercise/06-tests-from-scratch
mvn test
```

You'll see 8 failing tests in `NotificationServiceTest` — they're scaffolded with `fail("TODO(exercise-06)")` and Javadoc specs. Each spec describes what the method should do; your job is to turn each into a real test.

## What "done" looks like

- All 27 visible tests pass — including the 8 you'll fill in
- The bug fixes you apply to `NotificationService` are localised to the buggy methods (no rewriting working code)
- You've actually *read* the assertions Claude generated — not just rubber-stamped them

## The methods you're testing

`NotificationService` exposes 5 methods. The Javadoc on each is the contract:

| Method | Spec lives at |
| --- | --- |
| `formatTaskAssignment(Task)` | NotificationService.java:14 |
| `formatDueReminder(Task, long daysUntilDue)` | NotificationService.java:31 |
| `groupTasksByAssignee(List<Task>)` | NotificationService.java:53 |
| `daysUntilDue(Task, LocalDate today)` | NotificationService.java:69 |
| `summarizeStatus(List<Task>)` | NotificationService.java:91 |

Three of them have bugs — I won't tell you which. Your tests will.

## Hidden grading suite

Once `mvn test` is green, push the branch. CI runs the hidden grading suite (`grading/exercise-06-tests-from-scratch/HiddenNotificationGradingTest.java`) which covers the same surface from a fresh angle — same methods, different assertions, all the edge cases. If your fix was specific to the case *your* tests covered, the hidden suite will catch it.

## See also

[`CLAUDE_INSTRUCTIONS.md`](CLAUDE_INSTRUCTIONS.md) — recommended Claude Code workflow.
