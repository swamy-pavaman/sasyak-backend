package com.kapilagro.sasyak.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public class TaskAdviceRepo {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public TaskAdviceRepo(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public int countByTenantId(UUID tenantId) {
        String sql = "SELECT COUNT(*) FROM task_advices WHERE tenant_id = ?";
        return jdbcTemplate.queryForObject(sql, Integer.class, tenantId);
    }
}

