package com.learning.taskmanager.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learning.taskmanager.dto.CreateTaskRequest;
import com.learning.taskmanager.model.TaskPriority;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class TasksByPriorityTest {

    @Autowired
    private WebApplicationContext webApplicationContext;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private TaskRepository taskRepository;
    @Autowired
    private UserRepository userRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        taskRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void byPriority_groupsTasksAndIncludesAllPriorityKeys() throws Exception {
        createTask("a", TaskPriority.HIGH);
        createTask("b", TaskPriority.HIGH);
        createTask("c", TaskPriority.MEDIUM);

        mockMvc.perform(get("/api/tasks/by-priority"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.HIGH.length()").value(2))
                .andExpect(jsonPath("$.MEDIUM.length()").value(1))
                .andExpect(jsonPath("$.LOW").exists())
                .andExpect(jsonPath("$.LOW.length()").value(0));
    }

    private void createTask(String title, TaskPriority priority) throws Exception {
        CreateTaskRequest req = new CreateTaskRequest();
        req.setTitle(title);
        req.setPriority(priority);
        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());
    }
}
