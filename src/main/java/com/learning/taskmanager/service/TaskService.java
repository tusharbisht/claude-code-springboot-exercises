package com.learning.taskmanager.service;

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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;

    public TaskService(TaskRepository taskRepository, UserRepository userRepository) {
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public TaskDto createTask(CreateTaskRequest request) {
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
        return TaskDto.from(taskRepository.save(task));
    }

    @Transactional(readOnly = true)
    public TaskDto getTask(Long id) {
        return TaskDto.from(findOrThrow(id));
    }

    @Transactional(readOnly = true)
    public List<TaskDto> listTasks() {
        return taskRepository.findAll().stream().map(TaskDto::from).toList();
    }

    @Transactional(readOnly = true)
    public List<TaskDto> listTasksByAssignee(Long assigneeId) {
        if (!userRepository.existsById(assigneeId)) {
            throw new ResourceNotFoundException("user not found: " + assigneeId);
        }
        return taskRepository.findByAssigneeId(assigneeId).stream().map(TaskDto::from).toList();
    }

    @Transactional
    public TaskDto updateTask(Long id, UpdateTaskRequest request) {
        Task task = findOrThrow(id);
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

    @Transactional
    public void deleteTask(Long id) {
        Task task = findOrThrow(id);
        taskRepository.delete(task);
    }

    @Transactional(readOnly = true)
    public List<TaskDto> search(TaskStatus status,
                                TaskPriority priority,
                                Long assigneeId,
                                LocalDate dueBefore,
                                LocalDate dueAfter) {
        return taskRepository.search(status, priority, assigneeId, dueBefore, dueAfter)
                .stream().map(TaskDto::from).toList();
    }

    private Task findOrThrow(Long id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("task not found: " + id));
    }
}
