package com.learning.taskmanager.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Visible test for meta/06-prompting-tactics.
 *
 * Enforces the SPEC.md deliverable. The spec is the artifact of the
 * spec-first tactic — checking it exists with the right sections is a
 * weak proxy for "you actually wrote a spec," but it's a forcing function.
 */
@DisplayName("[meta-06] SPEC.md is the spec-first deliverable")
class SpecArtifactTest {

    private static final Path SPEC = Path.of("SPEC.md");

    @Test
    @DisplayName("SPEC.md exists at the repo root")
    void specExists() {
        assertThat(SPEC)
                .as("Write SPEC.md at the repo root *before* writing any code. The spec must "
                        + "answer the 'open questions' from FEATURE_REQUEST.md so the implementation "
                        + "is mechanical once you start.")
                .exists();
    }

    @Test
    @DisplayName("SPEC.md has the required sections")
    void specHasSections() throws IOException {
        if (!Files.exists(SPEC)) return;
        String content = Files.readString(SPEC).toLowerCase();
        assertThat(content).as("SPEC.md should have an 'Overview' section").contains("overview");
        assertThat(content).as("SPEC.md should have an 'Endpoints' or 'API' section").containsAnyOf("endpoints", "## api", "## rest");
        assertThat(content).as("SPEC.md should have a 'Data model' or 'Schema' section").containsAnyOf("data model", "schema");
        assertThat(content).as("SPEC.md should have a 'Constraints' or 'Non-goals' section").containsAnyOf("constraints", "non-goal", "out of scope");
        assertThat(content)
                .as("SPEC.md should resolve the FEATURE_REQUEST.md open questions — at minimum "
                        + "discuss diff-vs-snapshot, deletion semantics, and pagination")
                .containsAnyOf("diff", "snapshot", "delete", "pagination");
    }
}
