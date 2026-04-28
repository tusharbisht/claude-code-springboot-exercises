package com.learning.taskmanager.service;

import com.learning.taskmanager.model.Task;
import com.learning.taskmanager.model.TaskStatus;
import com.learning.taskmanager.model.User;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.fail;

/**
 * NotificationService is currently untested. Exercise 06: write the tests
 * below, then fix any bugs the tests surface in NotificationService itself.
 *
 * Each `fail(...)` is a placeholder. Replace the body of each test with real
 * assertions. The Javadoc above each method is the spec — read it carefully.
 *
 * Use Claude Code to scaffold these (e.g. "implement the body of
 * formatTaskAssignment_includesTitleAndAssignee_test based on the spec"),
 * but read the generated assertions critically before trusting them.
 */
class NotificationServiceTest {

    private final NotificationService service = new NotificationService();

    private static Task task(String title, User assignee, LocalDate dueDate, TaskStatus status) {
        Task t = new Task();
        t.setTitle(title);
        t.setAssignee(assignee);
        t.setDueDate(dueDate);
        if (status != null) t.setStatus(status);
        return t;
    }

    private static User user(String username) {
        return new User(username, username + "@example.com");
    }

    /**
     * formatTaskAssignment(task) where the task has an assignee returns
     *   "Task '<title>' has been assigned to <username>."
     */
    @Test
    void formatTaskAssignment_includesTitleAndAssignee() {
        fail("TODO(exercise-06): implement this test");
    }

    /**
     * formatTaskAssignment(task) where the task has no assignee returns
     *   "Task '<title>' is unassigned."
     */
    @Test
    void formatTaskAssignment_taskWithoutAssignee_returnsUnassignedNotice() {
        fail("TODO(exercise-06): implement this test");
    }

    /**
     * formatDueReminder(task, daysUntilDue):
     *   daysUntilDue > 0  → "Task '<title>' — Due in N day(s)"
     *   daysUntilDue == 0 → "Task '<title>' — Due today"
     *   daysUntilDue < 0  → "Task '<title>' — Overdue by N day(s)"
     *
     * Cover the boundary at zero AND the singular vs plural day(s) cases.
     */
    @Test
    void formatDueReminder_today_says_dueToday() {
        fail("TODO(exercise-06): implement this test");
    }

    @Test
    void formatDueReminder_inFuture_usesPluralOrSingular() {
        fail("TODO(exercise-06): implement this test");
    }

    @Test
    void formatDueReminder_overdue_says_overdueByN() {
        fail("TODO(exercise-06): implement this test");
    }

    /**
     * groupTasksByAssignee(tasks) returns a Map keyed by assignee username.
     *   - tasks with no assignee land under the key "unassigned"
     *   - a null input returns an empty map
     *   - tasks whose assignees share a username appear together
     */
    @Test
    void groupTasksByAssignee_unassignedBucketed_underLiteralKey() {
        fail("TODO(exercise-06): implement this test");
    }

    /**
     * daysUntilDue(task, today):
     *   dueDate after today  → POSITIVE number of days
     *   dueDate equal today  → 0
     *   dueDate before today → NEGATIVE number of days (overdue)
     *   dueDate is null      → Long.MAX_VALUE
     *
     * Use today = 2025-04-01 and a few hand-picked due dates.
     */
    @Test
    void daysUntilDue_signsAreCorrectAndZeroOnSameDay() {
        fail("TODO(exercise-06): implement this test");
    }

    /**
     * summarizeStatus(tasks) MUST return every TaskStatus value as a key,
     * even if its count is zero. Empty input ⇒ all zeros. Null input ⇒ all
     * zeros (don't NPE).
     */
    @Test
    void summarizeStatus_alwaysReturnsAllStatusKeys() {
        fail("TODO(exercise-06): implement this test");
    }

    // Helpers — these are usable from the test methods you'll write above.
    @SuppressWarnings("unused")
    private static List<Task> sampleTasks() {
        User alice = user("alice");
        User bob = user("bob");
        return List.of(
                task("a1", alice, LocalDate.of(2025, 4, 5), TaskStatus.TODO),
                task("a2", alice, LocalDate.of(2025, 4, 1), TaskStatus.IN_PROGRESS),
                task("b1", bob, LocalDate.of(2025, 4, 3), TaskStatus.DONE),
                task("orphan", null, null, null));
    }

    @SuppressWarnings("unused")
    private static Map<TaskStatus, Long> emptyStatusCounts() {
        return Map.of(TaskStatus.TODO, 0L, TaskStatus.IN_PROGRESS, 0L, TaskStatus.DONE, 0L);
    }
}
