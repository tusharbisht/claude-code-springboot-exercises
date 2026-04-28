---
description: Explain Hibernate SQL emitted during a recent test run
---

Look at `.claude/last-mvn.log` (written by the Stop hook) or `target/surefire-reports/`. Find the Hibernate SQL statements logged during the most recent `mvn test` run.

Group them by which test class triggered them. For each group, report:
1. Number of statements
2. Whether any look like an N+1 pattern (one SELECT followed by many similar SELECTs differing only by a parameter)
3. The single most expensive-looking query (long FROM clause, many JOINs, or no WHERE)

Do not edit any files. End with: which test would I tune first, and why?
