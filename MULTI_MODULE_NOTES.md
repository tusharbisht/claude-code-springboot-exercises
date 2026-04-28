# Multi-module preview

This branch (`extra/multi-module-preview`) is the same Task Manager app as `main`, but split into three Maven modules. There's nothing broken, no exercise to do — it exists so you can practice navigating a multi-module Spring Boot codebase with Claude Code.

## Module layout

```
.
├── pom.xml                                # parent (packaging=pom) — lists modules, manages versions
├── domain/                                # entities, enums, DTOs — no Spring code
│   ├── pom.xml
│   └── src/main/java/com/learning/taskmanager/{model,dto}/
├── persistence/                           # Spring Data JPA repositories — depends on domain
│   ├── pom.xml
│   └── src/main/java/com/learning/taskmanager/repository/
└── web/                                   # controllers, services, exception handler, app entrypoint
    ├── pom.xml
    └── src/main/
        ├── java/com/learning/taskmanager/{TaskManagerApplication.java,controller,service,exception}/
        └── resources/application.properties
    └── src/test/                          # all integration tests live here
```

## Why this matters for Claude Code

Most real-world Spring Boot codebases have multiple modules. Once a project crosses ~5k lines, navigation becomes the dominant cost — *finding* the right file is harder than editing it. Single-module projects don't teach this.

Two specific things change:

1. **`grep` and `Read` cost more.** A blind `grep` across the repo searches every module. Pointing Claude at the *right* module first is a real saving.
2. **The Explore subagent earns its keep.** Cross-module questions ("where does TaskService get its TaskRepository from? Which module is each in?") are exactly what it's for.

## Useful prompts to try on this branch

- > "Use the Explore agent to map every cross-module reference. For each `import com.learning.taskmanager.*` statement, report the file, the imported class, and which module it belongs to."
- > "I want to add a `findByDueDateBefore` method to `TaskRepository`. Which module does that live in, and what dependency relationship does it create?"
- > "Generate a dependency graph of the three modules from the poms (no need to actually run mvn). Format as ASCII boxes and arrows."
- > "Run `mvn -pl persistence -am test` and explain what `-pl` and `-am` did."

## Things that are *different* from `main`

- **Build commands work the same** at the top level — `mvn test` and `mvn spring-boot:run` still do what you expect; Maven's reactor handles the modules.
- **Running a single module** uses `-pl <module> -am`: `mvn -pl web -am test` builds and tests `web` plus what it depends on.
- **The Spring Boot fat jar** lives at `web/target/task-manager-web-*.jar` (was `target/task-manager-*.jar`).
- **Spring component scanning** still finds beans in all modules because `TaskManagerApplication` lives in `com.learning.taskmanager` and scanning walks downward — module boundaries don't affect classpath scanning.
- **There is no grading suite on this branch.** It's a navigation playground, not an exercise.

## Things that are *not* different

- **Java packages.** Same names as `main` — `com.learning.taskmanager.{model, dto, controller, service, repository, exception}`. This was a deliberate choice: it makes the module split feel realistic (multiple modules sharing a top-level package is normal in Java) without forcing you to relearn import paths.
- **Tests.** Same 19 tests pass.
- **Conventions.** Constructor injection, JPQL queries, no Lombok — all the same.

## What to look for as you explore

- The parent pom (`./pom.xml`) declares modules and centralizes version management
- Each module pom is a few dozen lines — Maven inheritance keeps them concise
- Inter-module dependencies are explicit (`web` -> `persistence` -> `domain`)
- The `domain` module pulls in *only* the JPA and Validation API jars, not Spring or Hibernate — keeping it lightweight
- The `domain` and `persistence` poms `<skip>true</skip>` on the Spring Boot plugin so they don't try to repackage as fat jars
