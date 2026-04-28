package com.learning.taskmanager.grading;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learning.taskmanager.dto.CreateTaskRequest;
import com.learning.taskmanager.legacy.LegacyAuditDao;
import com.learning.taskmanager.legacy.LegacyAuditEntry;
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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Hidden grading suite for meta/03-custom-subagent.
 *
 * The visible test verifies "deleting a task records something." This goes
 * deeper: the integration must use LegacyAuditDao (not bypass it), the
 * kind must be the canonical upper-case form, and the entry must include
 * the actor.
 *
 * A learner who built and used the legacy-navigator subagent should have
 * encoded these conventions in the agent and gotten them right on the
 * first try. A learner who skipped the agent will probably miss one.
 */
@SpringBootTest
@DisplayName("[grading] meta/03 — legacy integration quality")
class HiddenLegacyIntegrationGradingTest {

    @Autowired
    private WebApplicationContext webApplicationContext;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private TaskRepository taskRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private LegacyAuditDao auditDao;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        taskRepository.deleteAll();
        userRepository.deleteAll();
    }

    private long createTaskAndDelete(String title) throws Exception {
        CreateTaskRequest req = new CreateTaskRequest();
        req.setTitle(title);
        String body = mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long id = objectMapper.readTree(body).get("id").asLong();
        mockMvc.perform(delete("/api/tasks/{id}", id))
                .andExpect(status().isNoContent());
        return id;
    }

    @Test
    @DisplayName("audit entry uses canonical kind 'TASK_DELETED' (upper-case)")
    void canonicalKind() throws Exception {
        long id = createTaskAndDelete("kind-check");
        List<LegacyAuditEntry> entries = auditDao.findBySubject(id);
        assertThat(entries)
                .as("expected at least one audit entry written via LegacyAuditDao for the deleted task")
                .isNotEmpty();
        assertThat(entries.get(0).getKind())
                .as("legacy convention: kind is upper-snake_case. recordEvent() upper-cases "
                        + "automatically, but the caller still has to pass 'TASK_DELETED' "
                        + "(or any consistent string) — not 'task_deleted' or 'deleted'")
                .isEqualTo("TASK_DELETED");
    }

    @Test
    @DisplayName("audit entry has subject_id = the deleted task id")
    void subjectIsTaskId() throws Exception {
        long id = createTaskAndDelete("subject-check");
        List<LegacyAuditEntry> entries = auditDao.findBySubject(id);
        assertThat(entries).isNotEmpty();
        assertThat(entries.get(0).getSubjectId()).isEqualTo(id);
    }

    @Test
    @DisplayName("each delete writes EXACTLY ONE audit entry (no duplicates, no zero)")
    void exactlyOneEntryPerDelete() throws Exception {
        long before = auditDao.countByKind("TASK_DELETED");
        createTaskAndDelete("dup-check-1");
        createTaskAndDelete("dup-check-2");
        long after = auditDao.countByKind("TASK_DELETED");
        assertThat(after - before)
                .as("two deletes should produce exactly two audit entries")
                .isEqualTo(2);
    }

    @Test
    @DisplayName("integration goes THROUGH LegacyAuditDao, not directly to the entity")
    void usesLegacyAuditDao() throws Exception {
        // The DAO's recordEvent() rounds occurredAtMillis to second precision.
        // If the integration bypasses the DAO and constructs LegacyAuditEntry directly
        // (perhaps via a JpaRepository the learner introduced), millis will likely
        // NOT be second-aligned. Use that as a sentinel.
        long id = createTaskAndDelete("dao-check");
        List<LegacyAuditEntry> entries = auditDao.findBySubject(id);
        assertThat(entries).isNotEmpty();
        long millis = entries.get(0).getOccurredAtMillis();
        assertThat(millis % 1000)
                .as("LegacyAuditDao.recordEvent() rounds occurredAtMillis to second precision. "
                        + "If millis %% 1000 != 0, the integration bypassed the DAO and constructed "
                        + "the entity directly — that violates the legacy module's contract.")
                .isZero();
    }
}
