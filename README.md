# Task Manager — Claude Code Exercises (Java + Spring Boot)

A small Spring Boot REST API used as a playground for **learning to use Claude Code on a real Java codebase**. Each exercise lives on its own branch and represents a category of problem you'll regularly meet in production Spring Boot work:

| Branch | Type | What you'll do |
| --- | --- | --- |
| `main` | reference | clean working app, all tests green |
| `exercise/01-fix-validation-bug` | **fix** | a bug lets invalid input slip through; tests fail |
| `exercise/02-implement-search` | **implement** | endpoint exists but throws `NOT_IMPLEMENTED`; tests describe the contract |
| `exercise/03-optimize-n-plus-one` | **optimize** | list endpoint runs an N+1 query pattern; a test asserts query count |
| `exercise/04-refactor-fat-controller` | **refactor** | controller does business logic; tests stay green, structure must improve |

Each exercise branch ships with two docs:
- `EXERCISE.md` — what's broken / missing, how to verify
- `CLAUDE_INSTRUCTIONS.md` — how to solve it **faster and better with Claude Code**

---

## What you should already know

You'll get the most out of these exercises if you can:
- Read Spring Boot code (controllers, services, repositories, JPA entities)
- Run a Maven build (`mvn test`, `mvn spring-boot:run`)
- Have used REST APIs (curl, Postman, or similar)

You do **not** need to be a Spring expert. The point is to learn how to use Claude Code to navigate, diagnose and change a Java codebase — not to memorize Spring annotations.

---

## Prerequisites

- **JDK 21+** (we tested on 21 and 25)
- **Maven 3.9+** (or use the bundled `./mvnw` wrapper if your fork has one)
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
# 1. clone your fork
git clone https://github.com/<your-username>/claude-code-springboot-exercises.git
cd claude-code-springboot-exercises

# 2. confirm the reference branch builds and tests pass
git checkout main
mvn test
# → expect: Tests run: 19, Failures: 0, Errors: 0

# 3. start the app locally (optional — the tests do the same thing in-memory)
mvn spring-boot:run
# → http://localhost:8080/api/tasks
# → H2 console: http://localhost:8080/h2-console (jdbc:h2:mem:taskdb, user 'sa', empty password)
```

### Picking an exercise

```bash
git checkout exercise/01-fix-validation-bug   # or 02 / 03 / 04
cat EXERCISE.md          # read the problem description first
mvn test                 # see which tests fail — that's your starting point
cat CLAUDE_INSTRUCTIONS.md  # read this BEFORE you launch Claude Code
claude                   # launch Claude Code in the project root
```

When the listed failing tests turn green, you're done with the basic check. Push your branch and CI will run a stricter, hidden grading suite (see **Grading** below).

---

## How to use Claude Code on these exercises

These exercises are designed to teach a specific workflow:

1. **Read `EXERCISE.md` first.** Understand the problem in your own head before delegating it.
2. **Run the failing tests.** See the actual error message. This is what you'll show Claude.
3. **Open Claude Code in the repo root** so it can use the codebase context.
4. **Ask focused questions, not "fix it all."** Examples:
   - "Run `mvn test` and tell me which assertions are failing and why."
   - "Find every place in the codebase where `@Valid` is used. Compare with where it *should* be used."
   - "Show me the SQL Hibernate generates when I call `GET /api/users`. Is there an N+1 problem?"
5. **Use plan mode (`Shift+Tab`)** for the refactor exercise — let Claude propose the structure before editing.
6. **Verify with `mvn test` after each change.** Don't accept "should work" — *prove* it.

Each `CLAUDE_INSTRUCTIONS.md` walks through the *specific* Claude Code techniques most useful for that exercise (search agents, plan mode, parallel reads, git diff verification).

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
    ├── integration/                    # MockMvc-based API tests (visible to learners)
    └── TaskManagerApplicationTests.java
```

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

Each exercise has two layers of tests:

### 1. Local correctness check (visible)

The tests under `src/test/java/...` are the ones you see and run with `mvn test`. They tell you **whether the surface-level requirement is met**. When they pass, push your branch.

### 2. Hidden grading suite (CI)

When you push to GitHub, the workflow `.github/workflows/grade.yml` runs a **stricter, hidden test suite** that exercises edge cases the visible tests don't (boundary conditions, query counts, spec compliance). The grading job:

- Loads the hidden tests from `grading/<exercise-slug>/` into `src/test/java/...` before running
- Posts results as a comment on the pull request and uploads a JSON report as an artifact
- Optionally posts to a webhook (set `EVAL_WEBHOOK_URL` as a repo secret) so an instructor can track class-wide progress

You can run the same grading locally:

```bash
./grading/run-grading.sh exercise/01-fix-validation-bug
```

### 3. Claude Code progress hooks (optional)

The repo's `.claude/settings.json` registers a `Stop` hook that — after each Claude Code session — runs `mvn test` and POSTs a small JSON payload (branch, pass/fail counts, duration) to the URL in `$EVAL_WEBHOOK_URL`. If the env var isn't set, the hook does nothing. This is meant for cohort-style learning, not solo practice.

To enable:

```bash
export EVAL_WEBHOOK_URL=https://your-instructor.example.com/progress
# Claude Code will pick up .claude/settings.json automatically
```

A reference webhook receiver lives in `evaluation-server/` (Python + Flask, single file).

---

## Troubleshooting

**`mvn` says no Java runtime** → run `brew install openjdk@21`, then `export JAVA_HOME=$(/usr/libexec/java_home -v 21)`.

**Port 8080 already in use** → `lsof -i :8080`, kill the offender, or set `server.port=8090` in `application.properties`.

**H2 console rejects empty password** → leave the password field blank and click *Connect* (no quotes).

**Tests pass locally but CI grading fails** → that's the point. The hidden tests cover edge cases. Read the CI logs.

---

## Contributing / forking

This repo is meant to be forked and extended. Add new exercises by:
1. Creating a new branch from `main`: `git checkout -b exercise/05-<your-slug>`
2. Mutating the code to introduce the issue + the failing visible test
3. Adding `EXERCISE.md` and `CLAUDE_INSTRUCTIONS.md`
4. Adding a hidden test set under `grading/exercise-05-<your-slug>/`
5. Updating the table at the top of this README
