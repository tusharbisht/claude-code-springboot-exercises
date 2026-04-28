# Exercise 07 — Migrate to RFC 7807 ProblemDetail

**Type:** migration (touches code AND tests)
**Estimated time:** 45–90 min with Claude Code

## What's wrong

`GlobalExceptionHandler` returns an ad-hoc error shape:

```json
{
  "timestamp": "2025-04-28T10:42:11Z",
  "status": 400,
  "error": "Bad Request",
  "message": "validation failed",
  "fieldErrors": { "username": "username is required" }
}
```

This worked fine before Spring Boot 3 made [RFC 7807](https://datatracker.ietf.org/doc/html/rfc7807) (`application/problem+json`) the standard error format. We're catching up. Migrate the handler so every error response is a `ProblemDetail`:

```json
{
  "type": "about:blank",
  "title": "Bad Request",
  "status": 400,
  "detail": "Validation failed for 1 field",
  "instance": "/api/users",
  "fieldErrors": { "username": "username is required" }
}
```

## Reproduce

```bash
git checkout exercise/07-migration
mvn test
```

A new test class, `ProblemDetailComplianceTest`, has 4 failures (the 5th passes — that's the sanity check that successful responses *don't* change). Other tests are green.

## What "done" looks like

- `ProblemDetailComplianceTest` is fully green
- Existing 19 tests still green (the migration must not regress behaviour)
- Error responses use `application/problem+json` content type
- The body has the RFC 7807 mandatory fields: `type`, `title`, `status`, `detail`, `instance`
- `fieldErrors` survives as an **extension property** on the ProblemDetail — downstream clients still read it
- The legacy `message`, `error`, `timestamp` keys are gone

## What's allowed in this exercise

Unlike most exercises in this repo, **you may modify test files** here. Migration changes contracts; tests that pinned the old contract have to follow. Look in particular at the existing tests that assert `$.message` / `$.error` / `$.timestamp` on error responses and update them to the RFC 7807 equivalents.

(Don't change tests that pin behaviour unrelated to error shapes — only the assertions that referenced the old error contract.)

## Spring Boot pointers (no spoilers)

- Spring 3.x ships `org.springframework.http.ProblemDetail`
- `ProblemDetail.forStatusAndDetail(status, detail)` is your factory
- `setProperty(name, value)` adds an extension field (e.g. `fieldErrors`)
- `setInstance(URI)` populates `$.instance`
- `MediaType.APPLICATION_PROBLEM_JSON` is the right content type
- `ResponseEntityExceptionHandler` (Spring's base class) already produces ProblemDetail for some built-in exceptions if you extend it — worth investigating

## What the hidden grading suite checks

- All 4 RFC 7807 mandatory + recommended fields present on every error
- `instance` reflects the actual request path
- `fieldErrors` extension preserved
- Type-mismatch errors (`/api/tasks/not-a-number`) also return ProblemDetail
- Legacy keys (`message`, `error`, `timestamp`) are *gone* — not just optional

## See also

[`CLAUDE_INSTRUCTIONS.md`](CLAUDE_INSTRUCTIONS.md) — recommended Claude Code workflow for migrations.
