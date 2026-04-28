# Exercise 05 — Investigate from a vague symptom

**Type:** investigate-then-fix (no TODO markers, no obvious failing test name)
**Estimated time:** 30–60 min with Claude Code

## The bug report

> *"When users double-tap our signup button, we sometimes end up with two
> records that have the same username. Customer success has flagged it three
> times in the last week, but we can't reliably reproduce it. Can someone
> look?"* — internal Slack message, last Tuesday

That's all you get. No stack trace, no log line, no failing test. This is what most production bugs look like.

## Reproduce

```bash
git checkout exercise/05-investigate-vague-symptom
mvn test
```

A new test, `UserConcurrencyTest`, fails consistently:

```
expected exactly one user with username 'alice' after 20 concurrent creates
  (observed: 10 successes, 10 conflicts, 10 rows persisted)
```

That confirms the report. **Now you need to find the root cause.** No file in the repo has a `TODO(exercise-05)` comment pointing at the problem — the whole point of this exercise is to *find* it.

## What "done" looks like

- All 20 visible tests pass — including `UserConcurrencyTest`.
- Concurrent creates of the same username produce exactly **one** user row, regardless of timing.
- Concurrent creates of *different* usernames still all succeed.
- Loser threads receive **HTTP 409 Conflict**, not 500. (The hidden grading suite checks this — it's the difference between "the bug is gone" and "the bug is *fixed*".)

## Hints (look only if stuck for 15+ minutes)

<details>
<summary>Hint 1 — narrow the scope</summary>

The failing test creates the same username many times in parallel. Where in the code does username uniqueness get checked? Read `UserService.createUser` — what's the sequence of operations?
</details>

<details>
<summary>Hint 2 — name the pattern</summary>

The check ("does this username exist?") and the action ("save the new user") happen in two separate steps with nothing preventing another transaction from slipping between them. This pattern has a name: **TOCTOU** (time-of-check-to-time-of-use), also called a check-then-act race. Application-level uniqueness checks are *always* racy unless you also have a database constraint or a serializable transaction.
</details>

<details>
<summary>Hint 3 — pick an approach</summary>

Two complementary moves:
1. Add a UNIQUE index on `users.username` so the database catches dupes regardless of timing.
2. Translate the resulting `DataIntegrityViolationException` into a 409 response so loser threads get a clean error instead of a 500.

Both are necessary. Doing only (1) leaves loser threads getting 500s. Doing only (2) is impossible without (1) — there's no exception to catch if the DB lets the duplicate through.
</details>

## Why this exercise exists

The four earlier exercises all tell you *what kind of problem* you're looking at and gesture at *where* to look. Real bugs don't. The skill this branch trains is the chain:

> vague symptom → script a reliable repro → form hypotheses → confirm one → fix at the right layer

`CLAUDE_INSTRUCTIONS.md` walks through doing that with Claude Code rather than guessing.
