package com.kapilagro.sasyak.repository;

import com.kapilagro.sasyak.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Repository
public class UserRepo {

    @Autowired
    JdbcTemplate template;

    private final RowMapper<User> userRowMapper = (rs, rowNum) -> {
        User user = new User();
        user.setUserId((int) rs.getLong("user_id"));
        user.setName(rs.getString("name"));
        user.setEmail(rs.getString("email"));
        user.setPassword(rs.getString("password"));
        user.setRole(rs.getString("role"));

        // Check if tenant_id column is present in the result set
        try {
            UUID tenantId = UUID.fromString(rs.getString("tenant_id"));
            if (!rs.wasNull()) {
                user.setTenantId(tenantId);
            }
        } catch (Exception e) {
            // No tenant_id column or it's null
            user.setTenantId(null);
        }

        try {
            user.setPhone_number(rs.getString("phone_number"));
        } catch (Exception e) {
            // Phone number might be null
        }

        try {
            int managerId = rs.getInt("manager_id");
            if (!rs.wasNull()) {
                user.setManagerId(managerId);
            }
        } catch (Exception e) {
            // Manager ID might be null
        }

        return user;
    };

    public User getUserByEmail(String email) {
        String query = "SELECT * FROM users WHERE email = ?";
        try {
            return template.queryForObject(query, new Object[]{email}, userRowMapper);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    public User getSuperAdminByEmail(String email) {
        String query = "SELECT * FROM users WHERE email = ? AND role = 'SUPER_ADMIN'";
        try {
            return template.queryForObject(query, new Object[]{email}, userRowMapper);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    public int save(User user) {
        String query = "INSERT INTO users (name, email, password, role, tenant_id, phone_number, manager_id) VALUES (?, ?, ?, ?, ?, ?, ?)";

        KeyHolder keyHolder = new GeneratedKeyHolder();

        template.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, user.getName());
            ps.setString(2, user.getEmail());
            ps.setString(3, user.getPassword());
            ps.setString(4, user.getRole());

            if (user.getTenantId() != null) {
                ps.setObject(5, user.getTenantId(), java.sql.Types.OTHER); // UUID
            } else {
                ps.setNull(5, java.sql.Types.OTHER);
            }
            ps.setString(6, user.getPhone_number());

            if (user.getManagerId() != null) {
                ps.setInt(7, user.getManagerId());
            } else {
                ps.setNull(7, java.sql.Types.INTEGER);
            }

            return ps;
        }, keyHolder);

        Map<String, Object> keys = keyHolder.getKeys();
        if (keys != null) {
            return (int) keys.get("user_id");
        } else {
            throw new IllegalStateException("Failed to retrieve user_id after insertion.");
        }
    }

    @Transactional
    public boolean update(User user) {
        String query = "UPDATE users SET name = ?, email = ?, role = ?, phone_number = ?, manager_id = ? WHERE user_id = ?";

        int updated = template.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(query);
            ps.setString(1, user.getName());
            ps.setString(2, user.getEmail());
            ps.setString(3, user.getRole());
            ps.setString(4, user.getPhone_number());

            if (user.getManagerId() != null) {
                ps.setInt(5, user.getManagerId());
            } else {
                ps.setNull(5, java.sql.Types.INTEGER);
            }

            ps.setInt(6, user.getUserId());

            return ps;
        });

        // If password is provided, update it separately
        if (user.getPassword() != null && !user.getPassword().isEmpty()) {
            String passwordQuery = "UPDATE users SET password = ? WHERE user_id = ?";
            template.update(passwordQuery, user.getPassword(), user.getUserId());
        }

        return updated > 0;
    }

    public Optional<User> getUserById(int id) {
        String query = "SELECT * FROM users WHERE user_id = ?";
        return template.query(query, userRowMapper, id)
                .stream()
                .findFirst();
    }

    public List<User> getUsersByTenantAndRole(UUID tenantId, String role) {
        // Case-insensitive role search
        String query = "SELECT * FROM users WHERE tenant_id = ? AND UPPER(role) = UPPER(?)";
        return template.query(query, userRowMapper, tenantId, role);
    }

    // Modified: Case-insensitive role search with pagination
    public List<User> getPagedUsersByTenantAndRole(UUID tenantId, String role, int page, int size) {
        String query = "SELECT * FROM users WHERE tenant_id = ? AND UPPER(role) = UPPER(?) ORDER BY name LIMIT ? OFFSET ?";
        int offset = page * size;
        System.out.println("Executing query for tenant_id=" + tenantId + ", role=" + role + ", limit=" + size + ", offset=" + offset);
        return template.query(query, userRowMapper, tenantId, role, size, offset);
    }

    public List<User> getUsersByTenant(UUID tenantId) {
        String query = "SELECT * FROM users WHERE tenant_id = ?";
        return template.query(query, userRowMapper, tenantId);
    }

    // Method to get users by a specific tenant with roles in the provided list
    public List<User> getUsersByTenantAndRoles(UUID tenantId, List<String> roles) {
        if (roles == null || roles.isEmpty()) {
            return List.of();
        }

        // Convert the roles list to ensure case insensitivity
        StringBuilder placeholders = new StringBuilder();
        for (int i = 0; i < roles.size(); i++) {
            if (i > 0) {
                placeholders.append(",");
            }
            placeholders.append("UPPER(?)");
        }

        String query = String.format("SELECT * FROM users WHERE tenant_id = ? AND UPPER(role) IN (%s)", placeholders);

        Object[] params = new Object[roles.size() + 1];
        params[0] = tenantId;
        for (int i = 0; i < roles.size(); i++) {
            params[i + 1] = roles.get(i).toUpperCase();
        }

        return template.query(query, userRowMapper, params);
    }

    // Delete a user by ID
    @Transactional
    public boolean deleteById(int userId) {
        String query = "DELETE FROM users WHERE user_id = ?";
        int rowsAffected = template.update(query, userId);
        return rowsAffected > 0;
    }

    // Added: Debug method to get the count of managers for a tenant
    public int countManagersByTenant(UUID tenantId) {
        String query = "SELECT COUNT(*) FROM users WHERE tenant_id = ? AND UPPER(role) = 'MANAGER'";
        return template.queryForObject(query, Integer.class, tenantId);
    }

    public int countUsersByTenant(UUID tenantId) {
        String query = "SELECT COUNT(*) FROM users WHERE tenant_id = ?";
        return template.queryForObject(query, Integer.class, tenantId);
    }

    public int countUsersByTenantAndRole(UUID tenantId, String role) {
        String query = "SELECT COUNT(*) FROM users WHERE tenant_id = ? AND UPPER(role) = UPPER(?)";
        return template.queryForObject(query, Integer.class, tenantId, role);
    }

    // Get users that report to a specific manager
    public List<User> getUsersByManagerId(int managerId) {
        String query = "SELECT * FROM users WHERE manager_id = ?";
        return template.query(query, userRowMapper, managerId);
    }
}