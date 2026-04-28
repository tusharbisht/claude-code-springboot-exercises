package com.learning.taskmanager.grading;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learning.taskmanager.model.Task;
import com.learning.taskmanager.model.User;
import com.learning.taskmanager.repository.TaskRepository;
import com.learning.taskmanager.repository.UserRepository;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Hidden grading suite for exercise/03-optimize-n-plus-one.
 * Asserts that GET /api/users does NOT exhibit an N+1 query pattern by
 * inspecting Hibernate prepared-statement counts.
 */
@SpringBootTest
@TestPropertySource(properties = {
        "spring.jpa.properties.hibernate.generate_statistics=true"
})
@DisplayName("[grading] Exercise 03 — optimize N+1")
class HiddenQueryCountGradingTest {

    @Autowired
    private WebApplicationContext webApplicationContext;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private TaskRepository taskRepository;
    @Autowired
    private EntityManagerFactory entityManagerFactory;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        taskRepository.deleteAll();
        userRepository.deleteAll();

        // Seed 10 users, each with several tasks.
        for (int i = 0; i < 10; i++) {
            User u = userRepository.save(new User("user" + i, "user" + i + "@example.com"));
            for (int j = 0; j < 3; j++) {
                Task t = new Task();
                t.setTitle("task " + i + "-" + j);
                t.setAssignee(u);
                taskRepository.save(t);
            }
        }
    }

    @Test
    @DisplayName("GET /api/users runs at most 2 SQL statements regardless of user count")
    void listUsersHasNoNPlusOne() throws Exception {
        SessionFactory sessionFactory = entityManagerFactory.unwrap(SessionFactory.class);
        Statistics stats = sessionFactory.getStatistics();
        stats.setStatisticsEnabled(true);
        stats.clear();

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(10));

        long prepared = stats.getPrepareStatementCount();

        // A correct implementation does this in 1 query (or 2 with COUNT separately).
        // The N+1 version executes 1 + N = 11 statements for 10 users.
        assertThat(prepared)
                .as("GET /api/users should not run N+1 queries (got %d prepared statements for 10 users)", prepared)
                .isLessThanOrEqualTo(2);
    }

    @Test
    @DisplayName("task counts in the response are correct")
    void taskCountsAreAccurate() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(10))
                .andExpect(jsonPath("$[0].taskCount").value(3))
                .andExpect(jsonPath("$[9].taskCount").value(3));
    }
}
