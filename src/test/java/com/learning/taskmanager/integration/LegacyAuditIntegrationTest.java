package com.learning.taskmanager.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learning.taskmanager.dto.CreateTaskRequest;
import com.learning.taskmanager.legacy.LegacyAuditDao;
import com.learning.taskmanager.repository.TaskRepository;
import com.learning.taskmanager.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Visible test for meta/03-custom-subagent.
 *
 * The integration: when a task is deleted via the modern TaskService, an
 * entry must be recorded in the legacy audit log. That requires
 * understanding the legacy module's conventions (handwritten DAO, mandatory
 * use of recordEvent(), kind = "TASK_DELETED" by upper-case convention).
 *
 * The intended workflow is to build a custom subagent that knows how to
 * navigate the legacy package, then use it. See EXERCISE.md.
 */
@SpringBootTest
class LegacyAuditIntegrationTest {

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

    @Test
    void deletingTask_writesLegacyAuditEntry() throws Exception {
        long beforeDeletes = auditDao.countByKind("TASK_DELETED");

        CreateTaskRequest req = new CreateTaskRequest();
        req.setTitle("Audit me");
        String body = mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long taskId = objectMapper.readTree(body).get("id").asLong();

        mockMvc.perform(delete("/api/tasks/{id}", taskId))
                .andExpect(status().isNoContent());

        long afterDeletes = auditDao.countByKind("TASK_DELETED");
        assertThat(afterDeletes - beforeDeletes)
                .as("deleting a task should write exactly one legacy audit entry "
                        + "with kind=TASK_DELETED via LegacyAuditDao.recordEvent()")
                .isEqualTo(1);

        assertThat(auditDao.findBySubject(taskId))
                .as("the audit entry's subject_id should equal the deleted task id")
                .isNotEmpty();
    }
}
