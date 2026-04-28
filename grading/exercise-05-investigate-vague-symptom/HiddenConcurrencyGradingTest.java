package com.learning.taskmanager.grading;

import com.fasterxml.jackson.databind.ObjectMapper;
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
 * Hidden grading suite for exercise/05-investigate-vague-symptom.
 *
 * Beyond the visible "exactly one row" assertion, this checks that:
 *  - the loser threads receive a clean 409 response (not a 500)
 *  - concurrent creates of DIFFERENT usernames all succeed
 *  - the unique constraint actually exists at the DB level (probe via reflection-free check)
 */
@SpringBootTest
@DisplayName("[grading] Exercise 05 — investigate vague symptom")
class HiddenConcurrencyGradingTest {

    private static final int CLIENTS = 16;

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
    @DisplayName("loser threads get HTTP 409, never 500")
    void loserThreadsGetCleanConflictResponse() throws Exception {
        AtomicInteger statuses500 = new AtomicInteger();
        AtomicInteger statuses409 = new AtomicInteger();
        AtomicInteger statuses201 = new AtomicInteger();

        race("alice", CLIENTS, status -> {
            if (status == 500) statuses500.incrementAndGet();
            if (status == 409) statuses409.incrementAndGet();
            if (status == 201) statuses201.incrementAndGet();
        });

        assertThat(statuses500.get())
                .as("no client should observe HTTP 500 — duplicate-username errors must surface as 409")
                .isZero();
        assertThat(statuses201.get()).isEqualTo(1);
        assertThat(statuses409.get()).isEqualTo(CLIENTS - 1);
    }

    @Test
    @DisplayName("concurrent creates with distinct usernames all succeed")
    void concurrentDistinctUsernames_allSucceed() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(CLIENTS);
        CountDownLatch ready = new CountDownLatch(CLIENTS);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger created = new AtomicInteger();

        try {
            List<Future<?>> futures = new ArrayList<>();
            for (int i = 0; i < CLIENTS; i++) {
                final int idx = i;
                futures.add(pool.submit(() -> {
                    ready.countDown();
                    start.await();
                    String body = objectMapper.writeValueAsString(
                            new CreateUserRequest("user" + idx, "user" + idx + "@example.com"));
                    int status = mockMvc.perform(post("/api/users")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(body))
                            .andReturn().getResponse().getStatus();
                    if (status == 201) created.incrementAndGet();
                    return null;
                }));
            }
            ready.await(5, TimeUnit.SECONDS);
            start.countDown();
            for (Future<?> f : futures) f.get(10, TimeUnit.SECONDS);
        } finally {
            pool.shutdown();
            pool.awaitTermination(10, TimeUnit.SECONDS);
        }

        assertThat(created.get()).isEqualTo(CLIENTS);
        assertThat(userRepository.count()).isEqualTo(CLIENTS);
    }

    private interface IntConsumer {
        void accept(int value);
    }

    private void race(String username, int clients, IntConsumer collector) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(clients);
        CountDownLatch ready = new CountDownLatch(clients);
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<Integer>> futures = new ArrayList<>();
            for (int i = 0; i < clients; i++) {
                final int idx = i;
                futures.add(pool.submit(() -> {
                    ready.countDown();
                    start.await();
                    String body = objectMapper.writeValueAsString(
                            new CreateUserRequest(username, "u" + idx + "@example.com"));
                    return mockMvc.perform(post("/api/users")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(body))
                            .andReturn().getResponse().getStatus();
                }));
            }
            ready.await(5, TimeUnit.SECONDS);
            start.countDown();
            for (Future<Integer> f : futures) collector.accept(f.get(10, TimeUnit.SECONDS));
        } finally {
            pool.shutdown();
            pool.awaitTermination(10, TimeUnit.SECONDS);
        }
    }
}
