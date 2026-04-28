# Solving Exercise 07 with Claude Code — faster and better

Migrations are where Claude Code earns its biggest wins on real Java work. Spring point releases, Hibernate major bumps, Java version upgrades — they all share the same pattern: small mechanical changes scattered across many files, plus *one* conceptual change that has to be understood up-front. This exercise is a tiny example of that shape.

## Recommended workflow

### 1. Make Claude read the spec, not the code

> "Read the EXERCISE.md and the failing tests in `ProblemDetailComplianceTest`. Don't read any production code yet. Tell me: what does the target shape look like, and what content type is expected for error responses?"

If Claude leads with "let me look at GlobalExceptionHandler" — push back. The spec comes from the failing test and the RFC, not from the existing code.

### 2. Read Spring's docs *before* writing code

> "Use the WebFetch tool to read [Spring's ProblemDetail docs](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-ann-rest-exceptions.html). Summarize: how do I construct a ProblemDetail, how do I add a custom property, and what does Spring's `ResponseEntityExceptionHandler` give me for free?"

This is the migration-specific move: read the framework's own docs before guessing at the API. On real Spring/Hibernate migrations, the release notes and migration guides are the authoritative source — Claude is good at consuming them quickly when you point it at them.

### 3. Plan the migration as small steps

Plan mode (`Shift+Tab`):

> "Plan the migration as a sequence of small commits. Each step must keep at least the previously-green tests green. Acceptable end state: all 24 tests pass and GlobalExceptionHandler returns ProblemDetail for every exception type."

A good plan looks like:
1. Migrate the 404 handler to ProblemDetail; update tests that asserted `$.message` for not-found
2. Migrate the 409 handler; update its tests
3. Migrate the 400 (IllegalArgumentException) handler
4. Migrate the validation handler — preserve `fieldErrors` as an extension property
5. Confirm content type is `application/problem+json` everywhere
6. Run the full suite; verify the new ProblemDetailComplianceTest is green

A bad plan rewrites the whole handler in one go.

### 4. Tell Claude to identify *which* tests need updating, not "fix all the tests"

> "Without modifying anything, list every assertion in `src/test/java/...` that references `$.message`, `$.error`, or `$.timestamp` on an error response. For each, tell me the new RFC 7807 field it should reference."

This produces a punch list, which you can then apply in one focused edit. Much safer than asking Claude to "update the tests" wholesale.

### 5. Apply, run, push back

After each step:

```bash
mvn -B test                             # full suite
mvn -B -Dtest=ProblemDetailComplianceTest test   # the new contract specifically
```

If a test fails unexpectedly, **read the assertion** before patching. Sometimes the test is right and Claude introduced a regression. The plural form of "I'll just fix this" is a broken handler.

### 6. Run the hidden grading suite

```bash
./grading/run-grading.sh
```

It checks edge cases the visible tests don't: the path in `$.instance`, type-mismatch errors (`/api/tasks/not-a-number`), and that the legacy keys are *gone* (not just optional).

## Claude Code techniques that pay off here

| Technique | Why it matters |
| --- | --- |
| **Read the spec before the code** | migrations are constraint-driven; the target shape is the contract |
| **WebFetch the framework docs** | release notes / migration guides are authoritative; Claude consumes them quickly |
| **Plan mode for the migration order** | doing small steps in the right order keeps tests usable as guardrails |
| **"List the assertions to change" before changing them** | avoids mass test edits that bury real regressions |
| **Run both the new and the existing suites between steps** | the migration is "done" only when both pass |

## What NOT to do

- Don't disable or delete the existing tests "to clean up". Update the assertions; keep the coverage.
- Don't add a try/catch around Spring's exception types just to convert them. Use Spring's `@ExceptionHandler` machinery — extending `ResponseEntityExceptionHandler` is even nicer.
- Don't introduce a new dependency. ProblemDetail is in core Spring 6 / Boot 3.
- Don't fabricate URI types for `$.type`. `about:blank` is a valid default per RFC 7807; only switch to richer URIs if you're going to actually publish them.
- Don't store the timestamp manually. ProblemDetail doesn't include `timestamp` per RFC 7807 — that's the point.

## When you're stuck

> "I migrated 404 to ProblemDetail and `notFoundError_isProblemDetail` passes, but `getUser_notFound_returns404` (the existing test) is now failing. Read both tests and tell me what changed."
