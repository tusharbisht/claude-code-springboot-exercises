package com.learning.taskmanager.controller;

import com.learning.taskmanager.dto.CreateTaskRequest;
import com.learning.taskmanager.dto.TaskDto;
import com.learning.taskmanager.dto.UpdateTaskRequest;
import com.learning.taskmanager.exception.ResourceNotFoundException;
import com.learning.taskmanager.model.Task;
import com.learning.taskmanager.model.TaskPriority;
import com.learning.taskmanager.model.TaskStatus;
import com.learning.taskmanager.model.User;
import com.learning.taskmanager.repository.TaskRepository;
import com.learning.taskmanager.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * TODO(exercise-04): this controller has grown into a "fat controller" — it
 * holds business logic, talks directly to repositories, and manages
 * transactions. Refactor by moving the logic into the (already-defined)
 * TaskService and making this class delegate.
 */
@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;

    public TaskController(TaskRepository taskRepository, UserRepository userRepository) {
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
    }

    @PostMapping
    @Transactional
    public ResponseEntity<TaskDto> createTask(@Valid @RequestBody CreateTaskRequest request) {
        Task task = new Task();
        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setStatus(request.getStatus() == null ? TaskStatus.TODO : request.getStatus());
        task.setPriority(request.getPriority() == null ? TaskPriority.MEDIUM : request.getPriority());
        task.setDueDate(request.getDueDate());
        if (request.getAssigneeId() != null) {
            User assignee = userRepository.findById(request.getAssigneeId())
                    .orElseThrow(() -> new ResourceNotFoundException("user not found: " + request.getAssigneeId()));
            task.setAssignee(assignee);
        }
        Task saved = taskRepository.save(task);
        return ResponseEntity.status(HttpStatus.CREATED).body(TaskDto.from(saved));
    }

    @GetMapping
    @Transactional(readOnly = true)
    public List<TaskDto> listTasks() {
        return taskRepository.findAll().stream().map(TaskDto::from).toList();
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public TaskDto getTask(@PathVariable Long id) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("task not found: " + id));
        return TaskDto.from(task);
    }

    @PutMapping("/{id}")
    @Transactional
    public TaskDto updateTask(@PathVariable Long id, @Valid @RequestBody UpdateTaskRequest request) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("task not found: " + id));
        if (request.getTitle() != null) {
            task.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            task.setDescription(request.getDescription());
        }
        if (request.getStatus() != null) {
            task.setStatus(request.getStatus());
        }
        if (request.getPriority() != null) {
            task.setPriority(request.getPriority());
        }
        if (request.getDueDate() != null) {
            task.setDueDate(request.getDueDate());
        }
        if (request.getAssigneeId() != null) {
            User assignee = userRepository.findById(request.getAssigneeId())
                    .orElseThrow(() -> new ResourceNotFoundException("user not found: " + request.getAssigneeId()));
            task.setAssignee(assignee);
        }
        return TaskDto.from(taskRepository.save(task));
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<Void> deleteTask(@PathVariable Long id) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("task not found: " + id));
        taskRepository.delete(task);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/search")
    @Transactional(readOnly = true)
    public List<TaskDto> searchTasks(
            @RequestParam(required = false) TaskStatus status,
            @RequestParam(required = false) TaskPriority priority,
            @RequestParam(required = false) Long assigneeId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueBefore,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueAfter) {
        return taskRepository.search(status, priority, assigneeId, dueBefore, dueAfter)
                .stream().map(TaskDto::from).toList();
    }
}
