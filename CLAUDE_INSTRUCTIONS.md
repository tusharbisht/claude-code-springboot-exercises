# Solving Exercise 05 with Claude Code — faster and better

The hard part of this exercise is *not* the fix — once you know it's a TOCTOU race, the fix is small. The hard part is **getting to the diagnosis from a vague bug report**. That's the muscle this exercise trains.

## Recommended workflow

### 1. Don't ask Claude for the bug. Ask Claude to read the symptoms.

A bad first prompt:
> "There's a concurrency bug in user creation. Find and fix it."

You've already done the diagnostic work. You're paying Claude to be a typist.

A good first prompt:
> "Run `mvn -Dtest=UserConcurrencyTest test`. Show me the assertion message and the failing test method. Don't read any other files yet — just describe what the test is doing and what it observed."

You'll see something like *"20 threads tried to create the same username; 10 succeeded, 10 got conflicts, 10 ended up in the DB"*. Now you have a concrete observation, not a vague report.

### 2. Form hypotheses *before* reading source

> "Before reading any source code: from that observation alone, give me 3 plausible causes ranked by likelihood. For each, list one specific thing you'd check to confirm or rule it out."

What good looks like:
1. **Race between the existence check and the insert** (TOCTOU). Confirm by reading whether the check and the save are inside the same locking transaction.
2. **Missing DB-level unique constraint.** Confirm by checking the JPA entity / DDL.
3. **Stale repository cache returning false from `existsByUsername`.** Confirm by checking JPA second-level cache config.

The order matters — TOCTOU is by far the most likely on a Spring Data JPA codebase. If Claude leads with "second-level cache", push back.

### 3. Have Claude confirm one hypothesis at a time

> "Confirm hypothesis 1 by reading `UserService.createUser` and `User.java` only. Don't fix anything — tell me what you found."

If you delegate "investigate the bug" wholesale, Claude tends to read 12 files and write a long essay. Confirming hypotheses *one at a time* is much faster and keeps the context tight.

### 4. Decide the fix together

Once Claude confirms TOCTOU + missing DB constraint, ask:

> "Plan the smallest fix that satisfies: (a) at most one row per username regardless of timing, (b) loser threads get HTTP 409, not 500, (c) no new dependencies. Compare to alternatives: a synchronized block, a serializable transaction, a distributed lock. Recommend one."

The right answer is **DB unique constraint + catch DataIntegrityViolationException → 409**. Synchronized blocks don't survive multiple JVM instances. Serializable transactions are heavyweight. Distributed locks are absurd at this scale.

### 5. Apply, verify, push

After applying the plan:

```bash
mvn test                                    # all 20 visible tests pass
./grading/run-grading.sh                    # hidden tests verify the 409 contract
git diff main                               # should be ~10 lines across 2-3 files
```

If `mvn test` passes consistently across 5 reruns (`for i in 1 2 3 4 5; do mvn -Dtest=UserConcurrencyTest test || break; done`) you're confident. Concurrency tests can be flaky — *especially* a fix you don't fully understand.

## Claude Code techniques that pay off here

| Technique | Why it matters |
| --- | --- |
| **Symptom first, hypothesis second, source third** | resists Claude's tendency to dive into files before knowing what to look for |
| **Hypothesis ranking** | makes Claude commit to a most-likely cause; if wrong, you learn faster |
| **One hypothesis confirmed per turn** | keeps context narrow and answers concrete |
| **Compare alternative fixes explicitly** | catches "Claude picked the first thing that worked" — the cheapest fix is rarely the first one proposed |
| **Re-run the failing test 5× to confirm** | concurrency fixes that "pass once" may still race |

## What NOT to do

- Don't ask Claude to "wrap the createUser method in `synchronized`". It would work in a single-JVM test and break the moment you run two replicas in production. The exercise is about the right architectural fix, not the most local one.
- Don't write a fix that converts `DataIntegrityViolationException` to `RuntimeException("user exists")`. Use the existing `DuplicateResourceException` and the existing `GlobalExceptionHandler` mapping — the codebase already has a 409 path.
- Don't add a `@Transactional(isolation = Isolation.SERIALIZABLE)` "to be safe". It's a heavyweight knob that masks the real fix.
- Don't introduce a `@Lock` annotation, a Redis distributed lock, or a queue. Wildly disproportionate.

## When you're stuck

Re-read EXERCISE.md hints in order. Or try:

> "I've added a unique constraint and the visible test passes. But the hidden grading test says loser threads get 500 instead of 409. Show me where DataIntegrityViolationException is currently being handled — and where it should be."
