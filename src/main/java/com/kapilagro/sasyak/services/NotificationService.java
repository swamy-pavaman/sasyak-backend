package com.kapilagro.sasyak.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class NotificationService {
    private final JdbcTemplate jdbcTemplate;
    @Autowired
    public NotificationService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // Create notification for task assignment
    public void createTaskAssignmentNotification(UUID tenantId, int userId, int taskId, String title, String message) {
        String sql = "INSERT INTO notifications (tenant_id, user_id, title, message, task_id) VALUES (?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql, tenantId, userId, title, message, taskId);
    }

    // Create notification for task status update
    public void createTaskStatusNotification(UUID tenantId, int userId, int taskId, String title, String message) {
        String sql = "INSERT INTO notifications (tenant_id, user_id, title, message, task_id) VALUES (?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql, tenantId, userId, title, message, taskId);
    }

    // Create notification for task implementation
    public void createTaskImplementationNotification(UUID tenantId, int userId, int taskId, String title, String message) {
        String sql = "INSERT INTO notifications (tenant_id, user_id, title, message, task_id) VALUES (?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql, tenantId, userId, title, message, taskId);
    }

    // Create notification for new advice
    public void createAdviceNotification(UUID tenantId, int userId, int taskId, String title, String message) {
        String sql = "INSERT INTO notifications (tenant_id, user_id, title, message, task_id) VALUES (?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql, tenantId, userId, title, message, taskId);
    }

    // Get unread notifications count for a user
    public int getUnreadNotificationsCount(int userId) {
        String sql = "SELECT COUNT(*) FROM notifications WHERE user_id = ? AND is_read = false";
        return jdbcTemplate.queryForObject(sql, Integer.class, userId);
    }

    // Mark a notification as read
    public void markNotificationAsRead(int notificationId) {
        String sql = "UPDATE notifications SET is_read = true WHERE notification_id = ?";
        jdbcTemplate.update(sql, notificationId);
    }

    // Mark all notifications for a user as read
    public void markAllNotificationsAsRead(int userId) {
        String sql = "UPDATE notifications SET is_read = true WHERE user_id = ?";
        jdbcTemplate.update(sql, userId);
    }


}