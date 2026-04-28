# Project context for Claude Code

This file is loaded automatically when Claude Code starts in this repo. It tells Claude how the project is laid out, what conventions to respect, and how to verify work. Keep it short — Claude reads it on every session.

## What this repo is

A small Spring Boot REST task manager used as a teaching playground for Claude Code on Java. `main` is the reference (clean, tests green). Each `exercise/*` branch mutates the code to introduce one teaching scenario; each one ships an `EXERCISE.md` (problem) and `CLAUDE_INSTRUCTIONS.md` (recommended workflow). Hidden grading tests live in `grading/` and run in CI.

## Stack

- Java 21, Spring Boot 3.3.5, Maven (**multi-module — domain / persistence / web**)
- Spring Web + Spring Data JPA + Spring Validation
- H2 in-memory database for runtime and tests
- JUnit 5 + Spring's `MockMvc` for tests

## Source layout

This branch is split into three Maven modules. See `MULTI_MODULE_NOTES.md` for the why and how.

```
.
├── pom.xml                        # parent (packaging=pom)
├── domain/src/main/java/com/learning/taskmanager/
│   ├── model/                     # JPA entities + enums
│   └── dto/                       # request/response DTOs
├── persistence/src/main/java/com/learning/taskmanager/
│   └── repository/                # Spring Data JPA interfaces
└── web/src/
    ├── main/java/com/learning/taskmanager/
    │   ├── TaskManagerApplication.java    # Spring Boot entrypoint
    │   ├── controller/            # REST endpoints — thin, delegate to services
    │   ├── service/               # business logic + @Transactional boundaries
    │   └── exception/             # custom exceptions + GlobalExceptionHandler
    ├── main/resources/application.properties
    └── test/java/com/learning/taskmanager/
        ├── integration/           # @SpringBootTest + MockMvc tests
        └── TaskManagerApplicationTests.java
```

## Conventions to follow when editing

- **Constructor injection only.** No `@Autowired` on fields. Services and controllers take their dependencies through their single constructor.
- **Validation lives at the HTTP boundary.** `@Valid` on `@RequestBody` parameters; `@NotBlank` / `@Size` etc. on the DTO. Don't validate in the service.
- **Controllers are thin.** Each endpoint method is a one- or two-liner that delegates to the service. No `@Transactional` in controllers.
- **`@Transactional` lives on services.** Read-only methods get `@Transactional(readOnly = true)`.
- **Exceptions go through `GlobalExceptionHandler`.** Don't catch-and-rethrow in controllers. Return domain exceptions (`ResourceNotFoundException`, `DuplicateResourceException`) from services.
- **DTOs are immutable records when they carry data outward.** Mutable classes only for inbound `@RequestBody` types (Jackson needs setters or a creator).
- **No Lombok.** Boilerplate is intentional — these exercises are read by learners.
- **No `Specification` / `QueryDSL`.** Prefer JPQL `@Query` with nullable parameters for filtering.

## How to verify changes

- **Visible tests:** `mvn test` — should be green on `main` and on a correctly solved exercise branch.
- **Hidden grading suite:** `./grading/run-grading.sh` (auto-detects branch). Stages tests from `grading/<exercise-slug>/`, runs them, emits `grading-result.json`.
- **Run the app:** `mvn spring-boot:run` → `http://localhost:8080/api/tasks`. H2 console at `/h2-console`, JDBC URL `jdbc:h2:mem:taskdb`, user `sa`, empty password.

## Tooling already wired up

- `.claude/settings.json` registers a `Stop` hook that runs `mvn test` after each Claude Code session and writes `.claude/last-progress.json`. If `EVAL_WEBHOOK_URL` is set, results POST there.
- `.github/workflows/grade.yml` runs visible + hidden tests on every push to `exercise/*`, comments on PRs, uploads a JSON artifact, and POSTs to `EVAL_WEBHOOK_URL` if set as a repo secret.

## Things to avoid

- Don't commit changes to `EXERCISE.md` or `CLAUDE_INSTRUCTIONS.md` on an exercise branch — those are the teaching material, not the work product.
- Don't add new dependencies to `pom.xml` unless the exercise explicitly calls for it.
- Don't introduce caches, retries, or async machinery to "fix" performance issues — the right fix is usually a single query or a structural change.
- Don't bypass the test layer to "prove" something works — `mvn test` is the source of truth.

## Useful one-liners

```bash
mvn -Dtest=ClassName test                          # run a single test class (across modules)
mvn -pl web -am test                               # build/test the `web` module + its deps only
mvn -pl domain test                                # test just the domain module
mvn dependency:tree | grep -i <artifact>           # find what pulls in a dep
./grading/run-grading.sh exercise/02-implement-search  # run hidden grading explicitly
```
