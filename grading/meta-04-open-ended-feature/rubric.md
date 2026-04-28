# Grading rubric — meta/04 task labels feature

Score each axis from 0–10, then sum. Total: 0–60.

## 1. Functional correctness (0–10)

- Create label, attach, detach, list-by-task, list-by-label all work
- Behaviour matches the FEATURE_REQUEST.md must-haves

> Common deductions: detach silently fails when not attached; list-by-label returns null on missing label; attach is non-idempotent.

## 2. REST hygiene (0–10)

- HTTP verbs match semantics (POST creates, DELETE removes, GET reads)
- Status codes are accurate (201 on create, 204 on detach, 404 on missing, 409 on conflict)
- URL paths read naturally (e.g., `/api/tasks/{id}/labels` rather than `/api/tasks/attachLabel`)
- Resources are nouns, not verbs

> Common deductions: 200 OK on a create that should be 201; using `/api/labels/attach` style action paths; treating IDs as query params instead of path params.

## 3. Validation (0–10)

- Label name length enforced (1–30)
- Color format enforced (valid hex like `#RRGGBB`)
- Empty/blank inputs rejected with 400
- Validation errors include which field failed

> Common deductions: validation only at the entity level (so the response is 500 from a DB constraint instead of 400 from `@Valid`); regex for hex color rejects valid casing or accepts invalid like `#ZZZZZZ`.

## 4. Error handling (0–10)

- Missing label/task → 404 with a useful message
- Duplicate label name → 409 (or returns the existing label idempotently — defend either choice)
- Malformed inputs → 400, not 500
- Errors use the same shape as the rest of the codebase (the existing GlobalExceptionHandler shape)

> Common deductions: stack traces in responses; inconsistent error shapes between endpoints; treating "label already attached" as 500.

## 5. Consistency with the codebase (0–10)

- Follows the conventions in `CLAUDE.md` (constructor injection, JPQL @Query, no Lombok)
- Controllers stay thin; service layer owns business logic
- DTOs are records where outbound, mutable POJOs where inbound (Jackson)
- New entity follows the existing `Task`/`User` patterns

> Common deductions: `@Autowired` field injection in the new code; Lombok introduced; controller talks directly to a new repository.

## 6. Edge case design (0–10)

- Idempotent attach (attaching the same label twice is safe)
- Cascade behaviour on label delete is documented (do tasks lose the label?)
- Empty label name and trailing whitespace handled
- Very long task lists handled without blowing up

> Common deductions: attaching the same label twice creates duplicate join rows; deleting a label leaves dangling references.

---

## Overall scoring guide

- **50–60**: Production-ready; reviewer would approve with minor comments
- **40–49**: Solid; needs a follow-up PR for some edges
- **30–39**: Functional but rough; reviewer would request meaningful changes
- **20–29**: Half-finished; significant gaps in correctness or hygiene
- **0–19**: Doesn't fulfill the must-haves

Be strict. Generic answers ("looks good!") get docked — point at specific evidence.
