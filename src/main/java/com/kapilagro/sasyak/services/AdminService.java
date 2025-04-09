package com.kapilagro.sasyak.services;

import com.kapilagro.sasyak.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class AdminService {

    @Autowired
    private UserService userService;

    // Create a new employee
    public User createEmployee(User employee, UUID tenantId) {
        return userService.createEmployee(employee, tenantId);
    }

    // Get all employees for a tenant
    public List<User> getAllEmployees(UUID  tenantId) {
        return userService.getUsersByTenantAndRole(tenantId, "EMPLOYEE");
    }

    // Get all managers for a tenant
    public List<User> getAllManagers(UUID  tenantId) {
        return userService.getUsersByTenantAndRole(tenantId, "MANAGER");
    }

    // Get all supervisors for a tenant
    public List<User> getAllSupervisors(UUID  tenantId) {
        return userService.getUsersByTenantAndRole(tenantId, "SUPERVISOR");
    }

    // Get both managers and supervisors for a tenant
    public List<User> getManagersAndSupervisors(UUID tenantId) {
        return userService.getManagersAndSupervisors(tenantId);
    }
}