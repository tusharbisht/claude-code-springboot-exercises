package com.learning.taskmanager.grading;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Hidden grading suite for meta/02-hooks-and-commands.
 *
 * Goes beyond "the artifact files exist" (covered by the visible tests):
 * verifies the artifacts actually meet the *quality* bar a real cohort
 * mate would expect. Slash commands need clear instructions, hooks need
 * to actually inspect their payload, etc.
 */
@DisplayName("[grading] meta/02 — hooks and slash commands quality")
class HiddenArtifactQualityGradingTest {

    @Test
    @DisplayName("/find-controller delegates to a subagent (Explore) — not blind grep")
    void findControllerUsesSubagent() throws IOException {
        String content = Files.readString(Path.of(".claude/commands/find-controller.md"));
        assertThat(content.toLowerCase())
                .as("the /find-controller command should explicitly mention the Explore "
                        + "subagent — that's the right tool for codebase navigation in this repo")
                .contains("explore");
    }

    @Test
    @DisplayName("/test-changed runs the right thing (mvn -Dtest=... or mvn -pl)")
    void testChangedRunsTargetedTests() throws IOException {
        String content = Files.readString(Path.of(".claude/commands/test-changed.md"));
        assertThat(content)
                .as("/test-changed should not just `mvn test` (that's the whole suite). "
                        + "It should target tests by name (`-Dtest=`) or module (`-pl`)")
                .containsAnyOf("-Dtest=", "-pl ");
    }

    @Test
    @DisplayName("guard-pom.sh actually inspects the file_path field")
    void guardPomInspectsFilePath() throws IOException {
        String script = Files.readString(Path.of(".claude/scripts/guard-pom.sh"));
        assertThat(script)
                .as("the script should inspect tool_input.file_path from the hook payload "
                        + "(via jq or by reading stdin). A script that runs unconditionally is "
                        + "annoying noise, not a guard.")
                .containsAnyOf("file_path", "tool_input");
        assertThat(script)
                .as("the script should mention pom.xml — that's the file it's guarding")
                .contains("pom.xml");
    }

    @Test
    @DisplayName("settings.json references guard-pom.sh from a PreToolUse Edit/Write hook")
    void settingsWiresUpTheHook() throws IOException {
        String settings = Files.readString(Path.of(".claude/settings.json"));
        assertThat(settings)
                .as("the settings file should declare a PreToolUse hook (so it fires BEFORE the "
                        + "edit), with a matcher that targets Edit/Write/MultiEdit")
                .contains("PreToolUse");
        assertThat(settings)
                .as("the matcher should target file-modifying tools")
                .containsAnyOf("Edit|Write", "Write|Edit", "MultiEdit");
    }
}
