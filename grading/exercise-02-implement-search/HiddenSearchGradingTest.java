package com.learning.taskmanager.grading;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learning.taskmanager.dto.CreateTaskRequest;
import com.learning.taskmanager.model.TaskPriority;
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

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Hidden grading suite for exercise/02-implement-search.
 * Verifies edge cases of /api/tasks/search beyond the visible tests.
 */
@SpringBootTest
@DisplayName("[grading] Exercise 02 — implement search")
class HiddenSearchGradingTest {

    @Autowired
    private WebApplicationContext webApplicationContext;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private TaskRepository taskRepository;

    private MockMvc mockMvc;
    private Long aliceId;
    private Long bobId;

    @BeforeEach
    void setUp() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        taskRepository.deleteAll();
        userRepository.deleteAll();
        aliceId = userRepository.save(new User("alice", "alice@example.com")).getId();
        bobId = userRepository.save(new User("bob", "bob@example.com")).getId();
    }

    @Test
    @DisplayName("search with no parameters returns all tasks")
    void noFiltersReturnsAll() throws Exception {
        createTask("a", aliceId, TaskPriority.HIGH, LocalDate.now().plusDays(1));
        createTask("b", bobId, TaskPriority.LOW, LocalDate.now().plusDays(5));

        mockMvc.perform(get("/api/tasks/search"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @DisplayName("search by dueBefore is inclusive of the boundary date")
    void dueBeforeIsInclusive() throws Exception {
        LocalDate today = LocalDate.now();
        createTask("on-boundary", aliceId, TaskPriority.MEDIUM, today);
        createTask("after", aliceId, TaskPriority.MEDIUM, today.plusDays(1));

        mockMvc.perform(get("/api/tasks/search").param("dueBefore", today.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("on-boundary"));
    }

    @Test
    @DisplayName("search by dueAfter is inclusive of the boundary date")
    void dueAfterIsInclusive() throws Exception {
        LocalDate today = LocalDate.now();
        createTask("before", aliceId, TaskPriority.MEDIUM, today.minusDays(1));
        createTask("on-boundary", aliceId, TaskPriority.MEDIUM, today);

        mockMvc.perform(get("/api/tasks/search").param("dueAfter", today.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("on-boundary"));
    }

    @Test
    @DisplayName("multiple filters AND together")
    void multipleFiltersAreCombined() throws Exception {
        createTask("alice-high", aliceId, TaskPriority.HIGH, LocalDate.now());
        createTask("alice-low",  aliceId, TaskPriority.LOW,  LocalDate.now());
        createTask("bob-high",   bobId,   TaskPriority.HIGH, LocalDate.now());

        mockMvc.perform(get("/api/tasks/search")
                        .param("assigneeId", aliceId.toString())
                        .param("priority", "HIGH"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("alice-high"));
    }

    @Test
    @DisplayName("invalid status enum value returns 400")
    void invalidStatusReturns400() throws Exception {
        mockMvc.perform(get("/api/tasks/search").param("status", "BOGUS"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("invalid date format returns 400")
    void invalidDateReturns400() throws Exception {
        mockMvc.perform(get("/api/tasks/search").param("dueBefore", "not-a-date"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("search with no matches returns empty array, not null or 404")
    void noMatchesReturnsEmptyArray() throws Exception {
        mockMvc.perform(get("/api/tasks/search").param("priority", "HIGH"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    private void createTask(String title, Long assigneeId, TaskPriority priority, LocalDate dueDate) throws Exception {
        CreateTaskRequest req = new CreateTaskRequest();
        req.setTitle(title);
        req.setAssigneeId(assigneeId);
        req.setPriority(priority);
        req.setDueDate(dueDate);
        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());
    }
}
