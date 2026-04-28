# Grading framework

This directory contains the **hidden grading tests** for each exercise branch. Learners don't see these tests directly — they see only the basic `src/test/java/...` tests on their branch. The grading suite runs:

- in CI (`.github/workflows/grade.yml`) on every push / pull request
- locally via `./grading/run-grading.sh <branch-name>` if a learner wants the harder check

## Layout

```
grading/
├── run-grading.sh                                 # entrypoint
├── scripts/
│   ├── stage-tests.sh                             # copies grading tests into src/test/java
│   ├── unstage-tests.sh                           # cleans up
│   └── parse-surefire.sh                          # extracts pass/fail counts to JSON
├── exercise-01-fix-validation-bug/
│   └── HiddenValidationGradingTest.java
├── exercise-02-implement-search/
│   └── HiddenSearchGradingTest.java
├── exercise-03-optimize-n-plus-one/
│   └── HiddenQueryCountGradingTest.java
└── exercise-04-refactor-fat-controller/
    └── HiddenRefactorGradingTest.java
```

## How it works

1. The runner detects the current branch (or takes one as an argument).
2. `stage-tests.sh` copies the matching `grading/<exercise-slug>/*.java` files into `src/test/java/com/learning/taskmanager/grading/`.
3. `mvn test` runs the **combined** suite (visible + hidden).
4. `parse-surefire.sh` reads `target/surefire-reports/*.xml` and emits `grading-result.json`.
5. `unstage-tests.sh` removes the staged files so the working tree is clean again.

## Why hidden?

The visible tests on each branch tell the learner **what shape the answer should have**. The hidden tests verify **edge cases and architectural quality** that are easy to forget once the surface tests pass — null handling, query counts, structural separation of concerns. This mirrors how production code review works: passing your own tests is necessary but not sufficient.

## Adding a new exercise

1. Add `grading/exercise-NN-<slug>/HiddenXxxGradingTest.java` files. They must compile against `main`.
2. Add a switch case in `run-grading.sh` mapping the branch name to the directory.
3. Update `.github/workflows/grade.yml` if the exercise needs special steps.
