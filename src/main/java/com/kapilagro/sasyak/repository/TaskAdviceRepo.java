package com.kapilagro.sasyak.repository;

import com.kapilagro.sasyak.model.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Repository
public class TaskAdviceRepo {

    private final JdbcTemplate jdbcTemplate;

    // Row mapper for Task - focused on advice-related fields
    private final RowMapper<Task> taskRowMapper = (rs, rowNum) -> {
        return Task.builder()
                .taskId(rs.getInt("task_id"))
                .tenantId(UUID.fromString(rs.getString("tenant_id")))
                .createdById(rs.getInt("created_by_id"))
                .assignedToId(rs.getObject("assigned_to_id", Integer.class))
                .taskType(rs.getString("task_type"))
                .description(rs.getString("description"))
                .status(rs.getString("status"))
                .advice(rs.getString("advice"))
                .adviceCreatedAt(rs.getTimestamp("advice_created_at") != null ?
                        rs.getTimestamp("advice_created_at").toLocalDateTime() : null)
                .createdAt(rs.getTimestamp("created_at").toLocalDateTime())
                .updatedAt(rs.getTimestamp("updated_at").toLocalDateTime())
                .build();
    };

    @Autowired
    public TaskAdviceRepo(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // Rename this:
    public int countByTenantId(UUID tenantId) {
        String sql = "SELECT COUNT(*) FROM tasks WHERE tenant_id = ? AND advice IS NOT NULL AND advice != ''";
        return jdbcTemplate.queryForObject(sql, Integer.class, tenantId);
    }


    // Get task by ID if advice is present
    public Task getTaskByIdIfHasAdvice(int taskId) {
        String sql = "SELECT * FROM tasks WHERE task_id = ? AND advice IS NOT NULL AND advice != ''";
        List<Task> results = jdbcTemplate.query(sql, taskRowMapper, taskId);
        return results.isEmpty() ? null : results.get(0);
    }


    // Update task advice
    public boolean updateAdvice(int taskId, String advice) {
        LocalDateTime now = LocalDateTime.now();
        String sql = "UPDATE tasks SET advice = ?, advice_created_at = ?, updated_at = CURRENT_TIMESTAMP WHERE task_id = ?";
        int updated = jdbcTemplate.update(sql, advice, Timestamp.valueOf(now), taskId);
        return updated > 0;
    }

    // Update task advice with manager ID (since manager is already assigned, we only need to update advice)
    public boolean updateAdviceWithManager(int taskId, String advice, int managerId) {
        LocalDateTime now = LocalDateTime.now();
        String sql = "UPDATE tasks SET advice = ?, advice_created_at = ?, updated_at = CURRENT_TIMESTAMP WHERE task_id = ?";
        int updated = jdbcTemplate.update(sql, advice, Timestamp.valueOf(now), taskId);
        return updated > 0;
    }

    // Count tasks with advice
    public int countTasksWithAdvice(UUID tenantId) {
        String sql = "SELECT COUNT(*) FROM tasks WHERE tenant_id = ? AND advice IS NOT NULL AND advice != ''";
        return jdbcTemplate.queryForObject(sql, Integer.class, tenantId);
    }

    // Get tasks with advice by supervisor/manager ID (using assigned_to relationship)
    public List<Task> getTasksWithAdviceByManager(UUID tenantId, int managerId, int page, int size) {
        String sql = "SELECT * FROM tasks WHERE tenant_id = ? AND assigned_to_id = ? AND advice IS NOT NULL AND advice != '' ORDER BY advice_created_at DESC LIMIT ? OFFSET ?";
        int offset = page * size;
        return jdbcTemplate.query(sql, taskRowMapper, tenantId, managerId, size, offset);
    }

    // Count tasks with advice by supervisor/manager ID (using assigned_to relationship)
    public int countTasksWithAdviceByManager(UUID tenantId, int managerId) {
        String sql = "SELECT COUNT(*) FROM tasks WHERE tenant_id = ? AND assigned_to_id = ? AND advice IS NOT NULL AND advice != ''";
        return jdbcTemplate.queryForObject(sql, Integer.class, tenantId, managerId);
    }

    // Get recent advice tasks (ordered by advice_created_at)
    public List<Task> getRecentAdviceTasks(UUID tenantId, int page, int size) {
        String sql = "SELECT * FROM tasks WHERE tenant_id = ? AND advice IS NOT NULL AND advice != '' " +
                "ORDER BY advice_created_at DESC LIMIT ? OFFSET ?";
        int offset = page * size;
        return jdbcTemplate.query(sql, taskRowMapper, tenantId, size, offset);
    }

    // Get advice tasks by date range
    public List<Task> getAdviceTasksByDateRange(UUID tenantId, LocalDateTime startDate, LocalDateTime endDate, int page, int size) {
        String sql = "SELECT * FROM tasks WHERE tenant_id = ? AND advice IS NOT NULL AND advice != '' " +
                "AND advice_created_at BETWEEN ? AND ? ORDER BY advice_created_at DESC LIMIT ? OFFSET ?";
        int offset = page * size;
        return jdbcTemplate.query(sql, taskRowMapper, tenantId,
                Timestamp.valueOf(startDate), Timestamp.valueOf(endDate), size, offset);
    }

    // Count advice tasks by date range
    public int countAdviceTasksByDateRange(UUID tenantId, LocalDateTime startDate, LocalDateTime endDate) {
        String sql = "SELECT COUNT(*) FROM tasks WHERE tenant_id = ? AND advice IS NOT NULL AND advice != '' " +
                "AND advice_created_at BETWEEN ? AND ?";
        return jdbcTemplate.queryForObject(sql, Integer.class, tenantId,
                Timestamp.valueOf(startDate), Timestamp.valueOf(endDate));
    }

    // Get advice count per day for date range
    public List<Map<String, Object>> getAdviceCountPerDay(UUID tenantId, LocalDateTime startDate, LocalDateTime endDate) {
        String sql = "SELECT DATE(advice_created_at) as date, COUNT(*) as count FROM tasks " +
                "WHERE tenant_id = ? AND advice IS NOT NULL AND advice != '' AND advice_created_at BETWEEN ? AND ? " +
                "GROUP BY DATE(advice_created_at) ORDER BY date";
        return jdbcTemplate.queryForList(sql, tenantId, Timestamp.valueOf(startDate), Timestamp.valueOf(endDate));
    }

    // Get advice tasks created by specific user
    public List<Task> getAdviceTasksByCreator(UUID tenantId, int creatorId, int page, int size) {
        String sql = "SELECT * FROM tasks WHERE tenant_id = ? AND created_by_id = ? AND advice IS NOT NULL AND advice != '' " +
                "ORDER BY advice_created_at DESC LIMIT ? OFFSET ?";
        int offset = page * size;
        return jdbcTemplate.query(sql, taskRowMapper, tenantId, creatorId, size, offset);
    }

    // Clear advice from a task
    public boolean clearAdvice(int taskId) {
        String sql = "UPDATE tasks SET advice = NULL, advice_created_at = NULL, updated_at = CURRENT_TIMESTAMP WHERE task_id = ?";
        int updated = jdbcTemplate.update(sql, taskId);
        return updated > 0;
    }

    // Get average time between task creation and first advice
    public double getAvgTimeToAdvice(UUID tenantId) {
        String sql = "SELECT AVG(EXTRACT(EPOCH FROM (advice_created_at - created_at))/86400) as avg_days " +
                "FROM tasks WHERE tenant_id = ? AND advice IS NOT NULL AND advice != ''";
        return jdbcTemplate.queryForObject(sql, Double.class, tenantId);
    }
}