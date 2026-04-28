package com.learning.taskmanager.grading;

import com.learning.taskmanager.model.Task;
import com.learning.taskmanager.model.TaskStatus;
import com.learning.taskmanager.model.User;
import com.learning.taskmanager.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Hidden grading suite for exercise/06-tests-from-scratch.
 *
 * The visible tests prove the learner has WRITTEN tests. This suite proves
 * the learner has FIXED the bugs those tests should have surfaced — by
 * exercising NotificationService directly.
 */
@DisplayName("[grading] Exercise 06 — tests from scratch")
class HiddenNotificationGradingTest {

    private NotificationService service;

    @BeforeEach
    void setUp() {
        service = new NotificationService();
    }

    private static Task task(String title, User assignee, LocalDate dueDate, TaskStatus status) {
        Task t = new Task();
        t.setTitle(title);
        t.setAssignee(assignee);
        t.setDueDate(dueDate);
        if (status != null) t.setStatus(status);
        return t;
    }

    @Test
    @DisplayName("formatTaskAssignment includes title and username for assigned task")
    void assignmentNoticeAssigned() {
        Task t = task("Write docs", new User("alice", "alice@example.com"), null, null);
        assertThat(service.formatTaskAssignment(t))
                .contains("Write docs")
                .contains("alice")
                .contains("assigned to alice");
    }

    @Test
    @DisplayName("formatTaskAssignment uses 'unassigned' wording when no assignee")
    void assignmentNoticeUnassigned() {
        Task t = task("Roam free", null, null, null);
        assertThat(service.formatTaskAssignment(t)).contains("unassigned");
    }

    @Test
    @DisplayName("formatDueReminder says 'Due today' for daysUntilDue == 0")
    void reminderToday() {
        Task t = task("Today", null, LocalDate.now(), null);
        assertThat(service.formatDueReminder(t, 0L)).contains("Due today");
    }

    @Test
    @DisplayName("formatDueReminder pluralises correctly for 1 vs N days")
    void reminderPluralisation() {
        Task t = task("X", null, null, null);
        assertThat(service.formatDueReminder(t, 1L)).contains("Due in 1 day");
        assertThat(service.formatDueReminder(t, 5L)).contains("Due in 5 days");
        assertThat(service.formatDueReminder(t, -1L)).contains("Overdue by 1 day");
        assertThat(service.formatDueReminder(t, -3L)).contains("Overdue by 3 days");
    }

    @Test
    @DisplayName("groupTasksByAssignee buckets unassigned under 'unassigned'")
    void groupingHandlesUnassigned() {
        User alice = new User("alice", "alice@example.com");
        List<Task> tasks = List.of(
                task("a", alice, null, null),
                task("b", null, null, null));
        Map<String, List<Task>> grouped = service.groupTasksByAssignee(tasks);
        assertThat(grouped).containsKey("alice");
        assertThat(grouped).containsKey("unassigned");
        assertThat(grouped.get("alice")).hasSize(1);
        assertThat(grouped.get("unassigned")).hasSize(1);
    }

    @Test
    @DisplayName("groupTasksByAssignee on null returns empty map")
    void groupingHandlesNullInput() {
        assertThat(service.groupTasksByAssignee(null)).isEmpty();
    }

    @Test
    @DisplayName("daysUntilDue is positive for future, zero for today, negative for past")
    void daysUntilDueSigns() {
        LocalDate today = LocalDate.of(2025, 4, 1);
        assertThat(service.daysUntilDue(task("today", null, today, null), today)).isZero();
        assertThat(service.daysUntilDue(task("future", null, today.plusDays(5), null), today)).isEqualTo(5);
        assertThat(service.daysUntilDue(task("past", null, today.minusDays(2), null), today)).isEqualTo(-2);
    }

    @Test
    @DisplayName("daysUntilDue returns Long.MAX_VALUE for null due date")
    void daysUntilDueNoDueDate() {
        assertThat(service.daysUntilDue(task("none", null, null, null), LocalDate.now())).isEqualTo(Long.MAX_VALUE);
    }

    @Test
    @DisplayName("summarizeStatus returns all TaskStatus keys, even with empty input")
    void summarizeAllKeys() {
        Map<TaskStatus, Long> counts = service.summarizeStatus(List.of());
        assertThat(counts).containsOnlyKeys(TaskStatus.values());
        assertThat(counts.values()).allMatch(v -> v == 0L);
    }

    @Test
    @DisplayName("summarizeStatus on null input returns all zero counts (no NPE)")
    void summarizeNullSafe() {
        Map<TaskStatus, Long> counts = service.summarizeStatus(null);
        assertThat(counts).containsOnlyKeys(TaskStatus.values());
        assertThat(counts.values()).allMatch(v -> v == 0L);
    }

    @Test
    @DisplayName("formatTaskAssignment rejects a null task")
    void assignmentRejectsNull() {
        assertThatThrownBy(() -> service.formatTaskAssignment(null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
