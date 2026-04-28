# No hidden grading for exercise 08

Exercise 08 is the anti-exercise: the entire point is that the visible failing tests are enough signal to fix the code in 30 seconds without launching Claude. Adding a hidden grading layer would undercut that lesson.

The visible `mvn test` result is the source of truth here. If it's green, you're done.
