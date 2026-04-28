package com.learning.taskmanager.repository;

import com.learning.taskmanager.dto.UserSummaryDto;
import com.learning.taskmanager.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);

    @Query("""
            SELECT new com.learning.taskmanager.dto.UserSummaryDto(
                u.id, u.username, u.email, COUNT(t.id))
            FROM User u
            LEFT JOIN Task t ON t.assignee.id = u.id
            GROUP BY u.id, u.username, u.email
            ORDER BY u.id
            """)
    List<UserSummaryDto> findAllWithTaskCounts();
}
