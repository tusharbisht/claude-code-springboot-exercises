package com.learning.taskmanager.service;

import com.learning.taskmanager.dto.CreateUserRequest;
import com.learning.taskmanager.dto.UserDto;
import com.learning.taskmanager.dto.UserSummaryDto;
import com.learning.taskmanager.exception.DuplicateResourceException;
import com.learning.taskmanager.exception.ResourceNotFoundException;
import com.learning.taskmanager.model.User;
import com.learning.taskmanager.repository.TaskRepository;
import com.learning.taskmanager.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final TaskRepository taskRepository;

    public UserService(UserRepository userRepository, TaskRepository taskRepository) {
        this.userRepository = userRepository;
        this.taskRepository = taskRepository;
    }

    @Transactional
    public UserDto createUser(CreateUserRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateResourceException("username already exists: " + request.getUsername());
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("email already exists: " + request.getEmail());
        }
        User user = new User(request.getUsername(), request.getEmail());
        return UserDto.from(userRepository.save(user));
    }

    @Transactional(readOnly = true)
    public UserDto getUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("user not found: " + id));
        return UserDto.from(user);
    }

    @Transactional(readOnly = true)
    public List<UserSummaryDto> listUsersWithTaskCounts() {
        return userRepository.findAllWithTaskCounts();
    }
}
