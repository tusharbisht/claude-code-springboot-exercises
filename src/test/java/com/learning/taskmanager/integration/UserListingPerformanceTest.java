package com.learning.taskmanager.integration;

import com.learning.taskmanager.model.Task;
import com.learning.taskmanager.model.User;
import com.learning.taskmanager.repository.TaskRepository;
import com.learning.taskmanager.repository.UserRepository;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
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
 * Visible performance test for exercise/03-optimize-n-plus-one.
 *
 * GET /api/users must list users with task counts in a fixed (small) number of
 * SQL statements regardless of how many users exist. The current implementation
 * issues 1 + N statements (the classic N+1 query problem).
 *
 * Asserts via Hibernate's prepared-statement counter.
 */
@SpringBootTest
@TestPropertySource(properties = {
        "spring.jpa.properties.hibernate.generate_statistics=true"
})
class UserListingPerformanceTest {

    @Autowired
    private WebApplicationContext webApplicationContext;
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
        for (int i = 0; i < 5; i++) {
            User u = userRepository.save(new User("user" + i, "user" + i + "@example.com"));
            Task t = new Task();
            t.setTitle("task " + i);
            t.setAssignee(u);
            taskRepository.save(t);
        }
    }

    @Test
    void listUsers_doesNotIssueOneQueryPerUser() throws Exception {
        SessionFactory sessionFactory = entityManagerFactory.unwrap(SessionFactory.class);
        Statistics stats = sessionFactory.getStatistics();
        stats.setStatisticsEnabled(true);
        stats.clear();

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(5));

        long prepared = stats.getPrepareStatementCount();
        assertThat(prepared)
                .as("GET /api/users should run in O(1) SQL statements (got %d for 5 users)", prepared)
                .isLessThanOrEqualTo(2);
    }
}
