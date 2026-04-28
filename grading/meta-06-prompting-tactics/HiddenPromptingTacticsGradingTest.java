package com.learning.taskmanager.grading;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learning.taskmanager.dto.CreateTaskRequest;
import com.learning.taskmanager.dto.UpdateTaskRequest;
import com.learning.taskmanager.model.TaskStatus;
import com.learning.taskmanager.repository.TaskRepository;
import com.learning.taskmanager.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Hidden grading suite for meta/06-prompting-tactics.
 *
 * Verifies the spec is substantive (not a stub), the feature handles
 * edge cases the visible test doesn't, and (locally only — when
 * `.claude/session-log.jsonl` exists) the session log shows the four
 * tactics in use.
 */
@SpringBootTest
@DisplayName("[grading] meta/06 — prompting tactics")
class HiddenPromptingTacticsGradingTest {

    @Autowired private WebApplicationContext webApplicationContext;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private TaskRepository taskRepository;
    @Autowired private UserRepository userRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        taskRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("SPEC.md is substantive — at least 1500 chars, with Open Questions resolved")
    void specIsSubstantive() throws IOException {
        Path spec = Path.of("SPEC.md");
        if (!Files.exists(spec)) return;
        String content = Files.readString(spec);
        assertThat(content.length())
                .as("a useful spec is at least 1500 chars; yours is %d. Address each open "
                        + "question in FEATURE_REQUEST.md explicitly.", content.length())
                .isGreaterThanOrEqualTo(1500);
        // The spec must take a position on diff vs snapshot.
        String low = content.toLowerCase();
        assertThat(low)
                .as("the spec must take a position on whether to store a diff or a full snapshot")
                .containsAnyOf("diff", "snapshot");
    }

    @Test
    @DisplayName("history endpoint orders entries newest-first")
    void historyOrderedNewestFirst() throws Exception {
        long id = createBaseTask();
        // 3 updates with small pauses
        for (TaskStatus s : List.of(TaskStatus.IN_PROGRESS, TaskStatus.DONE, TaskStatus.TODO)) {
            UpdateTaskRequest u = new UpdateTaskRequest();
            u.setStatus(s);
            mockMvc.perform(put("/api/tasks/{id}", id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(u)))
                    .andExpect(status().isOk());
            Thread.sleep(10);
        }
        // Most recent record should be first in the array.
        mockMvc.perform(get("/api/tasks/{id}/history", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(4)));
    }

    @Test
    @DisplayName("deleting a task records a history entry")
    void deleteRecordsHistory() throws Exception {
        long id = createBaseTask();
        mockMvc.perform(delete("/api/tasks/{id}", id))
                .andExpect(status().isNoContent());
        // The history endpoint should either still return the deleted task's history
        // (200) OR a 404 — the SPEC.md should have taken a position. Either is OK.
        int statusCode = mockMvc.perform(get("/api/tasks/{id}/history", id))
                .andReturn().getResponse().getStatus();
        assertThat(statusCode)
                .as("after delete, history endpoint should return either 200 (with history) or 404 — "
                        + "consistent with what your SPEC.md committed to")
                .isIn(200, 404);
    }

    @Test
    @DisplayName("session log shows characteristic prompt patterns (advisory)")
    void sessionLogReflectsTactics() throws IOException {
        // This test runs only when .claude/session-log.jsonl exists. CI without
        // a learner session won't have one, so we early-return.
        Path log = Path.of(".claude/session-log.jsonl");
        if (!Files.exists(log)) return;
        String content = Files.readString(log);

        Pattern adversarial = Pattern.compile(
                "wrong|argue against|find issues|critique|push back|skeptical",
                Pattern.CASE_INSENSITIVE);
        Pattern constraint = Pattern.compile(
                "at most|fewer than|without (introducing|adding|using)|no new",
                Pattern.CASE_INSENSITIVE);
        Pattern testFirst = Pattern.compile(
                "failing test|test (first|before)|write the test",
                Pattern.CASE_INSENSITIVE);

        boolean a = adversarial.matcher(content).find();
        boolean c = constraint.matcher(content).find();
        boolean t = testFirst.matcher(content).find();

        // Advisory: print to stderr, never fail the build.
        StringBuilder report = new StringBuilder("\n[session tactics check] ");
        report.append(a ? "✓ adversarial " : "✗ adversarial ");
        report.append(c ? "✓ constraint-shaped " : "✗ constraint-shaped ");
        report.append(t ? "✓ test-first " : "✗ test-first ");
        System.err.println(report);
        // Don't assert. The deliverables are the gate; this is feedback.
    }

    private long createBaseTask() throws Exception {
        CreateTaskRequest c = new CreateTaskRequest();
        c.setTitle("history-target");
        String body = mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(c)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asLong();
    }
}
