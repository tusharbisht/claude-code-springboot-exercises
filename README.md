# Task Manager — Claude Code Exercises (Java + Spring Boot)

A small Spring Boot REST API used as a playground for **learning Claude Code on a real Java codebase**. Each branch is one teaching scenario. Work them in order, or pick the one that matches what you're avoiding at work.

## Branch map

### Start here

| Branch | Purpose |
| --- | --- |
| `main` | reference — clean working app, all 19 tests green |
| [`tour/workflow`](https://github.com/tusharbisht/claude-code-springboot-exercises/tree/tour/workflow) | 30-min guided walkthrough of Claude Code: plan mode, slash commands, hooks, IntelliJ, the Explore agent. Do this first. |

### Engineering exercises (each: own branch, `EXERCISE.md`, `CLAUDE_INSTRUCTIONS.md`)

| Branch | Type | What you'll do |
| --- | --- | --- |
| [`exercise/01-fix-validation-bug`](https://github.com/tusharbisht/claude-code-springboot-exercises/tree/exercise/01-fix-validation-bug) | **fix** | a bug lets invalid input slip through; tests fail |
| [`exercise/02-implement-search`](https://github.com/tusharbisht/claude-code-springboot-exercises/tree/exercise/02-implement-search) | **implement** | endpoint exists but throws `NOT_IMPLEMENTED`; tests describe the contract |
| [`exercise/03-optimize-n-plus-one`](https://github.com/tusharbisht/claude-code-springboot-exercises/tree/exercise/03-optimize-n-plus-one) | **optimize** | list endpoint runs an N+1 query pattern; a test asserts query count |
| [`exercise/04-refactor-fat-controller`](https://github.com/tusharbisht/claude-code-springboot-exercises/tree/exercise/04-refactor-fat-controller) | **refactor** | controller does business logic; tests stay green, structure must improve |
| [`exercise/05-investigate-vague-symptom`](https://github.com/tusharbisht/claude-code-springboot-exercises/tree/exercise/05-investigate-vague-symptom) | **investigate** | a vague bug report; no TODO markers; you script the repro |
| [`exercise/06-tests-from-scratch`](https://github.com/tusharbisht/claude-code-springboot-exercises/tree/exercise/06-tests-from-scratch) | **tests + bugs** | write tests for an untested service; the tests will surface bugs |
| [`exercise/07-migration`](https://github.com/tusharbisht/claude-code-springboot-exercises/tree/exercise/07-migration) | **migration** | migrate ad-hoc error responses to RFC 7807 ProblemDetail |
| [`exercise/08-when-not-to-use-claude`](https://github.com/tusharbisht/claude-code-springboot-exercises/tree/exercise/08-when-not-to-use-claude) | **calibration** | a one-word fix; *don't* use Claude. Build the instinct. |
| [`exercise/09-confident-but-wrong`](https://github.com/tusharbisht/claude-code-springboot-exercises/tree/exercise/09-confident-but-wrong) | **adversarial** | the visible test passes with Claude's first answer; the hidden grading suite catches what it missed |

### Meta exercises — Claude Code itself is the deliverable

These exercises don't measure Java output; they measure your **Claude Code configuration**. The Java task is the vehicle for surfacing what's missing.

| Branch | Deliverable | What you'll build |
| --- | --- | --- |
| [`meta/01-build-claude-md`](https://github.com/tusharbisht/claude-code-springboot-exercises/tree/meta/01-build-claude-md) | `CLAUDE.md` | implement a feature without `CLAUDE.md`; the convention violations Claude makes are the bullets you need to add |
| [`meta/02-hooks-and-commands`](https://github.com/tusharbisht/claude-code-springboot-exercises/tree/meta/02-hooks-and-commands) | slash commands + hooks | build `/find-controller`, `/test-changed`, and a `PreToolUse` hook that guards `pom.xml` |
| [`meta/03-custom-subagent`](https://github.com/tusharbisht/claude-code-springboot-exercises/tree/meta/03-custom-subagent) | `.claude/agents/legacy-navigator.md` | encode the conventions of a legacy module so future sessions don't re-discover them |
| [`meta/04-open-ended-feature`](https://github.com/tusharbisht/claude-code-springboot-exercises/tree/meta/04-open-ended-feature) | a working feature | open-ended task-labels feature; no failing test; an LLM judge walks through your API and scores you |
| [`meta/05-mcp-servers`](https://github.com/tusharbisht/claude-code-springboot-exercises/tree/meta/05-mcp-servers) | a working MCP server | build a custom MCP server that exposes `list_endpoints` and `summarize_test_failures` to Claude as first-class tools |
| [`meta/06-prompting-tactics`](https://github.com/tusharbisht/claude-code-springboot-exercises/tree/meta/06-prompting-tactics) | `SPEC.md` + a working feature | use spec-first → test-first → adversarial → constraint-shaped, in order, on a real feature |

### Going further

| Branch | Purpose |
| --- | --- |
| [`extra/multi-module-preview`](https://github.com/tusharbisht/claude-code-springboot-exercises/tree/extra/multi-module-preview) | the same app split into 3 Maven modules — practice cross-module navigation |

---

## What you should already know

You'll get the most out of these exercises if you can:
- Read Spring Boot code (controllers, services, repositories, JPA entities)
- Run a Maven build (`mvn test`, `mvn spring-boot:run`)
- Have used REST APIs (curl, Postman, or similar)

You do **not** need to be a Spring expert. The point is to learn how to use Claude Code to navigate, diagnose and change a Java codebase — not to memorize Spring annotations.

## Prerequisites

- **JDK 21+** (we tested on 21 and 25)
- **Maven 3.9+**
- **Git** and a terminal
- **[Claude Code](https://docs.claude.com/en/docs/claude-code/overview)** installed and authenticated

```bash
java -version    # should print 21.x or higher
mvn -version     # should print 3.9.x or higher
claude --version # should print a version string
```

---

## Getting started

```bash
# 1. clone
git clone https://github.com/tusharbisht/claude-code-springboot-exercises.git
cd claude-code-springboot-exercises

# 2. confirm main is green
git checkout main
mvn test                # → Tests run: 19, Failures: 0

# 3. start with the workflow tour
git checkout tour/workflow
cat WORKFLOW_TOUR.md
claude                  # launch Claude Code in this dir

# 4. then pick an exercise
git checkout exercise/01-fix-validation-bug
cat EXERCISE.md
mvn test                # see which tests fail — that's your starting point
cat CLAUDE_INSTRUCTIONS.md
claude
```

When the listed failing tests turn green, push your branch. CI runs a stricter, hidden grading suite (see **Grading** below).

---

## Project layout

```
src/
├── main/java/com/learning/taskmanager/
│   ├── TaskManagerApplication.java     # Spring Boot entrypoint
│   ├── controller/                     # REST endpoints
│   ├── service/                        # business logic
│   ├── repository/                     # Spring Data JPA repos
│   ├── model/                          # JPA entities + enums
│   ├── dto/                            # request/response DTOs
│   └── exception/                      # custom exceptions + global handler
├── main/resources/application.properties
└── test/java/com/learning/taskmanager/
    ├── integration/                    # MockMvc-based API tests (visible)
    └── TaskManagerApplicationTests.java
```

(Multi-module layout on `extra/multi-module-preview` is documented in `MULTI_MODULE_NOTES.md` on that branch.)

### Domain model

- **User** — `id`, `username` (unique), `email`
- **Task** — `id`, `title`, `description`, `status` (`TODO`/`IN_PROGRESS`/`DONE`), `priority` (`LOW`/`MEDIUM`/`HIGH`), `dueDate`, `assignee` (User), `createdAt`, `updatedAt`

### Endpoints

```
POST   /api/users              create user
GET    /api/users              list users with their task counts
GET    /api/users/{id}         get user
GET    /api/users/{id}/tasks   list one user's tasks

POST   /api/tasks              create task
GET    /api/tasks              list all tasks
GET    /api/tasks/{id}         get task
PUT    /api/tasks/{id}         update task
DELETE /api/tasks/{id}         delete task
GET    /api/tasks/search       search by status/priority/assignee/due date
```

---

## Grading & evaluation

Three layers, in increasing strictness:

### 1. Local correctness check (visible)

Tests under `src/test/java/...` are visible to learners and run with `mvn test`. They tell you whether the surface-level requirement is met. When they pass, push.

### 2. Hidden grading suite (CI)

When you push, [`.github/workflows/grade.yml`](.github/workflows/grade.yml) runs a stricter, hidden test suite from `grading/<exercise-slug>/`. The job:

- Loads the hidden tests into `src/test/java/...` before running
- Posts results as a comment on the pull request and uploads a JSON report as an artifact
- Optionally POSTs to a webhook (set `EVAL_WEBHOOK_URL` as a repo secret) so an instructor can track class-wide progress

You can run the same grading locally:

```bash
./grading/run-grading.sh
# or for a specific branch:
./grading/run-grading.sh exercise/03-optimize-n-plus-one
```

### 3. Claude Code progress hooks (optional)

The repo's [`.claude/settings.json`](.claude/settings.json) registers a `Stop` hook that — after each Claude Code session — runs `mvn test` and POSTs a small JSON payload (branch, pass/fail counts, duration) to the URL in `$EVAL_WEBHOOK_URL`. If the env var isn't set, the hook does nothing.

```bash
export EVAL_WEBHOOK_URL=https://your-instructor.example.com/progress
```

A reference webhook receiver lives in [`evaluation-server/`](evaluation-server/) (Python + stdlib, single file).

### 4. Auto-generated `SOLUTION_NOTES.md` (always on)

A separate set of hooks logs every prompt, edit, and bash command to `.claude/session-log.jsonl`, then folds it into `SOLUTION_NOTES.md` at the end of each session. The file is gitignored — its purpose is in-session **formative feedback**, not grading. After each session you'll see a one-line summary like:

```
[session] 7 prompts • 3 files edited • 4 mvn test runs → SOLUTION_NOTES.md updated
```

The notes file shows you, in retrospect, what your workflow with Claude actually looked like. Disable with `CLAUDE_DISABLE_SESSION_LOG=1` if you don't want it.

### 5. LLM-judge grading (meta/04 only)

`meta/04-open-ended-feature` has no failing test. Grading is an LLM that boots your app, walks through it as a user, and scores against a rubric. Requires `ANTHROPIC_API_KEY`. See the branch's [`EXERCISE.md`](https://github.com/tusharbisht/claude-code-springboot-exercises/blob/meta/04-open-ended-feature/EXERCISE.md) for details.

---

## Reading order recommendation

For a self-paced learner aiming for ~10 hours total:

1. `tour/workflow` (30 min) — orient yourself
2. `exercise/08-when-not-to-use-claude` (5 min) — calibrate first
3. `exercise/01-fix-validation-bug` (15 min) — easy win, builds the diagnose loop
4. `exercise/03-optimize-n-plus-one` (30 min) — see the SQL Claude shows you
5. `exercise/02-implement-search` (45 min) — multi-layer plan-mode practice
6. `exercise/04-refactor-fat-controller` (45 min) — disciplined refactor with tests
7. `exercise/06-tests-from-scratch` (75 min) — highest-ROI Claude workflow
8. `exercise/09-confident-but-wrong` (45 min) — the most-important calibration exercise
9. `exercise/05-investigate-vague-symptom` (60 min) — production-shaped, no hand-holding
10. `exercise/07-migration` (75 min) — the most realistic real-world Claude use case

Then the meta track — these change *how* you'll use Claude going forward:

11. `meta/01-build-claude-md` (60 min) — feel CLAUDE.md as a load-bearing document
12. `meta/02-hooks-and-commands` (75 min) — slash commands and hooks as personal infrastructure
13. `meta/03-custom-subagent` (75 min) — encode institutional knowledge as Claude Code config
14. `meta/06-prompting-tactics` (90–120 min) — spec-first, test-first, adversarial, constraint-shaped
15. `meta/05-mcp-servers` (90–120 min) — extend Claude's toolbelt with project-specific MCP servers
16. `meta/04-open-ended-feature` (90–180 min) — work without test oracles, with an LLM judge

Optional: `extra/multi-module-preview` (30 min) — practice cross-module navigation.

---

## Troubleshooting

**`mvn` says no Java runtime** → run `brew install openjdk@21`, then `export JAVA_HOME=$(/usr/libexec/java_home -v 21)`.

**Port 8080 already in use** → `lsof -i :8080`, kill the offender, or set `server.port=8090` in `application.properties`.

**H2 console rejects empty password** → leave the password field blank and click *Connect*.

**Tests pass locally but CI grading fails** → that's the point. Hidden tests cover edge cases. Read the CI logs.

---

## Contributing / forking

Add new exercises by:
1. Branching from `main`: `git checkout -b exercise/NN-<your-slug>`
2. Mutating the code to introduce the issue + the failing visible test
3. Adding `EXERCISE.md` and `CLAUDE_INSTRUCTIONS.md`
4. Adding a hidden test set under `grading/exercise-NN-<your-slug>/` (the runner picks it up automatically — no edit to `run-grading.sh` needed)
5. Updating the table at the top of this README
