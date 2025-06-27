package com.kapilagro.sasyak.services;

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
    /**
     * Creates a notification for a specific user
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
     * Notifies managers when a task is created by a supervisor
     *
     * @param tenantId The tenant ID
     * @param taskId The task ID that was created
     * @param taskDescription Task description for the notification
     * @param supervisorId The ID of the supervisor who created the task
     * @param supervisorName The name of the supervisor for the notification message
     */
    @Transactional
    public void notifyManagersOfSupervisorTask(UUID tenantId, int taskId, String taskDescription,
                                               int supervisorId, String supervisorName) {
        // Find all managers in the same tenant
        String managerSql = "SELECT user_id FROM users WHERE tenant_id = ? AND role = 'ROLE_MANAGER'";
        List<Integer> managerIds = jdbcTemplate.queryForList(managerSql, Integer.class, tenantId);

        String title = "New Task Created by Supervisor";
        String message = String.format(
                "A new task '%s' has been created by supervisor %s. Task ID: %d",
                taskDescription,
                supervisorName,
                taskId
        );

        // Create notification for each manager
        for (Integer managerId : managerIds) {
            createNotification(tenantId, managerId, title, message, taskId);
        }
    }

    /**
     * Notifies a user when a task is assigned to them
     *
     * @param tenantId The tenant ID
     * @param assignedUserId The user ID to whom the task was assigned
     * @param taskId The task ID that was assigned
     * @param taskDescription Task description for the notification
     * @param assignedByName The name of the user who assigned the task
     */
    @Transactional
    public void notifyTaskAssignment(UUID tenantId, int assignedUserId, int taskId,
                                     String taskDescription, String assignedByName) {
        String title = "New Task Assignment";
        String message = String.format(
                "You have been assigned a new task '%s' by %s. Task ID: %d",
                taskDescription,
                assignedByName,
                taskId
        );

        createNotification(tenantId, assignedUserId, title, message, taskId);
    }

    /**
     * Notifies the task creator when a task status is updated
     *
     * @param tenantId The tenant ID
     * @param creatorUserId The user ID who created the task
     * @param taskId The task ID that was updated
     * @param taskDescription Task description for the notification
     * @param newStatus The new status of the task
     * @param updatedByName The name of the user who updated the task
     */
    @Transactional
    public void notifyTaskStatusChange(UUID tenantId, int creatorUserId, int taskId,
                                       String taskDescription, String newStatus, String updatedByName) {
        String title = "Task Status Updated";
        String message = String.format(
                "The status of task '%s' has been updated to '%s' by %s",
                taskDescription,
                newStatus,
                updatedByName
        );

        createNotification(tenantId, creatorUserId, title, message, taskId);
    }

    /**
     * Notifies the assignee when advice is added to their task
     *
     * @param tenantId The tenant ID
     * @param assigneeUserId The user ID to whom the task is assigned
     * @param taskId The task ID that received advice
     * @param taskDescription Task description for the notification
     * @param adviceGiverName The name of the user who gave the advice
     */
    @Transactional
    public void notifyTaskAdvice(UUID tenantId, int assigneeUserId, int taskId,
                                 String taskDescription, String adviceGiverName) {
        String title = "New Advice on Your Task";
        String message = String.format(
                "You have received new advice on task '%s' from %s",
                taskDescription,
                adviceGiverName
        );

        createNotification(tenantId, assigneeUserId, title, message, taskId);
    }

    /**
     * Marks a notification as read
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
     * Marks all notifications as read for a user
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
     * Gets the count of unread notifications for a user
     *
     * @param userId The user ID
     * @return The count of unread notifications
     */
    public int getUnreadNotificationsCount(int userId) {
        String sql = "SELECT COUNT(*) FROM notifications WHERE user_id = ? AND is_read = false";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, userId);
        return count != null ? count : 0;
    }
}