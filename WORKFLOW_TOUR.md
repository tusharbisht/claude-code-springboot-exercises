# Claude Code workflow tour

This branch is **not an exercise** — there's nothing broken to fix. It's a guided walkthrough of the Claude Code features and habits that pay off most on Java codebases. Work through it once before you tackle the exercise branches.

You'll spend ~30 minutes here. By the end you'll have:
- A mental model of when to use which Claude Code mode
- A custom slash command saved in this repo that you'll actually use
- A working sense of what Claude Code does well, where it struggles, and how to push back

> **Pre-req:** make sure `mvn test` is green on this branch first.
> ```bash
> git checkout tour/workflow
> mvn test     # 19 green
> claude       # launch Claude Code in this dir
> ```

---

## Stop 1 — Let Claude read the project context (2 min)

`CLAUDE.md` is loaded automatically on every session. Verify Claude actually used it:

> **Try this prompt:**
> "Without reading any files, what does this repo do, what's the stack, and what conventions do I expect you to follow when editing? Cite specific lines from CLAUDE.md."

What you're looking for: Claude paraphrases the conventions (constructor injection, thin controllers, JPQL over Specifications, no Lombok). If it makes generic Spring Boot assertions instead, your CLAUDE.md isn't being loaded — check that you launched Claude from the repo root.

**Why this matters:** every prompt that *doesn't* re-explain conventions is shorter, faster, and less likely to drift. CLAUDE.md is the cheapest lever you have.

---

## Stop 2 — Plan mode vs. action mode (5 min)

Plan mode (toggle with `Shift+Tab`) makes Claude write a plan instead of editing files. It's the right mode for any change that touches more than one file.

Toggle plan mode and try:

> "Add a `GET /api/tasks/count` endpoint returning the total number of tasks. Plan the changes — don't write code yet."

A good plan calls out: a service method, a controller method, a new test, and where to add each. A bad plan jumps straight into writing the JPA query.

Now exit plan mode (`Shift+Tab` again) and tell Claude:

> "Reject that plan — I just want one method on the controller calling the existing `taskRepository.count()`. No service change. Replan."

Watch how Claude revises. Pushing back on a plan is **the most underused move** in Claude Code. The first plan is almost never the smallest plan.

When you're happy, tell Claude to apply it. Run `mvn test` and `mvn spring-boot:run`, hit the new endpoint with `curl localhost:8080/api/tasks/count`, then `git checkout .` to throw the change away — this is a tour, not a real change.

**Rule of thumb:** plan mode for anything > 1 file. Action mode for typo-fix-grade changes.

---

## Stop 3 — The Explore subagent (5 min)

For "where in the codebase does X happen?" questions, the **Explore subagent** is much faster than running greps yourself. It runs in its own context window so it doesn't bloat your main session.

Try:

> "Use the Explore agent to find every place where `@Transactional` is used in this repo. Report which class and method, and whether it's read-only or read-write. Don't read those files yourself — delegate it."

What good looks like: a compact table of locations. What you should compare to: the same question without the agent — typically returns more chatter and fewer files inspected because each `Read` tool call costs context.

**Rule of thumb:** any question that starts with "where" or "which files" → Explore agent.

---

## Stop 4 — Reading large output (5 min)

Java is verbose. Stack traces, Spring startup logs, and Hibernate SQL streams are signal buried in noise. Claude is great at compressing them — but only if you actually feed them in.

Try:

> "Run `mvn spring-boot:run` in the background, tail the output for 5 seconds, kill it, and tell me three things: (1) which beans took longest to initialize, (2) whether any auto-configuration was skipped, (3) the JDBC URL it picked up."

This trains a reflex: when you see a wall of log output, your first move is to ask Claude to summarize it, not to scroll past it.

For Hibernate SQL specifically, set `spring.jpa.show-sql=true` in `src/main/resources/application.properties` temporarily, run a test, and ask:

> "Show me the SQL Hibernate emitted during `UserApiIntegrationTest.listUsers_returnsTaskCounts`. How many statements, and what's the worst-case query in there?"

Then revert the property change.

---

## Stop 5 — Write a custom slash command (10 min)

Slash commands are project-local prompts you save once and reuse. Add one now.

Create `.claude/commands/find-failures.md`:

```markdown
---
description: Run `mvn test` and explain failures concisely
---

Run `mvn -B test` from the project root. Then for each failing test:
1. Print the test class and method
2. Quote the assertion error or exception (one line max)
3. Suggest the most likely root cause in 1 sentence

Do not edit any files. Do not run any commands other than `mvn test`.
End with a one-sentence summary: how many fail, how related are they, what's the top suspect file.
```

Save the file. In the Claude Code session, run `/find-failures`. You should see a clean test summary instead of raw Maven output.

**Variations to try later:**
- `/explain-bean` → describes how a Spring bean is wired and where it's used
- `/migrate-test` → upgrades a test from old assertion style to AssertJ
- `/sql-trace` → reads recent Hibernate SQL output and explains it

Slash commands are the highest-leverage Claude Code customization. **Make one a week.**

---

## Stop 6 — Permissions and hooks (3 min)

Look at `.claude/settings.json` in this repo. There's already a `Stop` hook that runs `mvn test` after every Claude session and writes results to `.claude/last-progress.json`. Try it:

```bash
# inside a Claude session, ask anything trivial then exit
> "What's 2+2?"
> exit

# back in your shell
cat .claude/last-progress.json
```

Now imagine running this in a class. Setting `EVAL_WEBHOOK_URL` (in your shell, before launching Claude) makes the hook POST to a server — see [`evaluation-server/`](evaluation-server/) for a reference receiver.

**Permissions:** if Claude keeps prompting you to approve `mvn test`, you can pre-approve it in `.claude/settings.local.json`:

```json
{
  "permissions": {
    "allow": ["Bash(mvn test:*)", "Bash(mvn -Dtest=*:*)"]
  }
}
```

`settings.local.json` is gitignored — your personal preferences don't end up in the repo.

---

## Stop 7 — IntelliJ integration (2 min, only if you use IntelliJ)

If you use IntelliJ for Java work (most do), install the **Claude Code** plugin from the marketplace. With it, `Cmd+L` opens Claude Code attached to your current file. Selection-based context ("explain this method", "write a test for this class") becomes the default workflow instead of typing file paths.

Skip this stop if you're a terminal-and-vim person.

---

## Stop 8 — Calibrate where Claude *isn't* the answer (3 min)

This is the most important stop. Run `git diff main`. The whole tour added one file (`WORKFLOW_TOUR.md`) and one slash command (`.claude/commands/find-failures.md`).

Now ask yourself: of the five things you did,
- Stop 5 (slash command) — Claude paid for itself
- Stop 3 (Explore agent search) — borderline; a single `grep -r '@Transactional'` would have done the job in 1 second
- Stop 1 (read CLAUDE.md) — Claude paid for itself
- Stop 2 (plan + reject + replan) — paid for itself; the rejection step is what bought you the small change
- Stop 4 (summarize logs) — paid for itself

The recurring trap on Java: small, mechanical edits where the prompt overhead is bigger than the work. **Typo fixes, single-line changes, files under 30 lines** — just edit them. Build the instinct to ask "is the prompt itself going to take longer than doing this?"

---

## Cheat sheet to keep open

| Situation | Claude Code move |
| --- | --- |
| > 1 file change | Plan mode first |
| "Where in the codebase…" | Explore subagent |
| Wall of log output | Ask Claude to summarize, don't scroll |
| Same prompt third time this week | Save it as a slash command |
| Failing tests | `/find-failures` (the one you just made) |
| Trivial fix < 30s | Don't open Claude — just edit |
| Refactor across many files | Plan mode + one commit per step |
| Migration | Plan mode + read the framework's release notes first |

---

## Going further

- The other branches (`exercise/01` through `exercise/07`, `exercise/08-when-not-to-use-claude`) practice these moves in concrete situations.
- `extra/multi-module-preview` shows how the same project looks split into Maven modules — the Explore agent and CLAUDE.md become more important on multi-module repos.
- Read [`README.md`](README.md) for the full course map.
