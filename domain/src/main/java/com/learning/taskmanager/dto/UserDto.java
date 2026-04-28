package com.learning.taskmanager.dto;

import com.learning.taskmanager.model.User;

public record UserDto(Long id, String username, String email) {

    public static UserDto from(User user) {
        if (user == null) {
            return null;
        }
        return new UserDto(user.getId(), user.getUsername(), user.getEmail());
    }
}
