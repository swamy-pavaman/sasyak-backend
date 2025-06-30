package com.kapilagro.sasyak.repository;

import com.kapilagro.sasyak.model.TaskAdvice;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Repository
public class TaskAdviceRepo {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public TaskAdviceRepo(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // Row mapper for TaskAdvice
    private final RowMapper<TaskAdvice> adviceRowMapper = (rs, rowNum) -> {
        return TaskAdvice.builder()
                .adviceId(rs.getInt("advice_id"))
                .tenantId(UUID.fromString(rs.getString("tenant_id")))
                .taskId(rs.getInt("task_id"))
                .managerId(rs.getInt("manager_id"))
                .adviceText(rs.getString("advice_text"))
                .createdAt(rs.getObject("created_at", OffsetDateTime.class))
                .updatedAt(rs.getObject("updated_at", OffsetDateTime.class))
                .build();
    };

    public int countByTenantId(UUID tenantId) {
        String sql = "SELECT COUNT(*) FROM task_advices WHERE tenant_id = ?";
        return jdbcTemplate.queryForObject(sql, Integer.class, tenantId);
    }

    // Save new task advice
    public int save(TaskAdvice advice) {
        String sql = "INSERT INTO task_advices (tenant_id, task_id, manager_id, advice_text) VALUES (?, ?, ?, ?)";

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setObject(1, advice.getTenantId());
            ps.setInt(2, advice.getTaskId());
            ps.setInt(3, advice.getManagerId());
            ps.setString(4, advice.getAdviceText());

            return ps;
        }, keyHolder);

        Map<String, Object> keys = keyHolder.getKeys();
        if (keys != null) {
            return (int) keys.get("advice_id");
        } else {
            throw new IllegalStateException("Failed to retrieve advice_id after insertion.");
        }
    }

    // Get advice by task ID
    public List<TaskAdvice> getByTaskId(int taskId) {
        String sql = "SELECT * FROM task_advices WHERE task_id = ? ORDER BY created_at DESC";
        return jdbcTemplate.query(sql, adviceRowMapper, taskId);
    }

    // Get advice provided by a manager
    public List<TaskAdvice> getByManagerId(UUID tenantId, int managerId) {
        String sql = "SELECT * FROM task_advices WHERE tenant_id = ? AND manager_id = ? ORDER BY created_at DESC";
        return jdbcTemplate.query(sql, adviceRowMapper, tenantId, managerId);
    }

    // Count advice provided by a manager
    public int countByManagerId(UUID tenantId, int managerId) {
        String sql = "SELECT COUNT(*) FROM task_advices WHERE tenant_id = ? AND manager_id = ?";
        return jdbcTemplate.queryForObject(sql, Integer.class, tenantId, managerId);
    }
}