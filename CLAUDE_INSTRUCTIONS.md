# Solving Exercise 02 with Claude Code — faster and better

This is a **multi-layer feature** (repository + service + controller). Easy to over-engineer, easy to under-test, easy to ask Claude for "the implementation" and get back something that works for the happy path but mishandles edge cases. The workflow below avoids all three traps.

## Recommended workflow

### 1. Get Claude to read the contract first — not write code

> "Read `EXERCISE.md`, `TaskApiIntegrationTest.java` (the search-related tests only), and the existing `TaskRepository`, `TaskService`, `TaskController`. Summarize: what does the endpoint contract look like, and which layers need new code?"

Why this matters: if you ask Claude to "implement search" cold, it'll guess at semantics (does `dueBefore` mean strict or inclusive? do missing params mean 400 or "no constraint"?). The visible tests answer those questions. Have Claude **read the tests as the spec**, not your prose.

### 2. Use plan mode for the design

Press `Shift+Tab` to enter plan mode, then:

> "Plan the implementation. List the changes per file, the JPQL query, and any test gaps. Don't write code — just the plan."

Look for:
- A single `@Query` on `TaskRepository` rather than an explosion of `findByXxxAndYyy` methods
- The plan handles nullable parameters (`:status IS NULL OR t.status = :status`)
- It mentions inclusive bounds on `dueBefore` / `dueAfter` (the hidden tests check this)
- It does NOT propose `Specification`, `Querydsl`, or any heavyweight abstraction

If the plan looks good, accept it. If not, push back ("simpler — one JPQL query, no Specifications").

### 3. Implement, then verify with tests

Once the plan is solid:

> "Implement the plan. After each layer change, re-run `mvn test` and report the failure count."

Expect three small commits' worth of changes. After implementation, all 19 visible tests should pass.

### 4. Run the hidden grading suite locally

```bash
./grading/run-grading.sh
```

The hidden tests will surface bugs the visible ones missed — typically:
- `dueBefore` exclusive instead of inclusive
- empty-result returning `null` instead of `[]`
- invalid enum returning 500 instead of 400 (Spring's binding handles this for free if you use enum-typed `@RequestParam`s, which the controller already does — so this should just work)

If a hidden test fails, **read the test name carefully** and ask Claude:

> "The hidden test `dueBeforeIsInclusive` is failing. Show me the JPQL where clause for `dueBefore` and walk me through whether it includes the boundary day."

### 5. Push and let CI run the full grading

```bash
git push origin exercise/02-implement-search
```

The PR comment from `.github/workflows/grade.yml` shows the result. If you set `EVAL_WEBHOOK_URL` (locally for the Stop hook, or as a repo secret for CI), an instructor can also see your progress.

## Claude Code techniques that pay off here

| Technique | Why it matters |
| --- | --- |
| **Read tests as the spec** | the visible tests literally encode the contract — cheaper than asking |
| **Plan mode** | three-layer changes need design discipline before code |
| **Parallel reads** | controllers + service + repo + tests in a single read batch |
| **Re-run tests after each layer** | catches "compiles but wrong" early — JPQL nulls are easy to mishandle |
| **`./grading/run-grading.sh` locally before push** | the CI feedback loop is slower; run it locally first |

## What NOT to do

- Don't ask Claude to "use Spring Data JPA Specifications." It's idiomatic but overkill. A single `@Query` with nullable parameters is shorter, clearer, and what the hidden test expects to see.
- Don't add new endpoints. The controller method is already there.
- Don't change the test file. If a test is "wrong" in your view, that's evidence you've misunderstood the contract — re-read.
- Don't catch and rewrap exceptions in the controller. Spring already maps invalid enums and dates to 400 via binding errors.

## Stuck? Try this prompt

> "I have all 19 visible tests passing but `noMatchesReturnsEmptyArray` in the hidden grading suite fails. Read `HiddenSearchGradingTest.java` to see the assertion, then explain what my repository or service is returning when no rows match."
