package com.learning.taskmanager.dto;

import com.learning.taskmanager.model.Task;
import com.learning.taskmanager.model.TaskPriority;
import com.learning.taskmanager.model.TaskStatus;

import java.time.Instant;
import java.time.LocalDate;

public record TaskDto(
        Long id,
        String title,
        String description,
        TaskStatus status,
        TaskPriority priority,
        LocalDate dueDate,
        Long assigneeId,
        String assigneeUsername,
        Instant createdAt,
        Instant updatedAt) {

    public static TaskDto from(Task task) {
        Long assigneeId = task.getAssignee() != null ? task.getAssignee().getId() : null;
        String assigneeUsername = task.getAssignee() != null ? task.getAssignee().getUsername() : null;
        return new TaskDto(
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                task.getStatus(),
                task.getPriority(),
                task.getDueDate(),
                assigneeId,
                assigneeUsername,
                task.getCreatedAt(),
                task.getUpdatedAt());
    }
}
