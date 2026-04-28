package com.learning.taskmanager.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learning.taskmanager.dto.BulkCreateTasksRequest;
import com.learning.taskmanager.dto.CreateTaskRequest;
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

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The visible test for exercise/09. Only exercises the happy path: all
 * three tasks are valid, all three should be created. A naive
 * implementation that just iterates and calls createTask() will pass this.
 *
 * The hidden grading suite checks the atomicity contract that this test
 * deliberately doesn't.
 */
@SpringBootTest
class BulkCreateHappyPathTest {

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

    @Test
    void bulkCreate_allValid_createsAllTasks() throws Exception {
        BulkCreateTasksRequest req = new BulkCreateTasksRequest();
        req.setTasks(List.of(
                taskReq("First"),
                taskReq("Second"),
                taskReq("Third")));

        mockMvc.perform(post("/api/tasks/bulk")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.count").value(3))
                .andExpect(jsonPath("$.tasks.length()").value(3))
                .andExpect(jsonPath("$.tasks[0].title").value("First"))
                .andExpect(jsonPath("$.tasks[2].title").value("Third"));
    }

    private CreateTaskRequest taskReq(String title) {
        CreateTaskRequest r = new CreateTaskRequest();
        r.setTitle(title);
        return r;
    }
}
