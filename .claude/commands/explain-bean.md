---
description: Trace how a Spring bean is wired and where it's used
argument-hint: <ClassName>
---

For the Spring bean named `$ARGUMENTS`:
1. Find its declaration (`@Service`, `@Component`, `@RestController`, `@Repository`, `@Configuration`, or `@Bean` method) — report the file path and line
2. List its constructor parameters (its dependencies)
3. List every other class in this repo that depends on it (constructor-injects it or `@Autowired`s it)
4. State its transactional scope (any `@Transactional` annotations on its methods)

Use the Explore agent to keep this fast. Don't edit files.
