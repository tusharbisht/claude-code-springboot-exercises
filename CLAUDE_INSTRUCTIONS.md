# Solving Exercise 09 with Claude Code — faster and better

This exercise has only one lesson, but it's the lesson that separates careful Claude users from confident-but-wrong ones: **the visible test isn't the contract.** Claude will pattern-match on the test and produce something that passes it. Whether the result is *correct* is your job, not Claude's.

## The trap

If you launch Claude on this exercise with no caveats, here's what's almost certain to happen:

1. Claude reads `BulkCreateHappyPathTest`. Sees three tasks come in, three tasks come out.
2. Claude reads `TaskService.createTask`. Sees a one-task method.
3. Claude writes the obvious bridge:
   ```java
   public BulkCreateTasksResponse bulkCreate(BulkCreateTasksRequest req) {
       List<TaskDto> created = req.getTasks().stream()
               .map(this::createTask)
               .toList();
       return new BulkCreateTasksResponse(created.size(), created);
   }
   ```
4. `mvn test` passes. Claude reports success. You commit.

That code violates atomicity. If task #5 throws (invalid assignee, DB error), tasks 1–4 are already committed inside their own transactions. You get partial state with no error reported on tasks 1–4. The hidden grading suite will catch this. **Production won't.**

## Recommended workflow

### 1. Make Claude state the contract before writing any code

> "Read `EXERCISE.md` and `BulkCreateHappyPathTest.java`. State the contract for `POST /api/tasks/bulk` in 3 bullet points. Don't write code yet."

If Claude's bullets are limited to "accept N tasks, return N tasks" — push back:
> "What's missing? What does the EXERCISE.md say must hold when one of the tasks is invalid? Re-state the contract including failure cases."

The lesson is for Claude to *say it out loud*. If atomicity isn't in Claude's stated contract, the implementation won't have it either.

### 2. Make Claude propose two implementations, not one

> "Plan two implementations: (A) a simple loop that iterates and calls createTask for each, (B) a single transactional method that pre-validates everything, then persists. Compare them on what happens when task #5 is invalid."

This is the single most important habit to build. Claude's first plan is biased toward simplicity. Forcing a comparison surfaces the trade-off, and it's *you* who picks.

### 3. Reject the wrong one explicitly

> "Approach A is wrong because it leaves rows 1–4 committed when row 5 throws. Implement approach B. Use `@Transactional` on the service method. Pre-validate all assignee references in a single query before saving anything."

### 4. Run the visible test AND probe the failure paths yourself

After implementation:

```bash
mvn -B -Dtest=BulkCreateHappyPathTest test    # passes
```

But don't stop there. Hand-craft an adversarial request:

```bash
curl -X POST http://localhost:8080/api/tasks/bulk \
  -H 'Content-Type: application/json' \
  -d '{"tasks":[{"title":"a"},{"title":""},{"title":"c"}]}'
# expect: 4xx, and AFTER the call, GET /api/tasks should be empty
```

You're playing the hidden grader. If your implementation persists "a" and "c" while rejecting the request, you have a bug.

### 5. Run the hidden grading suite locally before pushing

```bash
./grading/run-grading.sh
```

This runs all 6 hidden atomicity tests. If any fail, the hidden grader has caught what you missed.

## The general lesson

The pattern this exercise teaches generalizes to **every** Claude Code task:

| Move | What it catches |
| --- | --- |
| Make Claude state the contract before coding | Implicit assumptions in the test that aren't really the spec |
| Force two-option comparisons | Claude's bias toward the first plausible answer |
| Hand-probe failure paths the test doesn't | Edge cases the visible test was never going to cover |
| Re-read the diff *as a code reviewer*, not as the typist | Subtle issues Claude introduced confidently |

You can apply this to every other exercise in this course. **You can also skip applying it.** The one thing you *can't* do is "skip applying it" while genuinely getting better at Claude Code. The discipline IS the skill.

## What NOT to do

- Don't accept "the visible test passes" as evidence of correctness. It's evidence Claude pattern-matched the test, nothing more.
- Don't ask Claude to "make this atomic" without making it state the failure cases first. You'll get atomicity for the cases Claude thought of, not the ones you needed.
- Don't try to write the failure-case tests yourself before solving — the *exercise* is to verify atomicity *without* making the visible test more thorough. (If you change `BulkCreateHappyPathTest` to cover failure cases, you've just made the exercise easier and missed the lesson.)
- Don't add try/catch to "fix" a partial-failure issue. That hides the symptom; transactional rollback fixes the cause.

## When you're stuck

> "I have approach B implemented and the happy-path test passes, but the hidden test `invalidInMiddle_rollsBackAll` fails. Read my service method and tell me whether `@Transactional` will actually roll back when a `ConstraintViolationException` is thrown — and whether validation runs at all in that path."
