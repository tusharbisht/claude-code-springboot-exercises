package com.learning.taskmanager.grading;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learning.taskmanager.dto.BulkCreateTasksRequest;
import com.learning.taskmanager.dto.CreateTaskRequest;
import com.learning.taskmanager.model.User;
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

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Hidden grading suite for exercise/09-confident-but-wrong.
 *
 * Verifies the atomicity contract that the visible happy-path test does NOT.
 * A naive `tasks.forEach(taskService::createTask)` implementation will pass
 * the visible test and fail several of these.
 */
@SpringBootTest
@DisplayName("[grading] Exercise 09 — confident but wrong")
class HiddenAtomicityGradingTest {

    @Autowired
    private WebApplicationContext webApplicationContext;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private TaskRepository taskRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        taskRepository.deleteAll();
        userRepository.deleteAll();
    }

    private CreateTaskRequest taskReq(String title) {
        CreateTaskRequest r = new CreateTaskRequest();
        r.setTitle(title);
        return r;
    }

    private CreateTaskRequest taskReqWithAssignee(String title, Long assigneeId) {
        CreateTaskRequest r = taskReq(title);
        r.setAssigneeId(assigneeId);
        return r;
    }

    private BulkCreateTasksRequest batch(CreateTaskRequest... reqs) {
        BulkCreateTasksRequest b = new BulkCreateTasksRequest();
        List<CreateTaskRequest> list = new ArrayList<>();
        for (CreateTaskRequest r : reqs) list.add(r);
        b.setTasks(list);
        return b;
    }

    @Test
    @DisplayName("invalid task in the middle rolls back the entire batch")
    void invalidInMiddle_rollsBackAll() throws Exception {
        BulkCreateTasksRequest req = batch(
                taskReq("ok-1"),
                taskReq(""),       // blank title — fails @NotBlank
                taskReq("ok-3"));

        mockMvc.perform(post("/api/tasks/bulk")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().is4xxClientError());

        assertThat(taskRepository.count())
                .as("no tasks should be persisted when any task in the batch is invalid")
                .isZero();
    }

    @Test
    @DisplayName("missing assignee in any element rolls back the entire batch")
    void missingAssignee_rollsBackAll() throws Exception {
        BulkCreateTasksRequest req = batch(
                taskReq("ok-1"),
                taskReqWithAssignee("ok-2", 99999L),  // assignee doesn't exist
                taskReq("ok-3"));

        mockMvc.perform(post("/api/tasks/bulk")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().is4xxClientError());

        assertThat(taskRepository.count())
                .as("no tasks should be persisted when an assignee reference is invalid")
                .isZero();
    }

    @Test
    @DisplayName("empty task list rejected with 400")
    void emptyList_rejected() throws Exception {
        BulkCreateTasksRequest req = new BulkCreateTasksRequest();
        req.setTasks(List.of());

        mockMvc.perform(post("/api/tasks/bulk")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());

        assertThat(taskRepository.count()).isZero();
    }

    @Test
    @DisplayName("oversized batch (> 1000) rejected with 400")
    void oversizedBatch_rejected() throws Exception {
        BulkCreateTasksRequest req = new BulkCreateTasksRequest();
        List<CreateTaskRequest> tasks = new ArrayList<>();
        for (int i = 0; i < 1001; i++) tasks.add(taskReq("t" + i));
        req.setTasks(tasks);

        mockMvc.perform(post("/api/tasks/bulk")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());

        assertThat(taskRepository.count()).isZero();
    }

    @Test
    @DisplayName("100 valid tasks all persist with a single transaction")
    void hundredValid_allPersist() throws Exception {
        User alice = userRepository.save(new User("alice", "alice@example.com"));
        BulkCreateTasksRequest req = new BulkCreateTasksRequest();
        List<CreateTaskRequest> tasks = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            CreateTaskRequest r = taskReqWithAssignee("t" + i, alice.getId());
            tasks.add(r);
        }
        req.setTasks(tasks);

        mockMvc.perform(post("/api/tasks/bulk")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());

        assertThat(taskRepository.count()).isEqualTo(100);
    }

    @Test
    @DisplayName("partial-failure mid-batch leaves no orphan rows on retry")
    void retryAfterFailure_leavesNoOrphans() throws Exception {
        // First call: contains an invalid entry — entire batch should be rolled back.
        BulkCreateTasksRequest bad = batch(taskReq("a"), taskReq(""), taskReq("c"));
        mockMvc.perform(post("/api/tasks/bulk")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bad)))
                .andExpect(status().is4xxClientError());
        assertThat(taskRepository.count()).isZero();

        // Second call: all valid — should succeed cleanly with no leftover state.
        BulkCreateTasksRequest good = batch(taskReq("a"), taskReq("b"), taskReq("c"));
        mockMvc.perform(post("/api/tasks/bulk")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(good)))
                .andExpect(status().isCreated());
        assertThat(taskRepository.count()).isEqualTo(3);
    }
}
