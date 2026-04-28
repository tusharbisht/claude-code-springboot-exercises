package com.learning.taskmanager.repository;

import com.learning.taskmanager.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);

    // HINT(exercise-03): a JPQL projection query that LEFT JOINs Task and
    // returns a UserSummaryDto directly would let UserService render the user
    // list in a single SQL statement.
}
