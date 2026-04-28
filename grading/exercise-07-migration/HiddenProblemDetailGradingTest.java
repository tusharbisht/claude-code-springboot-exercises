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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Hidden grading suite for exercise/07-migration.
 *
 * Covers RFC 7807 conformance plus a couple of Spring 3-specific
 * regressions (binding errors on @RequestParam, type-mismatch errors).
 */
@SpringBootTest
@DisplayName("[grading] Exercise 07 — migrate to ProblemDetail")
class HiddenProblemDetailGradingTest {

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
    @DisplayName("validation problem has both title and detail set explicitly (not 'about:blank')")
    void validationDetailIsSpecific() throws Exception {
        CreateTaskRequest req = new CreateTaskRequest();   // missing title
        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.title").isString())
                .andExpect(jsonPath("$.detail").isString());
    }

    @Test
    @DisplayName("not-found problem includes the path in instance")
    void notFoundInstanceIsRequestPath() throws Exception {
        mockMvc.perform(get("/api/tasks/{id}", 12345L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.instance").value("/api/tasks/12345"));
    }

    @Test
    @DisplayName("validation problem keeps fieldErrors extension")
    void fieldErrorsExtensionPreserved() throws Exception {
        CreateUserRequest req = new CreateUserRequest("", "not-an-email");
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.username").exists())
                .andExpect(jsonPath("$.fieldErrors.email").exists());
    }

    @Test
    @DisplayName("conflict problem (duplicate user) is 409 ProblemDetail")
    void conflictResponseShape() throws Exception {
        CreateUserRequest a = new CreateUserRequest("alice", "alice@example.com");
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(a)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateUserRequest("alice", "alice2@example.com"))))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    @DisplayName("invalid type for path variable returns ProblemDetail (not stack trace)")
    void typeMismatchIsProblemDetail() throws Exception {
        mockMvc.perform(get("/api/tasks/not-a-number"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("legacy keys 'message', 'error', 'timestamp' are gone")
    void legacyShapeRemoved() throws Exception {
        mockMvc.perform(get("/api/tasks/{id}", 99999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").doesNotExist())
                .andExpect(jsonPath("$.error").doesNotExist())
                .andExpect(jsonPath("$.timestamp").doesNotExist());
    }
}
