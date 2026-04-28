package com.learning.taskmanager.dto;

public record UserSummaryDto(Long id, String username, String email, long taskCount) {
}
