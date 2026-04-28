package com.learning.taskmanager.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learning.taskmanager.dto.CreateTaskRequest;
import com.learning.taskmanager.dto.UpdateTaskRequest;
import com.learning.taskmanager.model.TaskPriority;
import com.learning.taskmanager.model.TaskStatus;
import com.learning.taskmanager.model.User;
import com.learning.taskmanager.repository.TaskRepository;
import com.learning.taskmanager.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class TaskApiIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private TaskRepository taskRepository;
    @Autowired
    private UserRepository userRepository;

    private MockMvc mockMvc;
    private Long aliceId;

    @BeforeEach
    @Transactional
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        taskRepository.deleteAll();
        userRepository.deleteAll();
        User alice = userRepository.save(new User("alice", "alice@example.com"));
        aliceId = alice.getId();
    }

    @Test
    void createTask_validRequest_returns201() throws Exception {
        CreateTaskRequest request = new CreateTaskRequest();
        request.setTitle("Write docs");
        request.setDescription("Document the API");
        request.setPriority(TaskPriority.HIGH);
        request.setDueDate(LocalDate.now().plusDays(3));
        request.setAssigneeId(aliceId);

        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.title").value("Write docs"))
                .andExpect(jsonPath("$.status").value("TODO"))
                .andExpect(jsonPath("$.priority").value("HIGH"))
                .andExpect(jsonPath("$.assigneeId").value(aliceId));
    }

    @Test
    void createTask_blankTitle_returns400() throws Exception {
        CreateTaskRequest request = new CreateTaskRequest();
        request.setTitle("");
        request.setDescription("body");

        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.title").exists());
    }

    @Test
    void createTask_titleOver200Chars_returns400() throws Exception {
        CreateTaskRequest request = new CreateTaskRequest();
        request.setTitle("x".repeat(201));

        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.title").exists());
    }

    @Test
    void createTask_unknownAssignee_returns404() throws Exception {
        CreateTaskRequest request = new CreateTaskRequest();
        request.setTitle("Orphan");
        request.setAssigneeId(99999L);

        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void getTask_notFound_returns404() throws Exception {
        mockMvc.perform(get("/api/tasks/{id}", 99999L))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateTask_changeStatus_persists() throws Exception {
        Long taskId = createTask("Original", null);

        UpdateTaskRequest update = new UpdateTaskRequest();
        update.setStatus(TaskStatus.IN_PROGRESS);

        mockMvc.perform(put("/api/tasks/{id}", taskId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    @Test
    void deleteTask_existing_returns204() throws Exception {
        Long taskId = createTask("To delete", null);

        mockMvc.perform(delete("/api/tasks/{id}", taskId))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/tasks/{id}", taskId))
                .andExpect(status().isNotFound());
    }

    @Test
    void searchTasks_byStatus_returnsMatching() throws Exception {
        Long t1 = createTask("Task 1", aliceId);
        Long t2 = createTask("Task 2", aliceId);

        UpdateTaskRequest update = new UpdateTaskRequest();
        update.setStatus(TaskStatus.DONE);
        mockMvc.perform(put("/api/tasks/{id}", t1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/tasks/search").param("status", "DONE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(t1));

        mockMvc.perform(get("/api/tasks/search").param("status", "TODO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(t2));
    }

    @Test
    void searchTasks_byAssignee_returnsMatching() throws Exception {
        User bob = userRepository.save(new User("bob", "bob@example.com"));
        Long bobId = bob.getId();
        createTask("Alice task", aliceId);
        createTask("Bob task", bobId);

        mockMvc.perform(get("/api/tasks/search").param("assigneeId", bobId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("Bob task"));
    }

    @Test
    void searchTasks_byPriority_returnsMatching() throws Exception {
        CreateTaskRequest high = new CreateTaskRequest();
        high.setTitle("Important");
        high.setPriority(TaskPriority.HIGH);
        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(high)))
                .andExpect(status().isCreated());

        CreateTaskRequest low = new CreateTaskRequest();
        low.setTitle("Whatever");
        low.setPriority(TaskPriority.LOW);
        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(low)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/tasks/search").param("priority", "HIGH"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("Important"));
    }

    @Test
    void listUserTasks_returnsOnlyUserTasks() throws Exception {
        User bob = userRepository.save(new User("bob", "bob@example.com"));
        Long bobId = bob.getId();
        createTask("Alice 1", aliceId);
        createTask("Alice 2", aliceId);
        createTask("Bob 1", bobId);

        mockMvc.perform(get("/api/users/{id}/tasks", aliceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    private Long createTask(String title, Long assigneeId) throws Exception {
        CreateTaskRequest request = new CreateTaskRequest();
        request.setTitle(title);
        if (assigneeId != null) {
            request.setAssigneeId(assigneeId);
        }
        String body = mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asLong();
    }
}
