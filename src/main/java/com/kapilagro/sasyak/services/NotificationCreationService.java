package com.kapilagro.sasyak.services;

import com.kapilagro.sasyak.model.Task;
import com.kapilagro.sasyak.model.User;
import com.kapilagro.sasyak.repository.UserRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class NotificationCreationService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private UserRepo userRepo;

    /**
     * Create a notification for a specific user
     *
     * @param tenantId The tenant ID
     * @param userId The user ID to notify
     * @param title Notification title
     * @param message Notification message
     * @param taskId Optional task ID (can be null)
     * @return The ID of the created notification
     */
    @Transactional
    public int createNotification(UUID tenantId, int userId, String title, String message, Integer taskId) {
        String sql = "INSERT INTO notifications (tenant_id, user_id, title, message, task_id) " +
                "VALUES (?, ?, ?, ?, ?) RETURNING notification_id";

        Integer notificationId = jdbcTemplate.queryForObject(sql, Integer.class,
                tenantId, userId, title, message, taskId);

        return notificationId != null ? notificationId : -1;
    }

    /**
     * Notify all managers when a task is created by a supervisor
     *
     * @param task The task that was created
     * @param createdByUser The user who created the task
     */
    @Transactional
    public void notifyManagersOfTaskCreatedBySupervisor(Task task, User createdByUser) {
        // Check if the user who created the task is a supervisor
        if (createdByUser.getRole().equals("ROLE_SUPERVISOR")) {
            // Find all managers in the same tenant
            List<User> managers = userRepo.findByTenantIdAndRole(
                    createdByUser.getTenantId(), "ROLE_MANAGER");

            String title = "New Task Created by Supervisor";
            String message = String.format(
                    "A new task '%s' has been created by supervisor %s %s. Task ID: %d",
                    task.getDescription(),
                    createdByUser.getFirstName(),
                    createdByUser.getLastName(),
                    task.getTaskId()
            );

            // Create notification for each manager
            for (User manager : managers) {
                createNotification(
                        createdByUser.getTenantId(),
                        manager.getUserId(),
                        title,
                        message,
                        task.getTaskId()
                );
            }
        }
    }

    /**
     * Notify a user when a task is assigned to them
     *
     * @param task The task that was assigned
     * @param assignedUser The user to whom the task was assigned
     * @param assignedByUser The user who assigned the task
     */
    @Transactional
    public void notifyTaskAssignment(Task task, User assignedUser, User assignedByUser) {
        String title = "New Task Assignment";
        String message = String.format(
                "You have been assigned a new task '%s' by %s %s. Task ID: %d",
                task.getDescription(),
                assignedByUser.getFirstName(),
                assignedByUser.getLastName(),
                task.getTaskId()
        );

        createNotification(
                assignedUser.getTenantId(),
                assignedUser.getUserId(),
                title,
                message,
                task.getTaskId()
        );
    }

    /**
     * Notify the task creator when a task status is updated
     *
     * @param task The updated task
     * @param updatedByUser The user who updated the task
     */
    @Transactional
    public void notifyTaskStatusChange(Task task, User updatedByUser) {
        // Notify the task creator
        if (task.getCreatedBy() != updatedByUser.getUserId()) {
            User taskCreator = userRepo.findById(task.getCreatedBy()).orElse(null);
            if (taskCreator != null) {
                String title = "Task Status Updated";
                String message = String.format(
                        "The status of task '%s' has been updated to '%s' by %s %s",
                        task.getDescription(),
                        task.getStatus(),
                        updatedByUser.getFirstName(),
                        updatedByUser.getLastName()
                );

                createNotification(
                        taskCreator.getTenantId(),
                        taskCreator.getUserId(),
                        title,
                        message,
                        task.getTaskId()
                );
            }
        }
    }

    /**
     * Notify the assignee when advice is added to their task
     *
     * @param task The task that received advice
     * @param adviceText The advice text
     * @param adviceGivenByUser The user who gave the advice
     */
    @Transactional
    public void notifyTaskAdvice(Task task, String adviceText, User adviceGivenByUser) {
        if (task.getAssignedTo() != null) {
            User assignee = userRepo.findById(task.getAssignedTo()).orElse(null);
            if (assignee != null) {
                String title = "New Advice on Your Task";
                String message = String.format(
                        "You have received new advice on task '%s' from %s %s",
                        task.getDescription(),
                        adviceGivenByUser.getFirstName(),
                        adviceGivenByUser.getLastName()
                );

                createNotification(
                        assignee.getTenantId(),
                        assignee.getUserId(),
                        title,
                        message,
                        task.getTaskId()
                );
            }
        }
    }

    /**
     * Mark a notification as read
     *
     * @param notificationId The notification ID to mark as read
     * @return True if successful, false otherwise
     */
    @Transactional
    public boolean markNotificationAsRead(int notificationId) {
        String sql = "UPDATE notifications SET is_read = true WHERE notification_id = ?";
        int updated = jdbcTemplate.update(sql, notificationId);
        return updated > 0;
    }

    /**
     * Mark all notifications as read for a user
     *
     * @param userId The user ID
     * @return The number of notifications marked as read
     */
    @Transactional
    public int markAllNotificationsAsRead(int userId) {
        String sql = "UPDATE notifications SET is_read = true WHERE user_id = ? AND is_read = false";
        return jdbcTemplate.update(sql, userId);
    }

    /**
     * Get the count of unread notifications for a user
     *
     * @param userId The user ID
     * @return The count of unread notifications
     */
    public int getUnreadNotificationsCount(int userId) {
        String sql = "SELECT COUNT(*) FROM notifications WHERE user_id = ? AND is_read = false";
        return jdbcTemplate.queryForObject(sql, Integer.class, userId);
    }
}
