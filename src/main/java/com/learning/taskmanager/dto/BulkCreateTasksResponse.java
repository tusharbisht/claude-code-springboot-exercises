package com.learning.taskmanager.dto;

import java.util.List;

public record BulkCreateTasksResponse(int count, List<TaskDto> tasks) {
}
