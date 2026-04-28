package com.learning.taskmanager.controller;

import com.learning.taskmanager.dto.BulkCreateTasksRequest;
import com.learning.taskmanager.dto.BulkCreateTasksResponse;
import com.learning.taskmanager.dto.CreateTaskRequest;
import com.learning.taskmanager.dto.TaskDto;
import com.learning.taskmanager.dto.UpdateTaskRequest;
import com.learning.taskmanager.model.TaskPriority;
import com.learning.taskmanager.model.TaskStatus;
import com.learning.taskmanager.service.TaskService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @PostMapping
    public ResponseEntity<TaskDto> createTask(@Valid @RequestBody CreateTaskRequest request) {
        TaskDto created = taskService.createTask(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Atomic bulk-create. The contract says: either every task in the request
     * is persisted OR no tasks are persisted. Validation failures, missing
     * assignees, and DB errors must roll the whole batch back.
     *
     * The visible test only covers the happy path (all tasks valid).
     * The hidden grading suite covers atomicity. Read carefully.
     */
    @PostMapping("/bulk")
    public ResponseEntity<BulkCreateTasksResponse> bulkCreate(@Valid @RequestBody BulkCreateTasksRequest request) {
        // TODO(exercise-09): implement.
        throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED, "bulk create not implemented yet");
    }

    @GetMapping
    public List<TaskDto> listTasks() {
        return taskService.listTasks();
    }

    @GetMapping("/{id}")
    public TaskDto getTask(@PathVariable Long id) {
        return taskService.getTask(id);
    }

    @PutMapping("/{id}")
    public TaskDto updateTask(@PathVariable Long id, @Valid @RequestBody UpdateTaskRequest request) {
        return taskService.updateTask(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(@PathVariable Long id) {
        taskService.deleteTask(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/search")
    public List<TaskDto> searchTasks(
            @RequestParam(required = false) TaskStatus status,
            @RequestParam(required = false) TaskPriority priority,
            @RequestParam(required = false) Long assigneeId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueBefore,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueAfter) {
        return taskService.search(status, priority, assigneeId, dueBefore, dueAfter);
    }
}
