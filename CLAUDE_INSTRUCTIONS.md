# Solving Exercise 01 with Claude Code — faster and better

The point of this branch is **not** just to fix the bug — you could probably eyeball the missing `@Valid` in 30 seconds. The point is to learn a reproducible **diagnose-then-fix** loop with Claude Code that scales to bugs you *can't* eyeball.

## Recommended workflow

### 1. Don't tell Claude what's wrong. Let it find out.

A bad first prompt:
> "Add `@Valid` to the controller methods."

A good first prompt:
> "Run `mvn test` and tell me which tests are failing and what error each one produces. Don't fix anything yet."

This forces Claude to read the actual failures, not pattern-match on the file name. You'll see whether it:
- correctly identifies the failing assertions (e.g. "`createUser_blankUsername_returns400` expects HTTP 400 but gets 201"),
- groups them by likely cause, or
- gets distracted by a stack trace.

### 2. Ask for a hypothesis before a fix

> "Based on those failures, what's the most likely root cause? Give me 2–3 hypotheses ranked by likelihood."

A strong response will mention something like: "validation annotations on the DTO are not being enforced — likely missing `@Valid` on the controller `@RequestBody` parameter, or `spring-boot-starter-validation` is missing from `pom.xml`." Notice that it considers *more than one* explanation. If Claude jumps straight to a fix, push back.

### 3. Have it confirm the hypothesis before editing

> "Confirm hypothesis 1 by reading both controllers and `pom.xml`. Don't edit yet — just tell me what you found."

This is where Claude's parallelism shines: it can read all three files in one turn. It should report back with concrete findings ("the `validation` starter is in `pom.xml`, but `@Valid` is missing from every `@RequestBody` parameter in both controllers").

### 4. Then ask for the smallest possible fix

> "Apply the minimum change needed to make all visible tests pass. Don't refactor anything else. Show me the diff before applying."

Watch for:
- The change is *only* in the two controllers
- Nothing else moves (no imports for unrelated classes added, no DTOs modified)
- Claude re-runs `mvn test` after applying, not just claims it works

### 5. Verify, then push

```bash
mvn test                  # all 19 green
git diff main             # should be ~6 lines across 2 files
git push origin exercise/01-fix-validation-bug
```

CI will run the **hidden grading suite**. If those tests fail, that's feedback worth reading — they cover edge cases that your visible tests don't (whitespace usernames, boundary-length titles, the *shape* of the 400 response body).

## Claude Code techniques used in this exercise

| Technique | Why it matters here |
| --- | --- |
| `mvn test` from inside Claude | tests are the source of truth; don't trust "this looks right" |
| Read multiple files in one turn | controllers + DTOs + `pom.xml` together — same context |
| Hypothesis → confirmation → fix | prevents Claude from "fixing" the wrong thing confidently |
| `git diff main` after the fix | proves the change is minimal; catches accidental edits |

## What NOT to do

- Don't ask Claude to "make the tests pass." That phrasing sometimes leads to *changing the tests*. Ask it to fix the **production code**.
- Don't accept a fix that adds custom exception handling, a new validator class, or DTO refactors. The bug is small; the fix is small.
- Don't skip running `mvn test` yourself. The Stop hook will run it for you and write `.claude/last-progress.json`, but seeing the live output catches surprises faster.

## When you're stuck

Re-read `EXERCISE.md` Hints 1 → 2 → 3, in order. If the hidden grading suite still flags edge cases after your visible tests pass, ask Claude:

> "The hidden grading suite expects whitespace-only usernames to be rejected. `@NotBlank` should already do that — verify the DTO and explain why it isn't catching `'   '`."

(Spoiler: `@NotBlank` *does* catch whitespace-only strings — but only when `@Valid` is present. If you fixed exercise 01 correctly, this case is already handled.)
