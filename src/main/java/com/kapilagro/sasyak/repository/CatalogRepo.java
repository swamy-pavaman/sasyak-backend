package com.kapilagro.sasyak.repository;

import com.kapilagro.sasyak.model.Catalog;
import com.kapilagro.sasyak.model.CatalogItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class CatalogRepo {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // RowMapper for Catalog entity
    private final RowMapper<Catalog> catalogRowMapper = new RowMapper<Catalog>() {
        @Override
        public Catalog mapRow(ResultSet rs, int rowNum) throws SQLException {
            return Catalog.builder()
                    .id(rs.getInt("id"))
                    .category(rs.getString("category"))
                    .value(rs.getString("value"))
                    .details(rs.getString("details"))
                    .tenantId((UUID) rs.getObject("tenant_id"))
                    .createdBy(rs.getInt("created_by"))
                    .createdAt(rs.getObject("created_at", OffsetDateTime.class))
                    .updatedAt(rs.getObject("updated_at", OffsetDateTime.class))  // ✅ Updated here
                    .build();
        }
    };

    // RowMapper for CatalogItem (simplified version)
    private final RowMapper<CatalogItem> catalogItemRowMapper = new RowMapper<CatalogItem>() {
        @Override
        public CatalogItem mapRow(ResultSet rs, int rowNum) throws SQLException {
            return CatalogItem.builder()
                    .id(rs.getInt("id"))
                    .value(rs.getString("value"))
                    .details(rs.getString("details"))
                    .category(rs.getString("category"))
                    .build();
        }
    };

    /**
     * Create a new catalog item
     */
    public int save(Catalog catalog) {
        String sql = "INSERT INTO catalog (category, value, details, tenant_id, created_by, created_at) " +
                "VALUES (?, ?, ?, ?, ?, ?)";

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, new String[]{"id"});  // Specify only 'id' column
            ps.setString(1, catalog.getCategory());
            ps.setString(2, catalog.getValue());
            ps.setString(3, catalog.getDetails());
            ps.setObject(4, catalog.getTenantId());
            ps.setInt(5, catalog.getCreatedBy());
            ps.setObject(6, LocalDateTime.now());
            return ps;
        }, keyHolder);

        return keyHolder.getKey().intValue();  // Now this will work since only 'id' is returned
    }

    /**
     * Get all catalog items for a tenant
     */
    public List<Catalog> findByTenantId(UUID tenantId) {
        String sql = "SELECT * FROM catalog WHERE tenant_id = ? ORDER BY category, value";
        return jdbcTemplate.query(sql, catalogRowMapper, tenantId);  // Fixed: Pass UUID directly
    }

    /**
     * Get catalog items by category for a tenant
     */
    public List<CatalogItem> findByTenantIdAndCategory(UUID tenantId, String category) {
        String sql = "SELECT id, category, value, details FROM catalog " +
                "WHERE tenant_id = ? AND LOWER(category) = LOWER(?) ORDER BY value";
        return jdbcTemplate.query(sql, catalogItemRowMapper, tenantId, category);  // Fixed: Pass UUID directly
    }

    /**
     * Find catalog item by ID and tenant
     */
    public Optional<Catalog> findByIdAndTenantId(int id, UUID tenantId) {
        String sql = "SELECT * FROM catalog WHERE id = ? AND tenant_id = ?";
        try {
            Catalog catalog = jdbcTemplate.queryForObject(sql, catalogRowMapper, id, tenantId);  // Fixed: Pass UUID directly
            return Optional.of(catalog);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    /**
     * Delete catalog item by ID and tenant
     */
    public boolean deleteByIdAndTenantId(int id, UUID tenantId) {
        String sql = "DELETE FROM catalog WHERE id = ? AND tenant_id = ?";
        int rowsAffected = jdbcTemplate.update(sql, id, tenantId);  // Fixed: Pass UUID directly
        return rowsAffected > 0;
    }

    /**
     * Update catalog item
     */
    public boolean update(Catalog catalog) {
        String sql = "UPDATE catalog SET category = ?, value = ?, details = ?, updated_at = ? " +
                "WHERE id = ? AND tenant_id = ?";
        int rowsAffected = jdbcTemplate.update(sql,
                catalog.getCategory(),
                catalog.getValue(),
                catalog.getDetails(),
                LocalDateTime.now(),
                catalog.getId(),
                catalog.getTenantId());  // Fixed: Pass UUID directly
        return rowsAffected > 0;
    }

    /**
     * Check if catalog item exists by ID and tenant
     */
    public boolean existsByIdAndTenantId(int id, UUID tenantId) {
        String sql = "SELECT COUNT(*) FROM catalog WHERE id = ? AND tenant_id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, id, tenantId);  // Fixed: Pass UUID directly
        return count != null && count > 0;
    }

    /**
     * Get distinct categories for a tenant
     */
    public List<String> findDistinctCategoriesByTenantId(UUID tenantId) {
        String sql = "SELECT DISTINCT category FROM catalog WHERE tenant_id = ? ORDER BY category";
        return jdbcTemplate.queryForList(sql, String.class, tenantId);  // Fixed: Pass UUID directly
    }

    /**
     * Count catalog items by tenant
     */
    public int countByTenantId(UUID tenantId) {
        String sql = "SELECT COUNT(*) FROM catalog WHERE tenant_id = ?";
        return jdbcTemplate.queryForObject(sql, Integer.class, tenantId);  // Fixed: Pass UUID directly
    }

    /**
     * Count catalog items by tenant and category
     */
    public int countByTenantIdAndCategory(UUID tenantId, String category) {
        String sql = "SELECT COUNT(*) FROM catalog WHERE tenant_id = ? AND LOWER(category) = LOWER(?)";
        return jdbcTemplate.queryForObject(sql, Integer.class, tenantId, category);  // Fixed: Pass UUID directly
    }
}