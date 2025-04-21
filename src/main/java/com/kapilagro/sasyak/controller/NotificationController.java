// File: NotificationController.java
package com.kapilagro.sasyak.controller;

import com.kapilagro.sasyak.model.*;
import com.kapilagro.sasyak.services.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController

@RequestMapping("/api/notifications")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // Helper method to get the current user from the authentication context
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return (User) authentication.getPrincipal();
    }

    // Get unread notification count for current user
    @GetMapping("/unread/count")
    public ResponseEntity<?> getUnreadNotificationCount() {
        try {
            User currentUser = getCurrentUser();
            int count = notificationService.getUnreadNotificationsCount(currentUser.getUserId());
            return ResponseEntity.ok(Map.of("count", count));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error retrieving notification count: " + e.getMessage());
        }
    }

    // Get notifications for current user
    @GetMapping
    public ResponseEntity<?> getNotifications(
            @RequestParam(defaultValue = "false") boolean onlyUnread,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            User currentUser = getCurrentUser();
            int userId = currentUser.getUserId();

            // Create a SQL query with optional filter for unread
            String sql = "SELECT * FROM notifications WHERE user_id = ?";
            if (onlyUnread) {
                sql += " AND is_read = false";
            }
            sql += " ORDER BY created_at DESC LIMIT ? OFFSET ?";

            // Define a row mapper for notifications
            RowMapper<Map<String, Object>> rowMapper = (rs, rowNum) -> {
                Map<String, Object> notification = Map.of(
                        "id", rs.getInt("notification_id"),
                        "title", rs.getString("title"),
                        "message", rs.getString("message"),
                        "taskId", rs.getObject("task_id", Integer.class),
                        "isRead", rs.getBoolean("is_read"),
                        "createdAt", rs.getTimestamp("created_at").toLocalDateTime()
                );
                return notification;
            };

            int offset = page * size;
            List<Map<String, Object>> notifications = jdbcTemplate.query(
                    sql, rowMapper, userId, size, offset
            );

            // Count total notifications
            String countSql = "SELECT COUNT(*) FROM notifications WHERE user_id = ?";
            if (onlyUnread) {
                countSql += " AND is_read = false";
            }
            int totalCount = jdbcTemplate.queryForObject(countSql, Integer.class, userId);

            return ResponseEntity.ok(Map.of(
                    "notifications", notifications,
                    "totalCount", totalCount,
                    "currentPage", page,
                    "totalPages", (int) Math.ceil((double) totalCount / size)
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error retrieving notifications: " + e.getMessage());
        }
    }

    // Mark a notification as read
    @PutMapping("/{notificationId}/read")
    public ResponseEntity<?> markNotificationAsRead(@PathVariable int notificationId) {
        try {
            User currentUser = getCurrentUser();
            int userId = currentUser.getUserId();

            // Verify the notification belongs to the current user
            String verifySql = "SELECT COUNT(*) FROM notifications WHERE notification_id = ? AND user_id = ?";
            int count = jdbcTemplate.queryForObject(verifySql, Integer.class, notificationId, userId);

            if (count == 0) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("You don't have permission to access this notification");
            }

            notificationService.markNotificationAsRead(notificationId);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error marking notification as read: " + e.getMessage());
        }
    }

    // Mark all notifications as read
    @PutMapping("/read-all")
    public ResponseEntity<?> markAllNotificationsAsRead() {
        try {
            User currentUser = getCurrentUser();
            int userId = currentUser.getUserId();

            notificationService.markAllNotificationsAsRead(userId);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error marking notifications as read: " + e.getMessage());
        }
    }
}