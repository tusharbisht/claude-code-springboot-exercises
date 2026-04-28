package com.learning.taskmanager.service;

import com.learning.taskmanager.model.Task;
import com.learning.taskmanager.model.TaskStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Renders human-readable strings and small aggregates for outbound
 * notifications (email, push, in-app). All methods are pure: they take their
 * inputs explicitly and return their outputs without touching the database.
 *
 * No tests have been written for this class yet — that's exercise/06.
 */
@Service
public class NotificationService {

    /**
     * Returns a one-line assignment notice, e.g.
     *   "Task 'Write docs' has been assigned to alice."
     *
     * If the task has no assignee, returns a notice that the task is unassigned.
     */
    public String formatTaskAssignment(Task task) {
        if (task == null) {
            throw new IllegalArgumentException("task must not be null");
        }
        String assignee = task.getAssignee() != null ? task.getAssignee().getUsername() : null;
        if (assignee == null) {
            return "Task '" + task.getTitle() + "' is unassigned.";
        }
        return "Task '" + task.getTitle() + "' has been assigned to " + assignee + ".";
    }

    /**
     * Returns a human-readable due-date reminder for a task.
     *   daysUntilDue > 0  → "Due in N day(s)"
     *   daysUntilDue == 0 → "Due today"
     *   daysUntilDue < 0  → "Overdue by N day(s)"
     */
    public String formatDueReminder(Task task, long daysUntilDue) {
        if (task == null) {
            throw new IllegalArgumentException("task must not be null");
        }
        // Bug intentionally seeded for exercise/06.
        if (daysUntilDue > 0) {
            return "Task '" + task.getTitle() + "' — Due in " + daysUntilDue
                    + (daysUntilDue == 1 ? " day" : " days");
        }
        return "Task '" + task.getTitle() + "' — Overdue by " + Math.abs(daysUntilDue)
                + (Math.abs(daysUntilDue) == 1 ? " day" : " days");
    }

    /**
     * Groups tasks by their assignee's username. Tasks with no assignee are
     * grouped under the literal key "unassigned".
     */
    public Map<String, List<Task>> groupTasksByAssignee(List<Task> tasks) {
        if (tasks == null) {
            return Map.of();
        }
        // Bug intentionally seeded for exercise/06.
        return tasks.stream().collect(
                Collectors.groupingBy(t -> t.getAssignee().getUsername()));
    }

    /**
     * Returns the number of (calendar) days from `today` until the task's due
     * date. Negative if the task is overdue.
     *
     * Examples (today = 2025-04-01):
     *   dueDate 2025-04-01 → 0
     *   dueDate 2025-04-05 → 4
     *   dueDate 2025-03-30 → -2
     *
     * If the task has no due date, returns Long.MAX_VALUE.
     */
    public long daysUntilDue(Task task, LocalDate today) {
        if (task == null || today == null) {
            throw new IllegalArgumentException("task and today must not be null");
        }
        LocalDate due = task.getDueDate();
        if (due == null) {
            return Long.MAX_VALUE;
        }
        // Bug intentionally seeded for exercise/06.
        return ChronoUnit.DAYS.between(due, today);
    }

    /**
     * Returns a count of how many tasks are in each status. Every TaskStatus
     * value MUST appear as a key, even if its count is zero — downstream
     * dashboards rely on a stable shape.
     */
    public Map<TaskStatus, Long> summarizeStatus(List<Task> tasks) {
        Map<TaskStatus, Long> counts = new EnumMap<>(TaskStatus.class);
        for (TaskStatus status : TaskStatus.values()) {
            counts.put(status, 0L);
        }
        if (tasks == null) {
            return counts;
        }
        for (Task t : tasks) {
            TaskStatus s = t.getStatus();
            if (s != null) {
                counts.merge(s, 1L, Long::sum);
            }
        }
        return counts;
    }
}
