package com.learning.taskmanager.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learning.taskmanager.dto.CreateUserRequest;
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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Reproduces a customer report that says:
 *
 *   "When users double-tap the signup button, we sometimes end up with two
 *    user records that have the same username. We can't reproduce it
 *    reliably but it's appeared three times in the last week."
 *
 * Goal: this test races N clients trying to create the same username and
 * asserts the database ends up with exactly one matching row.
 */
@SpringBootTest
class UserConcurrencyTest {

    private static final int CLIENT_COUNT = 20;
    private static final String CONTENDED_USERNAME = "alice";

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
    void concurrentCreatesForTheSameUsername_shouldProduceExactlyOneUser() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(CLIENT_COUNT);
        CountDownLatch ready = new CountDownLatch(CLIENT_COUNT);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger successes = new AtomicInteger();
        AtomicInteger conflicts = new AtomicInteger();

        try {
            List<Future<Integer>> futures = new ArrayList<>();
            for (int i = 0; i < CLIENT_COUNT; i++) {
                final int idx = i;
                futures.add(pool.submit(() -> {
                    ready.countDown();
                    start.await();
                    String body = objectMapper.writeValueAsString(
                            new CreateUserRequest(CONTENDED_USERNAME, "alice" + idx + "@example.com"));
                    int status = mockMvc.perform(post("/api/users")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(body))
                            .andReturn().getResponse().getStatus();
                    if (status == 201) successes.incrementAndGet();
                    else if (status == 409) conflicts.incrementAndGet();
                    return status;
                }));
            }

            ready.await(5, TimeUnit.SECONDS);
            start.countDown();
            for (Future<Integer> f : futures) {
                f.get(10, TimeUnit.SECONDS);
            }
        } finally {
            pool.shutdown();
            pool.awaitTermination(10, TimeUnit.SECONDS);
        }

        long persisted = userRepository.findAll().stream()
                .filter(u -> CONTENDED_USERNAME.equals(u.getUsername()))
                .count();

        assertThat(persisted)
                .as("expected exactly one user with username '%s' after %d concurrent creates "
                                + "(observed: %d successes, %d conflicts, %d rows persisted)",
                        CONTENDED_USERNAME, CLIENT_COUNT,
                        successes.get(), conflicts.get(), persisted)
                .isEqualTo(1);
    }
}
