package com.kapilagro.sasyak.services;

import com.kapilagro.sasyak.model.User;
import com.kapilagro.sasyak.repository.UserRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserService {

    @Autowired
    UserRepo userRepo;

    private final PasswordEncoder passwordEncoder;

    // Constructor injection instead of field injection
    public UserService(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    public User getUserByUserEmail(String email) {
        return userRepo.getUserByEmail(email);
    }

    public User getSuperAdminByEmail(String email) {
        return userRepo.getSuperAdminByEmail(email);
    }

    // This method registers a user and returns the user with ID
    public User registerUser(User user) {
        // Set default role if not provided
        if (user.getRole() == null || user.getRole().isEmpty()) {
            user.setRole("USER");
        }

        // Encode password
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        // Save to database
        int userId = userRepo.save(user);
        user.setUserId(userId);
        return user;
    }

    // This method is similar to registerUser but doesn't encode the password
    // It's used when the password is already encoded
    public int registerUserWithoutAuthentication(User user) {
        // Set default role if not provided
        if (user.getRole() == null || user.getRole().isEmpty()) {
            user.setRole("USER");
        }

        // Save to database
        int userId = userRepo.save(user);
        user.setUserId(userId);
        return userId;
    }

    // For development purposes, set the encoded password for the test user
    public void setEncodedPasswordForTestUser(String email) {
        User user = userRepo.getUserByEmail(email);
        if (user != null && (user.getPassword() == null || user.getPassword().startsWith("{noop}"))) {
            String rawPassword = user.getPassword() != null ?
                    user.getPassword().replace("{noop}", "") : "1010";
            user.setPassword(passwordEncoder.encode(rawPassword));
        }
    }

    public Optional<User> getUserById(int id) {
        return userRepo.getUserById(id);
    }

    // Get users by tenant ID
    public List<User> getUsersByTenant(UUID tenantId) {
        return userRepo.getUsersByTenant(tenantId);
    }

    // Get users by tenant ID and role (case-insensitive)
    public List<User> getUsersByTenantAndRole(UUID tenantId, String role) {
        return userRepo.getUsersByTenantAndRole(tenantId, role);
    }

    // Get paginated users by tenant ID and role (case-insensitive)
    public List<User> getPagedUsersByTenantAndRole(UUID tenantId, String role, int page, int size) {
        return userRepo.getPagedUsersByTenantAndRole(tenantId, role, page, size);
    }

    // Get managers and supervisors for a specific tenant (case-insensitive)
    public List<User> getManagersAndSupervisors(UUID tenantId) {
        return userRepo.getUsersByTenantAndRoles(tenantId, Arrays.asList("Manager", "Supervisor"));
    }

    // Check if a user belongs to a specific tenant
    public boolean isUserInTenant(int userId, UUID tenantId) {
        Optional<User> user = getUserById(userId);
        return user.isPresent() && user.get().getTenantId() != null && user.get().getTenantId().equals(tenantId);
    }

    // Create a new employee for a tenant
    public User createEmployee(User employee, UUID tenantId) {
        // Set tenant ID
        employee.setTenantId(tenantId);

        // Set role to EMPLOYEE if not specified
        if (employee.getRole() == null || employee.getRole().isEmpty()) {
            employee.setRole("EMPLOYEE");
        }

        // Register the user
        return registerUser(employee);
    }

    // Delete a user by ID
    @Transactional
    public boolean deleteUser(int userId) {
        return userRepo.deleteById(userId);
    }

    // Update an existing user
    @Transactional
    public User updateUser(User user) {
        // Check if there's a password to encode
        if (user.getPassword() != null && !user.getPassword().isEmpty()
                && !user.getPassword().startsWith("$2a$")) {
            // Only encode if it's not already encoded (doesn't start with bcrypt prefix)
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }

        // Update the user in the database
        userRepo.update(user);

        // Return the updated user
        return user;
    }

    // Debug method to get the count of managers for a tenant
    public int countManagersByTenant(UUID tenantId) {
        return userRepo.countManagersByTenant(tenantId);
    }

    public int countUsersByTenantAndRole(UUID tenantId, String role) {
        return userRepo.countUsersByTenantAndRole(tenantId, role);
    }

    public int countUsersByTenant(UUID tenantId) {
        return userRepo.countUsersByTenant(tenantId);
    }

    // Get users that report to a specific manager
    public List<User> getUsersByManagerId(int managerId) {
        return userRepo.getUsersByManagerId(managerId);
    }
}