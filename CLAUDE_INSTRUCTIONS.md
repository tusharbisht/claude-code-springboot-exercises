# Solving Meta 04 with Claude Code — faster and better

This is the only exercise in the course where there's no test that pins the answer. The judge plays the role tests usually play. Your workflow with Claude has to change accordingly.

## What changes when there's no test as oracle

In every other exercise, you can run `mvn test` after each change and know whether you broke something. Here, the *contract* is the rubric — and the rubric isn't checkable mechanically. You have to **simulate the judge yourself before running it for real.**

Concretely: you write your own probe before invoking the LLM. If your probe doesn't catch issues, you're going to fail the judge.

## Recommended workflow

### 1. Read the rubric BEFORE writing any code

> "Read `grading/meta-04-open-ended-feature/rubric.md` and `FEATURE_REQUEST.md`. List the 6 axes the judge will score. For each, give me one specific example of what would lose points."

The rubric is your spec. The judge is going to test against it. Internalize it before writing the first DTO.

### 2. Plan the data model and endpoints in plan mode

`Shift+Tab` for plan mode:

> "Plan the implementation: data model, JPA entities, repository interfaces, service methods, controller endpoints, request/response DTOs. Don't write code. Address each rubric axis: how does the plan handle validation, error responses, idempotency on attach, and edge cases like deleting a label that's still attached to tasks?"

The first plan is rarely the right one. **Push back twice:**

- *"What if I attach the same label to a task twice?"*
- *"What HTTP status do I return if I try to attach a label that doesn't exist?"*
- *"How does this look from the rest hygiene axis — am I using verbs in URL paths anywhere?"*

Force the plan to address rubric line items explicitly. *Then* implement.

### 3. Implement and probe yourself

Build the feature. Then *before* running the judge, hand-craft a curl walkthrough that mimics what the judge will do. Bonus credit: write a small Java integration test that exercises the happy path end-to-end. If your own probes don't catch issues, the judge's will.

```bash
mvn spring-boot:run     # in one terminal
# in another:
curl -X POST localhost:8080/api/labels -d '{"name":"urgent","color":"#FF0000"}' -H 'Content-Type: application/json'
curl -X POST localhost:8080/api/labels -d '{"name":"urgent","color":"#FF0000"}' -H 'Content-Type: application/json'  # what happens?
curl -X POST localhost:8080/api/labels -d '{"name":"","color":"not-a-color"}' -H 'Content-Type: application/json'    # what happens?
curl -X POST localhost:8080/api/tasks/99999/labels/abc -H 'Content-Type: application/json'                            # what happens?
```

Each of those is a question the judge will ask. If your answer is a 500, you're going to lose points.

### 4. Run the judge

```bash
export ANTHROPIC_API_KEY=sk-...
./grading/meta-04-open-ended-feature/run-judge.sh
```

The judge writes `grading-result.json`. Read the **critique** carefully — it cites specific requests and responses. The score is mechanical; the critique is where the learning is.

If you get < 36/60, the critique will tell you exactly what to fix. Iterate.

### 5. Iterate to ≥ 50/60

The 60% threshold is the bar to ship. The 80% bar is what a strong engineer hits. Each axis has 10 points; aim for 8+ on every axis, not 10 on three and 4 on the rest.

## The deeper lesson

Most production work looks like this. You ship a feature, real users hit it in ways you didn't anticipate, the bugs you hear about are the ones you didn't write tests for. The discipline this exercise trains is **simulating the user before they show up**.

Three habits that compound:

1. **Hand-craft adversarial probes before each push.** If you can't break it, you're not trying hard enough.
2. **Treat the rubric as a spec, not a secret.** Real teams have these — they're called acceptance criteria.
3. **Critique > score.** The score is a number. The critique is the engineering feedback. Read it like a code review.

## What NOT to do

- Don't try to game the judge. The model is calibrated to be strict and to cite evidence — generic answers don't fool it. Time spent gaming is time not spent shipping.
- Don't run the judge before you've probed yourself. Each judge run costs API calls. The first run is for "is this in the right shape?" — your own curl is faster.
- Don't add new dependencies "to score better." The rubric explicitly checks codebase consistency; new dependencies usually hurt that axis more than they help others.
- Don't optimize for the judge's specific probes. The model improvises; if you only handle the cases it asked you about last time, the next run will fail differently.

## When you're stuck

> "I just got a 32/60. The critique says I lost points on validation because empty label name returns 500 not 400, and on rest hygiene because /api/attachLabel is a verb in a URL. Help me fix both. After fixing, walk through MY probe (not the judge's) to verify."

## After this exercise

You've now experienced the open-ended-feature workflow with Claude Code. Notice what was different: **the rubric replaced the test.** That's the real-world default. Tests are a luxury; specs and reviewers are the norm. Train Claude (and yourself) to work against specs, not just tests.
