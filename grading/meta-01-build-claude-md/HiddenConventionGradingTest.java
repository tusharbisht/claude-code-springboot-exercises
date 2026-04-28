package com.learning.taskmanager.grading;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Hidden grading suite for meta/01-build-claude-md.
 *
 * The visible test verifies the by-priority feature works. These tests
 * verify the implementation respected the codebase's conventions —
 * conventions Claude only reliably follows when CLAUDE.md states them.
 *
 * Each failing test points the learner at a specific CLAUDE.md addition
 * they're missing.
 */
@DisplayName("[grading] meta/01 — build CLAUDE.md by working through these")
class HiddenConventionGradingTest {

    private static final Path SRC_MAIN = Path.of("src/main/java");

    private static List<String> sourceLines() throws IOException {
        try (Stream<Path> walk = Files.walk(SRC_MAIN)) {
            return walk.filter(p -> p.toString().endsWith(".java"))
                    .flatMap(p -> {
                        try {
                            return Files.lines(p).map(line -> p + ":" + line);
                        } catch (IOException e) {
                            return Stream.empty();
                        }
                    })
                    .toList();
        }
    }

    private static List<String> grep(Pattern pat) throws IOException {
        return sourceLines().stream().filter(l -> pat.matcher(l).find()).toList();
    }

    @Test
    @DisplayName("CLAUDE.md exists at the project root")
    void claudeMdExists() {
        assertThat(Path.of("CLAUDE.md"))
                .as("Add a CLAUDE.md file at the repo root. Without it, every "
                        + "Claude session has to rediscover the conventions of this codebase.")
                .exists();
    }

    @Test
    @DisplayName("constructor injection only — no @Autowired on fields")
    void noFieldAutowired() throws IOException {
        // Match @Autowired NOT on a constructor (fields/setters). Heuristic:
        // a line containing @Autowired that is NOT followed by 'public ' on the same line
        // and the next line doesn't start with the constructor signature.
        Pattern p = Pattern.compile("@Autowired");
        List<String> hits = grep(p);
        assertThat(hits)
                .as("This codebase uses constructor injection only — never @Autowired fields. "
                        + "Add a 'Constructor injection only' bullet to CLAUDE.md so Claude "
                        + "stops reaching for it.")
                .isEmpty();
    }

    @Test
    @DisplayName("no Lombok dependency or imports")
    void noLombok() throws IOException {
        Pattern p = Pattern.compile("import\\s+lombok\\.|@(Data|Getter|Setter|Builder|AllArgsConstructor|NoArgsConstructor|RequiredArgsConstructor|Slf4j)\\b");
        List<String> hits = grep(p);
        assertThat(hits)
                .as("Lombok is not used in this codebase — boilerplate is intentional. "
                        + "Add 'no Lombok' to CLAUDE.md so Claude doesn't introduce it on every refactor.")
                .isEmpty();
    }

    @Test
    @DisplayName("no Spring Data Specifications API")
    void noSpecificationsApi() throws IOException {
        Pattern p = Pattern.compile("Specification\\s*<|JpaSpecificationExecutor");
        List<String> hits = grep(p);
        assertThat(hits)
                .as("This codebase prefers JPQL @Query with nullable parameters over the "
                        + "Specifications API. Add a 'JPQL only, no Specifications' bullet "
                        + "to CLAUDE.md.")
                .isEmpty();
    }

    @Test
    @DisplayName("@Transactional lives on services, not controllers")
    void noTransactionalInControllers() throws IOException {
        Pattern p = Pattern.compile("(?s)@Transactional[^;]*\\bclass\\s+\\w+Controller|@Transactional\\s*(\\([^)]*\\))?\\s*public\\s+[^;]*\\bController");
        // Simpler: find any controller file with @Transactional anywhere.
        try (Stream<Path> walk = Files.walk(SRC_MAIN)) {
            List<Path> bad = walk.filter(pp -> pp.toString().endsWith("Controller.java"))
                    .filter(pp -> {
                        try {
                            return Files.readString(pp).contains("@Transactional");
                        } catch (IOException e) {
                            return false;
                        }
                    }).toList();
            assertThat(bad)
                    .as("@Transactional belongs on services, not controllers. "
                            + "Add a 'Transactions live on services' bullet to CLAUDE.md.")
                    .isEmpty();
        }
    }

    @Test
    @DisplayName("controllers don't depend on repositories directly")
    void controllersDontImportRepositories() throws IOException {
        try (Stream<Path> walk = Files.walk(SRC_MAIN)) {
            List<Path> bad = walk.filter(pp -> pp.toString().endsWith("Controller.java"))
                    .filter(pp -> {
                        try {
                            String src = Files.readString(pp);
                            return src.contains(".repository.") || src.contains("Repository ");
                        } catch (IOException e) {
                            return false;
                        }
                    }).toList();
            assertThat(bad)
                    .as("Controllers must depend on services, not repositories. "
                            + "Add a 'Controllers are thin / data-access lives in services' "
                            + "bullet to CLAUDE.md.")
                    .isEmpty();
        }
    }

    @Test
    @DisplayName("CLAUDE.md mentions all four conventions above")
    void claudeMdCoversTheConventions() throws IOException {
        Path md = Path.of("CLAUDE.md");
        if (!Files.exists(md)) {
            // The earlier test will have already failed — this one is a no-op when CLAUDE.md is missing.
            return;
        }
        String content = Files.readString(md).toLowerCase();
        assertThat(content)
                .as("CLAUDE.md should mention 'constructor injection'")
                .containsAnyOf("constructor injection", "no @autowired", "no autowired field");
        assertThat(content)
                .as("CLAUDE.md should mention 'no lombok'")
                .containsAnyOf("no lombok", "without lombok", "lombok is not used");
        assertThat(content)
                .as("CLAUDE.md should mention 'no specifications'")
                .containsAnyOf("specification", "jpql");
        assertThat(content)
                .as("CLAUDE.md should mention 'transactions on services'")
                .containsAnyOf("transactional", "transaction boundar");
    }
}
