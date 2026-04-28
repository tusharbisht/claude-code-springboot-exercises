# Feature request: task labels

We want users to organize tasks with **labels** (tags). The product team gave us this rough spec — fill in the gaps with sensible REST design.

## Must-have

- Users can create a label with a `name` (unique, 1–30 chars) and a `color` (hex, e.g., `#3366FF`)
- Users can attach one or more labels to a task
- Users can detach a label from a task
- Users can list all labels
- Users can fetch a task and see its labels
- Users can list all tasks that have a given label

## Constraints

- Don't break any existing endpoint or test (`mvn test` must stay green for the existing 19)
- Use the existing patterns in this codebase (CLAUDE.md describes them)
- Don't add new dependencies to `pom.xml`

## Deliberately left to your judgement

- URL paths — pick something that reads naturally
- Detach semantics — body, path param, query param?
- What happens when attaching a label that's already attached?
- What happens when creating a label whose name already exists?
- Pagination on the listing endpoints
- Error response shapes for validation failures, missing labels, etc.

There's no "right" answer to those. Pick one, defend it. The grading judge will exercise the API as a user would and score correctness, REST hygiene, error handling, and consistency.

## How this is graded

There is **no failing visible test** for this feature. There is no hidden grading test. Instead, an **LLM-driven judge** acts as a user walking through your API:

```bash
ANTHROPIC_API_KEY=sk-... ./grading/meta-04-open-ended-feature/run-judge.sh
```

The judge:
1. Boots your app on `localhost:8080`
2. Plans a walkthrough as a real user would
3. Executes it via HTTP calls
4. Probes adversarial / edge cases
5. Scores you against `grading/meta-04-open-ended-feature/rubric.md`
6. Writes `grading-result.json` with a score and a written critique

You're done when you've shipped a label feature you'd be willing to defend in a PR review.

See `CLAUDE_INSTRUCTIONS.md` for the recommended workflow and `grading/meta-04-open-ended-feature/rubric.md` for the scoring criteria — they're public so you can shape your work to them.
