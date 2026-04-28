package com.learning.taskmanager.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Visible test for meta/03-custom-subagent.
 *
 * The Java integration alone could be solved without ever building the
 * agent, but you'd burn far more context on the way there. The point of
 * this exercise is to *build the navigation tool*, not to solve the task
 * the fastest possible way.
 *
 * This test enforces that the agent file exists with the structure a
 * custom Claude Code subagent needs.
 */
@DisplayName("[meta-03] custom subagent for the legacy package")
class LegacyNavigatorAgentTest {

    private static final Path AGENT = Path.of(".claude/agents/legacy-navigator.md");

    @Test
    @DisplayName("subagent file .claude/agents/legacy-navigator.md exists")
    void agentFileExists() {
        assertThat(AGENT)
                .as("Add a custom subagent at .claude/agents/legacy-navigator.md that "
                        + "encapsulates how to navigate the legacy/ package. See EXERCISE.md "
                        + "for the conventions it needs to know about.")
                .exists();
    }

    @Test
    @DisplayName("subagent has YAML frontmatter with `name` and `description`")
    void agentFrontmatter() throws IOException {
        if (!Files.exists(AGENT)) return;
        String content = Files.readString(AGENT);
        assertThat(content)
                .as("the agent file must start with `---` YAML frontmatter")
                .startsWith("---");
        assertThat(content)
                .as("the frontmatter should declare `name:` (the agent's identifier)")
                .contains("name:");
        assertThat(content)
                .as("the frontmatter should declare `description:` (when Claude should pick this agent)")
                .contains("description:");
    }

    @Test
    @DisplayName("subagent body documents the legacy module's conventions")
    void agentBodyMentionsConventions() throws IOException {
        if (!Files.exists(AGENT)) return;
        String content = Files.readString(AGENT).toLowerCase();
        // The agent should encode the things this exercise's legacy/ package does differently.
        assertThat(content)
                .as("the agent should mention LegacyAuditDao (the canonical write path)")
                .contains("legacyauditdao");
        assertThat(content)
                .as("the agent should mention recordevent (the only safe write API) — "
                        + "and ideally warn against constructing entries directly")
                .contains("recordevent");
        assertThat(content)
                .as("the agent should mention the upper-case kind convention OR EntityManager — "
                        + "those are the two conventions a navigator would warn about")
                .containsAnyOf("upper-case", "uppercase", "entitymanager", "snake_case");
    }
}
