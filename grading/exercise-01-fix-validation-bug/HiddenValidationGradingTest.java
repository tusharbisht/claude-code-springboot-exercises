package com.learning.taskmanager.grading;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learning.taskmanager.dto.CreateTaskRequest;
import com.learning.taskmanager.dto.CreateUserRequest;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Hidden grading suite for exercise/01-fix-validation-bug.
 * Verifies edge cases that the visible tests don't cover.
 */
@SpringBootTest
@DisplayName("[grading] Exercise 01 — fix validation bug")
class HiddenValidationGradingTest {

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
    @DisplayName("user request with whitespace-only username is rejected")
    void whitespaceOnlyUsernameRejected() throws Exception {
        CreateUserRequest request = new CreateUserRequest("   ", "ws@example.com");
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.username").exists());
    }

    @Test
    @DisplayName("user request with username over 50 chars is rejected")
    void oversizedUsernameRejected() throws Exception {
        CreateUserRequest request = new CreateUserRequest("a".repeat(51), "x@example.com");
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.username").exists());
    }

    @Test
    @DisplayName("user request with malformed email (missing TLD) is rejected")
    void malformedEmailRejected() throws Exception {
        CreateUserRequest request = new CreateUserRequest("alice", "alice@");
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.email").exists());
    }

    @Test
    @DisplayName("task request with null title is rejected as 400, not 500")
    void nullTitleRejected() throws Exception {
        String body = "{\"description\":\"no title here\"}";
        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.title").exists());
    }

    @Test
    @DisplayName("task request with title exactly 200 chars is accepted")
    void titleAt200CharBoundaryAccepted() throws Exception {
        CreateTaskRequest request = new CreateTaskRequest();
        request.setTitle("x".repeat(200));
        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("task request with description over 2000 chars is rejected")
    void oversizedDescriptionRejected() throws Exception {
        CreateTaskRequest request = new CreateTaskRequest();
        request.setTitle("ok");
        request.setDescription("y".repeat(2001));
        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.description").exists());
    }

    @Test
    @DisplayName("validation error response includes a fieldErrors map, not a stack trace")
    void validationErrorShapeIsClean() throws Exception {
        CreateUserRequest request = new CreateUserRequest("", "");
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors").isMap())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(400));
    }
}
