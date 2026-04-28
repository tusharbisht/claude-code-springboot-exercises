---
description: Run `mvn test` and explain failures concisely
---

Run `mvn -B test` from the project root. Then for each failing test:
1. Print the test class and method
2. Quote the assertion error or exception (one line max)
3. Suggest the most likely root cause in 1 sentence

Do not edit any files. Do not run any commands other than `mvn test`.
End with a one-sentence summary: how many tests fail, how related they are, and your top suspect file.
