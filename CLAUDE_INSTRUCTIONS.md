# Solving Exercise 03 with Claude Code — faster and better

This exercise is about **reading the SQL Hibernate generates** and reasoning about it. The behavioural tests pass. The fix is small. The hard part is *seeing* the problem.

## Recommended workflow

### 1. See the queries before you change anything

> "Run `mvn -Dtest=UserListingPerformanceTest test` and show me the failure message. Then enable Hibernate SQL logging and re-run — paste the SQL statements that ran during the call to `GET /api/users`."

Concretely: have Claude add `spring.jpa.show-sql=true` (or use `logging.level.org.hibernate.SQL=DEBUG`) temporarily so the SQL stream appears in the test output. With seeded data of 5 users you should see one `select ... from users` followed by five `select count(*) from tasks where assignee_id = ?`. That's the N+1.

### 2. Locate the hot loop with a precise question

> "Read `UserService.java` and tell me which method generates that pattern of queries. Don't fix it — just point at the line."

This is much better than "find the N+1 problem" because it gives Claude a target. The answer should be the `.map(...)` lambda inside `listUsersWithTaskCounts`.

### 3. Pick the simplest fix

In plan mode (`Shift+Tab`):

> "Plan the fix. Constraint: at most 2 SQL statements regardless of user count, no caching, no pagination. Compare two approaches: a single JPQL projection vs. one users query + one grouped-count query merged in Java."

Look for a plan that picks **the JPQL projection** as the primary fix because it's the smallest diff and keeps all logic in one place. The `dto.UserSummaryDto` record is already designed for projection (its constructor matches the column list).

### 4. Implement, then verify the query count

> "Implement the JPQL projection approach. After the change, re-run the failing test and report the prepared-statement count from the assertion message."

Expect:
- ~10 lines added in `UserRepository` (the new `@Query` returning `UserSummaryDto`)
- 1 line changed in `UserService` (the body of `listUsersWithTaskCounts` becomes `return userRepository.findAllWithTaskCounts();`)
- nothing else moves

### 5. Run the hidden grading suite

```bash
./grading/run-grading.sh
```

The hidden test scales the user count to 10 and asserts the same `≤ 2` bound — if you accidentally introduced a "looks small with 5 users but grows" implementation, this catches it.

## Claude Code techniques that pay off here

| Technique | Why it matters |
| --- | --- |
| **Toggle Hibernate SQL logging** | the queries are the evidence; reading them once changes how you write JPA forever |
| **Read `UserSummaryDto`** | knowing the DTO's constructor signature suggests a JPQL projection without you having to design one |
| **`mvn -Dtest=ClassName` for fast feedback** | each full `mvn test` is ~3 s of context churn; running just the failing class is faster |
| **Plan-mode comparison** | makes Claude defend the chosen approach against the alternative — surfaces hidden assumptions |
| **Keep the diff minimal** | optimizations that touch unrelated code accumulate risk |

## What NOT to do

- Don't refactor `Task` to use `@OneToMany` on `User`. That changes the model for an unrelated reason and risks introducing a new N+1 elsewhere.
- Don't add `@BatchSize` or `@Fetch(FetchMode.SUBSELECT)`. Those help when fetching child rows; here we want a count, not the children.
- Don't introduce a cache. The visible test will pass, the hidden test will pass, but the *lesson* is missed — and on the next data change you'll serve stale counts.
- Don't skip checking `git diff main`. Optimization PRs that touch many files are usually the wrong shape.

## Stuck? Try this prompt

> "I have a JPQL query that LEFT JOINs Task and groups by user, but I'm getting one row per (user × task). Show me the GROUP BY clause and tell me what's missing."
