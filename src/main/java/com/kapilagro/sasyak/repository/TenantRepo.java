package com.kapilagro.sasyak.repository;

import com.kapilagro.sasyak.model.Tenant;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Repository
public class TenantRepo {

    @Autowired
    JdbcTemplate template;

    // Row mapper for Tenant objects
    private final RowMapper<Tenant> tenantRowMapper = (rs, rowNum) -> {
        Tenant tenant = Tenant.builder()
                .tenantId(UUID.fromString(rs.getString("tenant_id")))
                .companyName(rs.getString("company_name"))
                .contactEmail(rs.getString("contact_email"))
                .createdAt(rs.getTimestamp("created_at").toLocalDateTime())
                .build();

        // Handle active status if it exists in the result set
        try {
            tenant.setActive(rs.getBoolean("active"));
        } catch (Exception e) {
            // If active column doesn't exist, default to true
            tenant.setActive(true);
        }

        return tenant;
    };

    /**
     * Save a new tenant
     */
    public UUID save(Tenant tenant) {
        String query = "INSERT INTO tenants (tenant_id, company_name, contact_email, created_at, active) VALUES (?, ?, ?, ?, ?)";

        UUID tenantId = UUID.randomUUID(); // manually generate UUID
        tenant.setTenantId(tenantId);     // set it in the object

        template.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(query);
            ps.setObject(1, tenantId); // tenant_id
            ps.setString(2, tenant.getCompanyName()); // name (not companyName!)
            ps.setString(3, tenant.getContactEmail());
            ps.setTimestamp(4, Timestamp.valueOf(tenant.getCreatedAt()));
            ps.setBoolean(5, tenant.isActive()); // NEW active column
            return ps;
        });

        return tenantId;
    }


    /**
     * Get all tenants
     */
    public List<Tenant> getAllTenants() {
        String query = "SELECT * FROM tenants ORDER BY created_at DESC";
        return template.query(query, tenantRowMapper);
    }

    /**
     * Get a tenant by ID
     */
    public Optional<Tenant> getTenantById(UUID id) {
        String query = "SELECT * FROM tenants WHERE tenant_id = ?";
        return template.query(query, tenantRowMapper, id)
                .stream()
                .findFirst();
    }

    /**
     * Update tenant status (active/inactive)
     */
    public boolean updateTenantStatus(int tenantId, boolean active) {
        String query = "UPDATE tenants SET active = ? WHERE tenant_id = ?";
        int rowsAffected = template.update(query, active, tenantId);
        return rowsAffected > 0;
    }

    /**
     * Get all active tenants
     */
    public List<Tenant> getAllActiveTenants() {
        String query = "SELECT * FROM tenants WHERE active = true ORDER BY created_at DESC";
        return template.query(query, tenantRowMapper);
    }

    /**
     * Get all inactive tenants
     */
    public List<Tenant> getAllInactiveTenants() {
        String query = "SELECT * FROM tenants WHERE active = false ORDER BY created_at DESC";
        return template.query(query, tenantRowMapper);
    }
}