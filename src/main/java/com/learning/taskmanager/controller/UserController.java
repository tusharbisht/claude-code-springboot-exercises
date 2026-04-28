package com.learning.taskmanager.controller;

import com.learning.taskmanager.dto.CreateUserRequest;
import com.learning.taskmanager.dto.TaskDto;
import com.learning.taskmanager.dto.UserDto;
import com.learning.taskmanager.dto.UserSummaryDto;
import com.learning.taskmanager.service.TaskService;
import com.learning.taskmanager.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final TaskService taskService;

    public UserController(UserService userService, TaskService taskService) {
        this.userService = userService;
        this.taskService = taskService;
    }

    @PostMapping
    public ResponseEntity<UserDto> createUser(@Valid @RequestBody CreateUserRequest request) {
        UserDto created = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    public List<UserSummaryDto> listUsers() {
        return userService.listUsersWithTaskCounts();
    }

    @GetMapping("/{id}")
    public UserDto getUser(@PathVariable Long id) {
        return userService.getUser(id);
    }

    @GetMapping("/{id}/tasks")
    public List<TaskDto> listUserTasks(@PathVariable Long id) {
        return taskService.listTasksByAssignee(id);
    }
}
