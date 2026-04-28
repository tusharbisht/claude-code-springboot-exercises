# Meta 06 — Prompting tactics

**Type:** meta-skill (the deliverables enforce a sequence of four tactics)
**Estimated time:** 90–120 min with Claude Code

## What's missing

A real feature request: **task history** (audit trail for every change to a task). See [`FEATURE_REQUEST.md`](FEATURE_REQUEST.md) for the spec — it deliberately leaves several open questions.

## Reproduce

```bash
git checkout meta/06-prompting-tactics
mvn test                 # 2 failing tests:
                         #   SpecArtifactTest.specExists
                         #   TaskHistoryFeatureTest.createUpdateDelete_producesHistoryEntries
```

## What "done" looks like

Two artifacts plus a working feature:

1. **`SPEC.md`** at the repo root, written *before* you start coding. It must:
   - Have an `Overview`, `Endpoints`/`API`, `Data model`/`Schema`, `Constraints`/`Non-goals` section
   - Resolve the open questions in `FEATURE_REQUEST.md` (diff vs snapshot, deletion semantics, pagination)
   - Be substantive — at least 1500 chars in the hidden grading suite
2. **`TaskHistoryFeatureTest`** passes — the visible feature test that the test-first tactic produces
3. **The history feature** — entity, repository, service hooks on create/update/delete, `GET /api/tasks/{id}/history` endpoint
4. The implementation matches the SPEC.md (you took a position on diff/snapshot, deletion handling, etc., and stuck to it)

## The four tactics, in order

This exercise forces a workflow most engineers skip. Use Claude with these tactics:

### 1. Spec-first

> "Read FEATURE_REQUEST.md. Don't write code. Draft `SPEC.md` answering each open question. For diff vs snapshot, give me both options with trade-offs, then recommend one. For deletion, decide what `GET /tasks/{id}/history` does after the task is deleted."

The output is `SPEC.md` — a markdown file you commit. The hidden grading suite checks it's substantive.

### 2. Test-first

> "Based on SPEC.md, write `TaskHistoryFeatureTest` (or extend the existing one) to pin every behaviour the spec promises. Don't implement the feature yet — just commit a failing test."

Run `mvn test` — the new test fails because the implementation doesn't exist. Good. Commit.

### 3. Adversarial

Once Claude proposes the implementation:

> "Before you implement: find three things wrong with this plan. Argue against your own approach. Then revise."

Real test of this tactic: the *first* plan is rarely the best. Forcing Claude to critique its own plan surfaces issues you'd otherwise discover at runtime.

### 4. Constraint-shaped

After the feature works:

> "Now re-implement under these constraints: at most one new entity, no new dependencies, no changes to the existing TaskService method signatures. Show me the diff."

Constraint-shaped re-prompts produce sharper code. They also catch over-engineering — usually the second pass is half the size of the first.

## What the hidden grading suite checks

- `SPEC.md` is substantive (≥1500 chars, addresses diff vs snapshot)
- History endpoint orders entries newest-first
- Deleting a task either still surfaces history (200) or returns 404 — your SPEC.md picked one
- The session log is checked for tactic markers (advisory only — won't fail the build, but prints what tactics it spotted)

## Why this exercise exists

Most Claude Code use is reactive: a problem appears, you describe it, Claude fixes it. That works for small bugs. It doesn't work for features bigger than ~200 lines.

The four tactics above are how senior engineers structure feature work, with or without an LLM. **Spec-first** prevents implementation drift. **Test-first** prevents over-build. **Adversarial** catches bad plans before they cost time. **Constraint-shaped** trims fat.

When you use these with Claude in the loop, the velocity gain is huge — Claude does the typing, you do the structure. This exercise builds the muscle for that.

## See also

- [`FEATURE_REQUEST.md`](FEATURE_REQUEST.md) — what the product team asked for
- [`CLAUDE_INSTRUCTIONS.md`](CLAUDE_INSTRUCTIONS.md) — illustrated workflow with each tactic
