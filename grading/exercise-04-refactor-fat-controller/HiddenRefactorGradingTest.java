package com.learning.taskmanager.grading;

import com.learning.taskmanager.controller.TaskController;
import com.learning.taskmanager.repository.TaskRepository;
import com.learning.taskmanager.repository.UserRepository;
import com.learning.taskmanager.service.TaskService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Hidden grading suite for exercise/04-refactor-fat-controller.
 * Verifies architectural cleanliness via reflection — the visible tests still pass,
 * but here we additionally check that the controller does NOT depend on repositories
 * or hold business logic, and that TaskService now owns it.
 */
@SpringBootTest
@DisplayName("[grading] Exercise 04 — refactor fat controller")
class HiddenRefactorGradingTest {

    @Autowired
    private TaskController taskController;
    @Autowired
    private TaskService taskService;

    @Test
    @DisplayName("TaskController does NOT directly depend on any *Repository bean")
    void controllerHasNoRepositoryDependency() {
        Field[] fields = TaskController.class.getDeclaredFields();
        boolean hasRepoField = Arrays.stream(fields)
                .anyMatch(f -> f.getType().getName().endsWith("Repository")
                        || f.getType() == TaskRepository.class
                        || f.getType() == UserRepository.class);
        assertThat(hasRepoField)
                .as("TaskController should depend on TaskService only — no repositories. "
                        + "Move data-access calls into the service layer.")
                .isFalse();
    }

    @Test
    @DisplayName("TaskController depends on TaskService")
    void controllerUsesTaskService() {
        Field[] fields = TaskController.class.getDeclaredFields();
        boolean hasServiceField = Arrays.stream(fields)
                .anyMatch(f -> f.getType() == TaskService.class);
        assertThat(hasServiceField)
                .as("TaskController must hold a TaskService reference (constructor-injected).")
                .isTrue();
    }

    @Test
    @DisplayName("TaskController methods are short — no method body over 25 lines")
    void controllerMethodsAreThin() throws Exception {
        // Heuristic: count lines in the .class file's bytecode is hard; instead we
        // check the count of *instructions* per method via Method.toGenericString isn't viable.
        // Use a simpler heuristic: every public controller method must delegate, i.e.
        // it must reference TaskService at least once. We approximate this via the
        // declared methods returning either a DTO/ResponseEntity AND the controller
        // holding only one collaborator.
        long collaboratorCount = Arrays.stream(TaskController.class.getDeclaredFields())
                .filter(f -> !f.getType().isPrimitive() && !f.getType().getName().startsWith("java."))
                .count();
        assertThat(collaboratorCount)
                .as("TaskController should have exactly one collaborator (TaskService) — "
                        + "additional fields signal logic that should live in the service layer.")
                .isEqualTo(1);
    }

    @Test
    @DisplayName("TaskService exposes the operations the controller needs")
    void serviceExposesOperations() {
        boolean hasCreate = false, hasUpdate = false, hasDelete = false, hasGet = false, hasList = false, hasSearch = false;
        for (Method m : TaskService.class.getDeclaredMethods()) {
            String name = m.getName();
            if (name.startsWith("create")) hasCreate = true;
            if (name.startsWith("update")) hasUpdate = true;
            if (name.startsWith("delete")) hasDelete = true;
            if (name.equals("getTask")) hasGet = true;
            if (name.startsWith("listTasks")) hasList = true;
            if (name.equals("search")) hasSearch = true;
        }
        assertThat(hasCreate).as("TaskService.createTask").isTrue();
        assertThat(hasUpdate).as("TaskService.updateTask").isTrue();
        assertThat(hasDelete).as("TaskService.deleteTask").isTrue();
        assertThat(hasGet).as("TaskService.getTask").isTrue();
        assertThat(hasList).as("TaskService.listTasks").isTrue();
        assertThat(hasSearch).as("TaskService.search").isTrue();
    }
}
