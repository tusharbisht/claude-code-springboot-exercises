package com.learning.taskmanager.integration;

import com.learning.taskmanager.controller.TaskController;
import com.learning.taskmanager.repository.TaskRepository;
import com.learning.taskmanager.repository.UserRepository;
import com.learning.taskmanager.service.TaskService;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Visible structural test for exercise/04-refactor-fat-controller.
 *
 * The behavioural tests already prove the controller does the right thing for
 * each endpoint. This test enforces a separation-of-concerns boundary:
 * controllers should not talk to repositories directly. Move the business
 * logic into TaskService and make TaskController delegate.
 */
class TaskControllerStructureTest {

    @Test
    void taskController_doesNotDependOnRepositories() {
        Field[] fields = TaskController.class.getDeclaredFields();
        boolean hasRepoField = Arrays.stream(fields)
                .anyMatch(f -> f.getType() == TaskRepository.class
                        || f.getType() == UserRepository.class);
        assertThat(hasRepoField)
                .as("TaskController must not depend on a *Repository directly. "
                        + "Inject TaskService instead and let it own data-access calls.")
                .isFalse();
    }

    @Test
    void taskController_dependsOnTaskService() {
        Field[] fields = TaskController.class.getDeclaredFields();
        boolean hasServiceField = Arrays.stream(fields)
                .anyMatch(f -> f.getType() == TaskService.class);
        assertThat(hasServiceField)
                .as("TaskController should hold a TaskService field (constructor-injected) "
                        + "and route every endpoint through it.")
                .isTrue();
    }
}
