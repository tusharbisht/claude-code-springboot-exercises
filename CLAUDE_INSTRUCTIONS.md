# Solving Exercise 06 with Claude Code — faster and better

This is the highest-ROI use of Claude Code on Java in real life: **scaffolding tests for an existing class**. It's also the easiest place to be lulled by "looks plausible" output. The workflow below trains you to use Claude *and* push back on it.

## The danger you need to avoid

Claude generates tests very quickly. They will look professional, use AssertJ correctly, and probably pass. But:
- Tests aren't useful if they assert what the code *currently does* rather than what the code *should* do.
- Claude has no way to know which methods have bugs. If it tests "what the code does", buggy code stays buggy with green tests.

You'll get a lot less value from "write all 8 tests" than from "let me write the spec; you write the assertions". The Javadoc in each `@Test` is your spec.

## Recommended workflow

### 1. Have Claude read the contracts before writing anything

> "Read NotificationService.java carefully. Don't write code yet. For each of the 5 public methods, summarize: what's the input, what's the contract, and what edge cases would be worth testing?"

What good looks like: an itemised list of inputs/outputs/edges per method, including the boundaries (zero, null, empty, sign changes). What you should reject: any output that says "I tested it manually and it works."

### 2. Test ONE method at a time, with you reading the assertions

> "Write the body of `formatTaskAssignment_includesTitleAndAssignee`. Use only the spec in the Javadoc on the test method. Don't run anything yet — just show me the diff."

Read the diff *before* applying. Specifically check:
- Does the assertion match the spec word-for-word, or did Claude paraphrase it (and lose precision)?
- Are the test inputs the *minimum* needed to exercise the case?
- Is the test calling other helpers from the file (`task(...)`, `user(...)`) rather than reinventing them?

Apply, run, see what happens.

### 3. When a test fails, decide who's wrong

This is the heart of the exercise. A failing test means one of three things:
1. Your test is wrong — you misread the spec or set up bad inputs
2. The production code has a bug
3. The spec itself is ambiguous

Ask Claude to help you triage:

> "Test `formatDueReminder_today_says_dueToday` failed: expected 'Due today' but got 'Overdue by 0 days'. Read the spec in the Javadoc on `formatDueReminder` (in NotificationService.java) and tell me whether the test is wrong or the production code is wrong. Quote both before answering."

Forcing Claude to *quote both* before judging is the move. Without that, it tends to side with the most recent code change you made.

### 4. Fix the SMALLEST possible thing

If the production code is wrong, fix the buggy method only. Run the failing test alone (`mvn -Dtest=NotificationServiceTest#formatDueReminder_today_says_dueToday test`). Then run the full suite. Then move on.

If the test is wrong, fix the test. Don't "fix" the production code to match a wrong test.

### 5. Repeat for the other 7 tests

By the end you should have written ~8 tests, fixed ~3 bugs, and rerun the suite ~10 times. The diff to `NotificationService.java` should be a handful of lines per buggy method — not a rewrite.

### 6. Run the hidden grading suite

```bash
./grading/run-grading.sh
```

The hidden suite tests the same surface but with different assertions. If your tests passed but the hidden ones fail, your *coverage* was thin — Claude tested the happy path of each spec but missed an edge. Read the failing hidden test and use Claude to:

> "Hidden test `groupingHandlesNullInput` fails. The visible test I wrote covered the happy path but didn't cover null input. Add a test for null input to `NotificationServiceTest` matching that case."

Then verify the visible test catches the same bug as the hidden one.

## Claude Code techniques that pay off here

| Technique | Why it matters |
| --- | --- |
| **Spec → assertion, one at a time** | Claude will happily generate 8 tests in one shot; reading them critically is much harder than reviewing 1 at a time |
| **"Show me the diff before applying"** | catches paraphrased specs and missing edges |
| **Quote-both-before-judging** | when a test fails, Claude needs to read both spec and code, not pick a side |
| **Run a single test by name** (`mvn -Dtest=...#methodName`) | fast feedback loop; full suite is too coarse for test-by-test work |
| **Compare visible vs hidden coverage** | if hidden tests fail, your tests aren't broken — they're shallow |

## What NOT to do

- Don't ask Claude to "write all 8 tests at once and I'll review at the end". You won't review at the end. You'll merge.
- Don't ask Claude to "look at the code and write tests that match its behaviour". That tests the bug.
- Don't add tests for private helpers (`task(...)`, `user(...)`). They're test scaffolding.
- Don't refactor `NotificationService` while you're fixing bugs. Each bug is local. Save refactoring for a separate change.
- Don't accept a Claude-generated test that uses Mockito to mock the `Task` entity. These are pure unit tests against POJOs — no mocking needed.

## When you're stuck

> "Test X is now passing but I don't trust it. Walk me through the test, then independently walk me through what the production code does for the same input. If they agree, why? If they disagree, who's right?"
