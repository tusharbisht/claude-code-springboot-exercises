# Exercise 03 — Optimize the N+1 query

**Type:** optimize
**Estimated time:** 15–30 min with Claude Code

## What's wrong

`GET /api/users` returns each user with their task count. The current implementation does this naively: it loads all users, then iterates and runs **one `COUNT(*)` query per user**. With N users that's N+1 SQL statements — a textbook N+1 problem.

The contract is correct (response shape, counts, ordering all match what the visible tests expect). What's wrong is **how** the response is built.

## Reproduce

```bash
git checkout exercise/03-optimize-n-plus-one
mvn test
```

The visible test that fails:

```
UserListingPerformanceTest.listUsers_doesNotIssueOneQueryPerUser
  expected: ≤ 2 prepared statements
  actual:   6 (= 1 findAll + 5 COUNT)
```

The other 19 tests still pass — proving the *behavior* is right, only the *cost* is wrong.

## What "done" looks like

- The performance test passes: GET `/api/users` runs in **at most 2 prepared statements** regardless of user count.
- All other visible tests still pass (the response shape, ordering, and counts must not change).
- The fix is a **data-access change**, not a caching layer or pagination workaround.
- `git diff main` should show changes in **`UserService` and `UserRepository` only** (typically: a new JPQL projection query + a one-line service change).

## Acceptable approaches

Any of these will pass the assertion:
1. **Single JPQL projection** — `LEFT JOIN Task` + `GROUP BY u.id` returning `UserSummaryDto` directly (recommended; simplest)
2. **Two queries** — load all users, then a single `groupBy` count keyed by assignee, then merge in memory (still O(2), still passes)
3. **Native SQL** — same idea, slightly less portable; only worth it if JPQL can't express the projection cleanly (it can)

What will **not** work and is not the lesson:
- Caching the response (hides the symptom, doesn't fix the query)
- Pagination (doesn't reduce queries per page)
- `@BatchSize` on the `Task` entity (helps for fetching tasks themselves; we want counts, not the rows)

## What the hidden grading suite checks

- 10 users with 3 tasks each — query count must still be `≤ 2` (proves it's truly O(1), not just "small enough")
- task counts in the response are correct (1 → 3, etc.)

## See also

[`CLAUDE_INSTRUCTIONS.md`](CLAUDE_INSTRUCTIONS.md) — recommended Claude Code workflow.
