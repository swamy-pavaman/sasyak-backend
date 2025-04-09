package com.kapilagro.sasyak.repository;

import com.kapilagro.sasyak.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

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
        // TODO add manager id also here
        user.setRole(rs.getString("role"));

        // Check if tenant_id column is present in the result set
        try {
            UUID  tenantId = UUID.fromString(rs.getString("tenant_id"));
            if (!rs.wasNull()) {
                user.setTenantId(tenantId);
            }
        } catch (Exception e) {
            // No tenant_id column or it's null
            user.setTenantId(null);
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
        String query = "INSERT INTO users (name, email, password, role, tenant_id) VALUES (?, ?, ?, ?, ?)";

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


            return ps;
        }, keyHolder);

        Map<String, Object> keys = keyHolder.getKeys();
        if (keys != null) {
            return (int) keys.get("user_id");
        } else {
            throw new IllegalStateException("Failed to retrieve user_id after insertion.");
        }
    }

    public Optional<User> getUserById(int id) {
        String query = "SELECT * FROM users WHERE user_id = ?";
        return template.query(query, userRowMapper, id)
                .stream()
                .findFirst();
    }

    public List<User> getUsersByTenantAndRole(UUID tenantId, String role) {
        String query = "SELECT * FROM users WHERE tenant_id = ? AND role = ?";
        return template.query(query, userRowMapper, tenantId, role);
    }

    public List<User> getUsersByTenant(int tenantId) {
        String query = "SELECT * FROM users WHERE tenant_id = ?";
        return template.query(query, userRowMapper, tenantId);
    }

    // Method to get users by a specific tenant with roles in the provided list
    public List<User> getUsersByTenantAndRoles(UUID tenantId, List<String> roles) {
        String inSql = String.join(",", java.util.Collections.nCopies(roles.size(), "?"));
        String query = String.format("SELECT * FROM users WHERE tenant_id = ? AND role IN (%s)", inSql);

        Object[] params = new Object[roles.size() + 1];
        params[0] = tenantId;
        for (int i = 0; i < roles.size(); i++) {
            params[i + 1] = roles.get(i);
        }

        return template.query(query, userRowMapper, params);
    }
}