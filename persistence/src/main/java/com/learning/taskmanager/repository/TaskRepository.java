package com.learning.taskmanager.repository;

import com.learning.taskmanager.model.Task;
import com.learning.taskmanager.model.TaskPriority;
import com.learning.taskmanager.model.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {

    List<Task> findByAssigneeId(Long assigneeId);

    long countByAssigneeId(Long assigneeId);

    @Query("""
            SELECT t FROM Task t
            WHERE (:status IS NULL OR t.status = :status)
              AND (:priority IS NULL OR t.priority = :priority)
              AND (:assigneeId IS NULL OR t.assignee.id = :assigneeId)
              AND (:dueBefore IS NULL OR t.dueDate <= :dueBefore)
              AND (:dueAfter IS NULL OR t.dueDate >= :dueAfter)
            """)
    List<Task> search(TaskStatus status,
                      TaskPriority priority,
                      Long assigneeId,
                      LocalDate dueBefore,
                      LocalDate dueAfter);
}
