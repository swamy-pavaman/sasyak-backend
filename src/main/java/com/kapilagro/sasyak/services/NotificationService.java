package com.kapilagro.sasyak.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class NotificationService {
    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public NotificationService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // Create notification for task assignment
    @Transactional
    public void createTaskAssignmentNotification(UUID tenantId, int userId, int taskId, String title, String message) {
        String sql = "INSERT INTO notifications (tenant_id, user_id, title, message, task_id) VALUES (?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql, tenantId, userId, title, message, taskId);
    }

    // Create notification for task status update
    @Transactional
    public void createTaskStatusNotification(UUID tenantId, int userId, int taskId, String title, String message) {
        String sql = "INSERT INTO notifications (tenant_id, user_id, title, message, task_id) VALUES (?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql, tenantId, userId, title, message, taskId);
    }

    // Create notification for task implementation
    @Transactional
    public void createTaskImplementationNotification(UUID tenantId, int userId, int taskId, String title, String message) {
        String sql = "INSERT INTO notifications (tenant_id, user_id, title, message, task_id) VALUES (?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql, tenantId, userId, title, message, taskId);
    }

    // Create notification for new advice
    @Transactional
    public void createAdviceNotification(UUID tenantId, int userId, int taskId, String title, String message) {
        String sql = "INSERT INTO notifications (tenant_id, user_id, title, message, task_id) VALUES (?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql, tenantId, userId, title, message, taskId);
    }

    // Notify managers when a task is created by a supervisor
    @Transactional
    public void notifySupervisorTaskCreation(UUID tenantId, int taskId, String taskType,
                                             String taskDescription, int supervisorId, String supervisorName) {
        System.out.println("==== NOTIFY SUPERVISOR TASK CREATION START ====");
        try {
            // Find all managers in the same tenant
            System.out.println("Looking for managers in tenant: " + tenantId);
            String managerSql = "SELECT user_id FROM users WHERE tenant_id = ? AND role = 'MANAGER'";
            List<Integer> managerIds = jdbcTemplate.queryForList(managerSql, Integer.class, tenantId);
            System.out.println("Found " + managerIds.size() + " managers");

            String title = "New Task Created by Supervisor";
            String message = "Supervisor " + supervisorName + " has created a new task: " + taskDescription;

            // Create notification for each manager
            for (Integer managerId : managerIds) {
                System.out.println("Creating notification for manager ID: " + managerId);
                String sql = "INSERT INTO notifications (tenant_id, user_id, title, message, task_id) VALUES (?, ?, ?, ?, ?)";
                int rowsAffected = jdbcTemplate.update(sql, tenantId, managerId, title, message, taskId);
                System.out.println("Notification created, rows affected: " + rowsAffected);
            }
        } catch (Exception e) {
            System.out.println("Error in notifySupervisorTaskCreation: " + e.getMessage());
            e.printStackTrace();
        }
        System.out.println("==== NOTIFY SUPERVISOR TASK CREATION END ====");
    }

    @Transactional
    public void notifySupervisorManagerOfTaskCreation(UUID tenantId, int taskId, String taskType,
                                                      String taskDescription, int supervisorId, String supervisorName) {
        System.out.println("==== NOTIFY SUPERVISOR'S MANAGER START ====");
        try {
            // Print all params for debugging
            System.out.println("Parameters:");
            System.out.println("- tenantId: " + tenantId);
            System.out.println("- taskId: " + taskId);
            System.out.println("- taskType: " + taskType);
            System.out.println("- supervisorId: " + supervisorId);
            System.out.println("- supervisorName: " + supervisorName);

            // Find the supervisor's manager
            System.out.println("Looking for manager of supervisor ID: " + supervisorId);
            String managerSql = "SELECT manager_id FROM users WHERE user_id = ? AND manager_id IS NOT NULL";
            Integer managerId = null;
            try {
                managerId = jdbcTemplate.queryForObject(managerSql, Integer.class, supervisorId);
                System.out.println("Manager ID found: " + managerId);
            } catch (Exception e) {
                System.out.println("Error finding manager: " + e.getMessage());
                // Try to debug by looking at the user record
                try {
                    String userSql = "SELECT * FROM users WHERE user_id = ?";
                    Map<String, Object> userData = jdbcTemplate.queryForMap(userSql, supervisorId);
                    System.out.println("User data found:");
                    for (Map.Entry<String, Object> entry : userData.entrySet()) {
                        System.out.println("  " + entry.getKey() + ": " + entry.getValue());
                    }
                } catch (Exception ex) {
                    System.out.println("Could not query user data: " + ex.getMessage());
                }
            }

            // If manager exists, create notification
            if (managerId != null) {
                String title = "Task Created by Your Team Member";
                String message = supervisorName + " has created a new '" + taskType + "' task. Click here to see task and give advice.";

                System.out.println("Creating notification with message: " + message);
                String sql = "INSERT INTO notifications (tenant_id, user_id, title, message, task_id) VALUES (?, ?, ?, ?, ?)";
                int rowsAffected = jdbcTemplate.update(sql, tenantId, managerId, title, message, taskId);
                System.out.println("Notification created, rows affected: " + rowsAffected);
            } else {
                System.out.println("No manager found for supervisor ID: " + supervisorId);
                // Try to find all users with role manager
                try {
                    String allManagersSql = "SELECT user_id, name FROM users WHERE role = 'MANAGER' AND tenant_id = ?";
                    List<Map<String, Object>> managers = jdbcTemplate.queryForList(allManagersSql, tenantId);
                    System.out.println("All managers in the system (" + managers.size() + "):");
                    for (Map<String, Object> manager : managers) {
                        System.out.println("  ID: " + manager.get("user_id") + ", Name: " + manager.get("name"));
                    }
                } catch (Exception e) {
                    System.out.println("Error listing managers: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            System.out.println("Error in notifySupervisorManagerOfTaskCreation: " + e.getMessage());
            e.printStackTrace();
        }
        System.out.println("==== NOTIFY SUPERVISOR'S MANAGER END ====");
    }

    // Get unread notifications count for a user
    public int getUnreadNotificationsCount(int userId) {
        String sql = "SELECT COUNT(*) FROM notifications WHERE user_id = ? AND is_read = false";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, userId);
        return count != null ? count : 0; // Handle potential null value
    }

    // Mark a notification as read
    @Transactional
    public void markNotificationAsRead(int notificationId) {
        String sql = "UPDATE notifications SET is_read = true WHERE notification_id = ?";
        jdbcTemplate.update(sql, notificationId);
    }

    // Mark all notifications for a user as read
    @Transactional
    public void markAllNotificationsAsRead(int userId) {
        String sql = "UPDATE notifications SET is_read = true WHERE user_id = ?";
        jdbcTemplate.update(sql, userId);
    }

    // Check if a user is a supervisor
    public boolean isSupervisor(int userId) {
        String sql = "SELECT COUNT(*) FROM users WHERE user_id = ? AND role = 'ROLE_SUPERVISOR'";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, userId);
        return count != null && count > 0;
    }

    // Get user info for notification
    public String getUserName(int userId) {
        String sql = "SELECT CONCAT(first_name, ' ', last_name) FROM users WHERE user_id = ?";
        return jdbcTemplate.queryForObject(sql, String.class, userId);
    }

    // Batch create notifications for multiple users
    @Transactional
    public void createBatchNotifications(UUID tenantId, List<Integer> userIds, String title, String message, Integer taskId) {
        for (Integer userId : userIds) {
            String sql = "INSERT INTO notifications (tenant_id, user_id, title, message, task_id) VALUES (?, ?, ?, ?, ?)";
            jdbcTemplate.update(sql, tenantId, userId, title, message, taskId);
        }
    }
}