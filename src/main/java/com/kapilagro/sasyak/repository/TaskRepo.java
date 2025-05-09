package com.kapilagro.sasyak.repository;

import com.kapilagro.sasyak.model.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Repository
public class TaskRepo {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public TaskRepo(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // Row mapper for Task
    private final RowMapper<Task> taskRowMapper = (rs, rowNum) -> {
        return Task.builder()
                .taskId(rs.getInt("task_id"))
                .tenantId(UUID.fromString(rs.getString("tenant_id")))
                .createdById(rs.getInt("created_by_id"))
                .assignedToId(rs.getObject("assigned_to_id", Integer.class)) // Can be null
                .taskType(rs.getString("task_type"))
                .detailsJson(rs.getString("details_json"))
                .imagesJson(rs.getString("images"))
                .description(rs.getString("description"))
                .implementationJson(rs.getString("implementation"))
                .status(rs.getString("status"))
                .advice(rs.getString("advice")) // Get advice as plain text
                .adviceCreatedAt(rs.getTimestamp("advice_created_at") != null ?
                        rs.getTimestamp("advice_created_at").toLocalDateTime() : null) // Get advice creation date
                .createdAt(rs.getTimestamp("created_at").toLocalDateTime())
                .updatedAt(rs.getTimestamp("updated_at").toLocalDateTime())
                .build();
    };

    // Count tasks by tenant
    public int countByTenantId(UUID tenantId) {
        String sql = "SELECT COUNT(*) FROM tasks WHERE tenant_id = ?";
        return jdbcTemplate.queryForObject(sql, Integer.class, tenantId);
    }


    // Get task status breakdown
    public Map<String, Integer> getTaskStatusBreakdown(UUID tenantId) {
        String sql = "SELECT status, COUNT(*) as count FROM tasks WHERE tenant_id = ? GROUP BY status";
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, tenantId);

        Map<String, Integer> statusBreakdown = new java.util.HashMap<>();
        for (Map<String, Object> row : rows) {
            String status = (String) row.get("status");
            Integer count = ((Number) row.get("count")).intValue();
            statusBreakdown.put(status, count);
        }

        return statusBreakdown;
    }

    // Count recent tasks
    public int countRecentByTenantId(UUID tenantId, int days) {
        String sql = "SELECT COUNT(*) FROM tasks WHERE tenant_id = ? AND created_at >= CURRENT_TIMESTAMP - INTERVAL '" + days + " days'";
        return jdbcTemplate.queryForObject(sql, Integer.class, tenantId);
    }

    // Save new task
    public int save(Task task) {
        String sql = "INSERT INTO tasks " +
                "(tenant_id, created_by_id, assigned_to_id, task_type, details_json, images, description, implementation, status, advice) " +
                "VALUES (?, ?, ?, ?, ?::jsonb, ?::jsonb, ?, ?::jsonb, ?, ?)";

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setObject(1, task.getTenantId());
            ps.setInt(2, task.getCreatedById());

            if (task.getAssignedToId() != null) {
                ps.setInt(3, task.getAssignedToId());
            } else {
                ps.setNull(3, java.sql.Types.INTEGER);
            }

            ps.setString(4, task.getTaskType());
            ps.setString(5, task.getDetailsJson() != null ? task.getDetailsJson() : "{}");
            ps.setString(6, task.getImagesJson() != null ? task.getImagesJson() : "[]");
            ps.setString(7, task.getDescription());
            ps.setString(8, task.getImplementationJson() != null ? task.getImplementationJson() : "{}");
            ps.setString(9, task.getStatus() != null ? task.getStatus() : "submitted");
            ps.setString(10, task.getAdvice() != null ? task.getAdvice() : ""); // Plain text advice

            return ps;
        }, keyHolder);

        Map<String, Object> keys = keyHolder.getKeys();
        if (keys != null) {
            return (int) keys.get("task_id");
        } else {
            throw new IllegalStateException("Failed to retrieve task_id after insertion.");
        }
    }

    // Get task by ID
    public Optional<Task> getById(int taskId) {
        String sql = "SELECT * FROM tasks WHERE task_id = ?";
        try {
            return Optional.of(jdbcTemplate.queryForObject(sql, taskRowMapper, taskId));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    // Get tasks by tenant ID
    public List<Task> getByTenantId(UUID tenantId, int page, int size) {
        String sql = "SELECT * FROM tasks WHERE tenant_id = ? ORDER BY created_at DESC LIMIT ? OFFSET ?";
        int offset = page * size;
        return jdbcTemplate.query(sql, taskRowMapper, tenantId, size, offset);
    }

    // Get tasks created by a user
    public List<Task> getByCreatedBy(UUID tenantId, int userId, int page, int size) {
        String sql = "SELECT * FROM tasks WHERE tenant_id = ? AND created_by_id = ? ORDER BY created_at DESC LIMIT ? OFFSET ?";
        int offset = page * size;
        return jdbcTemplate.query(sql, taskRowMapper, tenantId, userId, size, offset);
    }

    // Get tasks assigned to a user
    public List<Task> getByAssignedTo(UUID tenantId, int userId, int page, int size) {
        String sql = "SELECT * FROM tasks WHERE tenant_id = ? AND assigned_to_id = ? ORDER BY created_at DESC LIMIT ? OFFSET ?";
        int offset = page * size;
        return jdbcTemplate.query(sql, taskRowMapper, tenantId, userId, size, offset);
    }

    // Get tasks by status
    public List<Task> getByStatus(UUID tenantId, String status, int page, int size) {
        String sql = "SELECT * FROM tasks WHERE tenant_id = ? AND UPPER(status) = UPPER(?) ORDER BY created_at DESC LIMIT ? OFFSET ?";
        int offset = page * size;
        return jdbcTemplate.query(sql, taskRowMapper, tenantId, status, size, offset);
    }

    // Update task status
    public boolean updateStatus(int taskId, String status) {
        String sql = "UPDATE tasks SET status = ?, updated_at = CURRENT_TIMESTAMP WHERE task_id = ?";
        int updated = jdbcTemplate.update(sql, status, taskId);
        return updated > 0;
    }

    // Update task implementation
    public boolean updateImplementation(int taskId, String implementationJson) {
        String sql = "UPDATE tasks SET implementation = ?::jsonb, status = 'implemented', updated_at = CURRENT_TIMESTAMP WHERE task_id = ?";
        int updated = jdbcTemplate.update(sql, implementationJson, taskId);
        return updated > 0;
    }

    // Assign task to user
    public boolean assignTask(int taskId, int assignedToId) {
        String sql = "UPDATE tasks SET assigned_to_id = ?, updated_at = CURRENT_TIMESTAMP WHERE task_id = ?";
        int updated = jdbcTemplate.update(sql, assignedToId, taskId);
        return updated > 0;
    }

    // Count tasks created by a user
    public int countByCreatedBy(UUID tenantId, int userId) {
        String sql = "SELECT COUNT(*) FROM tasks WHERE tenant_id = ? AND created_by_id = ?";
        return jdbcTemplate.queryForObject(sql, Integer.class, tenantId, userId);
    }

    // Count tasks assigned to a user
    public int countByAssignedTo(UUID tenantId, int userId) {
        String sql = "SELECT COUNT(*) FROM tasks WHERE tenant_id = ? AND assigned_to_id = ?";
        return jdbcTemplate.queryForObject(sql, Integer.class, tenantId, userId);
    }

    // Get task counts by type
    public Map<String, Integer> getTaskCountsByType(UUID tenantId) {
        String sql = "SELECT task_type, COUNT(*) as count FROM tasks WHERE tenant_id = ? GROUP BY task_type";
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, tenantId);

        Map<String, Integer> typeCounts = new java.util.HashMap<>();
        for (Map<String, Object> row : rows) {
            String type = (String) row.get("task_type");
            Integer count = ((Number) row.get("count")).intValue();
            typeCounts.put(type, count);
        }

        return typeCounts;
    }

    // Get task counts by user
    public Map<String, Integer> getTaskCountsByUser(UUID tenantId) {
        String sql = "SELECT u.name, COUNT(t.task_id) as count " +
                "FROM tasks t JOIN users u ON t.created_by_id = u.user_id " +
                "WHERE t.tenant_id = ? GROUP BY u.name";
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, tenantId);

        Map<String, Integer> userCounts = new java.util.HashMap<>();
        for (Map<String, Object> row : rows) {
            String name = (String) row.get("name");
            Integer count = ((Number) row.get("count")).intValue();
            userCounts.put(name, count);
        }

        return userCounts;
    }

    // Get average task completion time by type
    public Map<String, Double> getAvgCompletionTimeByType(UUID tenantId) {
        String sql = "SELECT task_type, AVG(EXTRACT(EPOCH FROM (updated_at - created_at))/86400) as avg_days " +
                "FROM tasks WHERE tenant_id = ? AND status = 'implemented' GROUP BY task_type";
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, tenantId);

        Map<String, Double> avgTimes = new java.util.HashMap<>();
        for (Map<String, Object> row : rows) {
            String type = (String) row.get("task_type");
            Double avgDays = ((Number) row.get("avg_days")).doubleValue();
            avgTimes.put(type, avgDays);
        }

        return avgTimes;
    }

    // Get tasks by task type
    public List<Task> getByTaskType(UUID tenantId, String taskType, int page, int size) {
        String sql = "SELECT * FROM tasks WHERE tenant_id = ? AND UPPER(task_type) = UPPER(?) ORDER BY created_at DESC LIMIT ? OFFSET ?";
        int offset = page * size;
        return jdbcTemplate.query(sql, taskRowMapper, tenantId, taskType, size, offset);
    }

    // Count tasks by task type (for pagination total)
    public int countByTaskType(UUID tenantId, String taskType) {
        String sql = "SELECT COUNT(*) FROM tasks WHERE tenant_id = ? AND UPPER(task_type) = UPPER(?)";
        return jdbcTemplate.queryForObject(sql, Integer.class, tenantId, taskType);
    }

    // Get tasks completed per day for date range
    public List<Map<String, Object>> getTasksCompletedPerDay(UUID tenantId, LocalDateTime startDate, LocalDateTime endDate) {
        String sql = "SELECT DATE(updated_at) as date, COUNT(*) as count FROM tasks " +
                "WHERE tenant_id = ? AND status = 'implemented' AND updated_at BETWEEN ? AND ? " +
                "GROUP BY DATE(updated_at) ORDER BY date";
        return jdbcTemplate.queryForList(sql, tenantId, Timestamp.valueOf(startDate), Timestamp.valueOf(endDate));
    }

    // Get tasks created per day for date range
    public List<Map<String, Object>> getTasksCreatedPerDay(UUID tenantId, LocalDateTime startDate, LocalDateTime endDate) {
        String sql = "SELECT DATE(created_at) as date, COUNT(*) as count FROM tasks " +
                "WHERE tenant_id = ? AND created_at BETWEEN ? AND ? " +
                "GROUP BY DATE(created_at) ORDER BY date";
        return jdbcTemplate.queryForList(sql, tenantId, Timestamp.valueOf(startDate), Timestamp.valueOf(endDate));
    }

    // Get average completion time by user
    public Map<String, Double> getAvgCompletionTimeByUser(UUID tenantId) {
        String sql = "SELECT u.name, AVG(EXTRACT(EPOCH FROM (t.updated_at - t.created_at))/86400) as avg_days " +
                "FROM tasks t JOIN users u ON t.assigned_to_id = u.user_id " +
                "WHERE t.tenant_id = ? AND t.status = 'implemented' GROUP BY u.name";
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, tenantId);

        Map<String, Double> avgTimes = new java.util.HashMap<>();
        for (Map<String, Object> row : rows) {
            String name = (String) row.get("name");
            Double avgDays = ((Number) row.get("avg_days")).doubleValue();
            avgTimes.put(name, avgDays);
        }

        return avgTimes;
    }

    // Get task completion rate by user
    public Map<String, Object> getTaskCompletionRateByUser(UUID tenantId) {
        String sql = "SELECT u.name, " +
                "COUNT(CASE WHEN t.status = 'implemented' THEN 1 ELSE NULL END) as completed, " +
                "COUNT(*) as total, " +
                "ROUND(COUNT(CASE WHEN t.status = 'implemented' THEN 1 ELSE NULL END)::NUMERIC / COUNT(*)::NUMERIC * 100, 2) as rate " +
                "FROM tasks t JOIN users u ON t.assigned_to_id = u.user_id " +
                "WHERE t.tenant_id = ? GROUP BY u.name";
        return jdbcTemplate.queryForList(sql, tenantId).stream()
                .collect(Collectors.toMap(
                        row -> (String) row.get("name"),
                        row -> {
                            Map<String, Object> result = new HashMap<>();
                            result.put("completed", ((Number) row.get("completed")).intValue());
                            result.put("total", ((Number) row.get("total")).intValue());
                            result.put("rate", ((Number) row.get("rate")).doubleValue());
                            return result;
                        }
                ));
    }
}