package com.kapilagro.sasyak.repository;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Repository
public class TaskRepo {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public TaskRepo(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public int countByTenantId(UUID tenantId) {
        String sql = "SELECT COUNT(*) FROM tasks WHERE tenant_id = ?";
        return jdbcTemplate.queryForObject(sql, Integer.class, tenantId);
    }

    public Map<String, Integer> getTaskStatusBreakdown(UUID tenantId) {
        String sql = "SELECT status, COUNT(*) as count FROM tasks WHERE tenant_id = ? GROUP BY status";
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, tenantId);

        Map<String, Integer> statusBreakdown = new HashMap<>();
        for (Map<String, Object> row : rows) {
            String status = (String) row.get("status");
            Integer count = ((Number) row.get("count")).intValue();
            statusBreakdown.put(status, count);
        }

        return statusBreakdown;
    }

    public int countRecentByTenantId(UUID tenantId, int days) {
        String sql = "SELECT COUNT(*) FROM tasks WHERE tenant_id = ? AND created_at >= CURRENT_TIMESTAMP - INTERVAL '" + days + " days'";
        return jdbcTemplate.queryForObject(sql, Integer.class, tenantId);
    }
}