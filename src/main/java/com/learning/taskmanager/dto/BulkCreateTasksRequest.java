package com.learning.taskmanager.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public class BulkCreateTasksRequest {

    @NotEmpty(message = "tasks must not be empty")
    @Size(max = 1000, message = "at most 1000 tasks per request")
    private List<CreateTaskRequest> tasks;

    public BulkCreateTasksRequest() {
    }

    public List<CreateTaskRequest> getTasks() {
        return tasks;
    }

    public void setTasks(List<CreateTaskRequest> tasks) {
        this.tasks = tasks;
    }
}
