package com.kapilagro.sasyak.repository;

import com.kapilagro.sasyak.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.web.bind.annotation.RequestBody;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.Map;
import java.util.Optional;


@Repository
public class UserRepo {

    @Autowired
    JdbcTemplate template;

    public User getUserByEmail(String email) {
        String query = "SELECT * FROM users WHERE email = ?";
        try {
            return template.queryForObject(query, new Object[]{email}, (rs, rowNum) -> {
                User user = new User();
                user.setUserId((int) rs.getLong("user_id"));
                user.setName(rs.getString("name"));
                user.setEmail(rs.getString("email"));
                user.setPassword(rs.getString("password"));
                user.setOauthProvider(rs.getString("oauth_provider"));
                user.setOAuthProviderId(rs.getString("oauth_provider_id"));
                user.setRole(rs.getString("role"));
                return user;
            });
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

//    public void save(User user) {
//        String query = "INSERT INTO users (name, email, password, oauth_provider, oauth_provider_id, role) VALUES (?, ?, ?, ?, ?, ?)";
//        template.update(query,
//                user.getName(),
//                user.getEmail(),
//                user.getPassword(),
//                user.getOauthProvider(),
//                user.getOAuthProviderId(),
//                user.getRole()
//        );
//    }

    public int save(User user) {
        String query = "INSERT INTO users (name, email, password, oauth_provider, oauth_provider_id, role) VALUES (?, ?, ?, ?, ?, ?)";

        KeyHolder keyHolder = new GeneratedKeyHolder();

        template.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, user.getName());
            ps.setString(2, user.getEmail());
            ps.setString(3, user.getPassword());
            ps.setString(4, user.getOauthProvider());
            ps.setString(5, user.getOAuthProviderId());
            ps.setString(6, user.getRole());
            return ps;
        }, keyHolder);

        // Correct way: Extracting user_id from getKeys()
        Map<String, Object> keys = keyHolder.getKeys();
        if (keys != null) {
            return (int) keys.get("user_id");  // Extract user_id safely
        } else {
            throw new IllegalStateException("Failed to retrieve user_id after insertion.");
        }
    }

    public Optional<User> getUserById(int id) {
        String query = "SELECT * FROM users WHERE user_id = ?";
        RowMapper<User> rowMapper = (rs, rowNum) -> {
            User user=User.builder()
                    .userId(rs.getInt("user_id"))
                    .name(rs.getString("name"))
                    .email(rs.getString("email"))
                    .role(rs.getString("role"))
                    .build();
            return user;
        };

        return template.query(query, rowMapper, id)
                .stream()
                .findFirst(); // Return Optional<User>
    }
}
