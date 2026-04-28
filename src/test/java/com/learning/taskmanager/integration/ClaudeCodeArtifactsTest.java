package com.learning.taskmanager.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Visible tests for meta/02-hooks-and-commands.
 *
 * The exercise ships no failing Java tests — the deliverable is the
 * Claude Code configuration itself. These tests verify the configuration
 * is in place. Each test's failure message tells you what to add.
 */
@DisplayName("[meta-02] Claude Code artifacts that streamline this codebase")
class ClaudeCodeArtifactsTest {

    @Test
    @DisplayName("/find-controller slash command exists with an argument-hint")
    void findControllerSlashCommand() throws IOException {
        Path cmd = Path.of(".claude/commands/find-controller.md");
        assertThat(cmd)
                .as("Add a slash command at .claude/commands/find-controller.md that, given "
                        + "a URL path or HTTP verb+path, finds the controller method that handles it. "
                        + "Use the Explore subagent. See EXERCISE.md for details.")
                .exists();
        String content = Files.readString(cmd);
        assertThat(content)
                .as("the slash command file should declare an `argument-hint` so the user "
                        + "knows what to type after `/find-controller`")
                .contains("argument-hint:");
        assertThat(content)
                .as("the slash command should reference Spring's @RequestMapping/@GetMapping/etc.")
                .containsAnyOf("RequestMapping", "GetMapping", "PostMapping");
    }

    @Test
    @DisplayName("/test-changed slash command exists")
    void testChangedSlashCommand() throws IOException {
        Path cmd = Path.of(".claude/commands/test-changed.md");
        assertThat(cmd)
                .as("Add a slash command at .claude/commands/test-changed.md that runs tests "
                        + "corresponding to the source files modified since `main`. See EXERCISE.md.")
                .exists();
        String content = Files.readString(cmd);
        assertThat(content)
                .as("the slash command should reference `git diff` (to discover changed files) "
                        + "and `mvn` (to run the targeted tests)")
                .contains("git diff");
        assertThat(content)
                .as("the slash command should reference `mvn` (to actually run the tests)")
                .contains("mvn");
    }

    @Test
    @DisplayName("a project-level settings.json defines a hook that guards pom.xml edits")
    void pomEditHook() throws IOException {
        Path settings = Path.of(".claude/settings.json");
        assertThat(settings).exists();
        String content = Files.readString(settings);
        assertThat(content)
                .as("Add a hook in .claude/settings.json that warns or guards when Claude is "
                        + "about to Edit/Write pom.xml. Dependency changes deserve attention. "
                        + "Hint: PreToolUse with matcher 'Edit|Write|MultiEdit', and a script "
                        + "that inspects tool_input.file_path for 'pom.xml'.")
                .containsAnyOf("PreToolUse", "guard-pom", "pom.xml");
    }

    @Test
    @DisplayName("the pom-guard hook script exists and is referenced from settings.json")
    void pomGuardScriptExists() throws IOException {
        Path script = Path.of(".claude/scripts/guard-pom.sh");
        assertThat(script)
                .as("Add a script at .claude/scripts/guard-pom.sh that the PreToolUse hook calls. "
                        + "It should read the hook payload from stdin (JSON), check whether "
                        + "tool_input.file_path is `pom.xml`, and print a warning to stderr if so. "
                        + "Exiting non-zero blocks the edit; exiting 0 allows it.")
                .exists();
        Path settings = Path.of(".claude/settings.json");
        if (Files.exists(settings)) {
            assertThat(Files.readString(settings))
                    .as(".claude/settings.json should reference the guard-pom.sh script in a hook")
                    .contains("guard-pom.sh");
        }
    }
}
