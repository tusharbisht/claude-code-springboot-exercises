package com.learning.taskmanager.repository;

import com.learning.taskmanager.model.Task;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {

    List<Task> findByAssigneeId(Long assigneeId);

    long countByAssigneeId(Long assigneeId);

    // TODO(exercise-02): add a query method that supports filtering tasks by an
    // arbitrary combination of: status, priority, assignee, and a due-date range
    // (inclusive on both ends). Any subset of those filters may be supplied —
    // missing ones must not constrain the result.
}
