package com.kapilagro.sasyak.services;

import com.kapilagro.sasyak.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
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
    public List<User> getAllEmployees(UUID tenantId) {
        return userService.getUsersByTenantAndRole(tenantId, "EMPLOYEE");
    }

    // Get all managers for a tenant
    public List<User> getAllManagers(UUID tenantId) {
        return userService.getUsersByTenantAndRole(tenantId, "MANAGER");
    }

    // Get all supervisors for a tenant
    public List<User> getAllSupervisors(UUID tenantId) {
        return userService.getUsersByTenantAndRole(tenantId, "SUPERVISOR");
    }

    // Get both managers and supervisors for a tenant
    public List<User> getManagersAndSupervisors(UUID tenantId) {
        return userService.getManagersAndSupervisors(tenantId);
    }

    // New method: Get paginated managers for a tenant
    public Page<User> getPagedManagers(UUID tenantId, Pageable pageable) {
        // Get total count of managers
        List<User> allManagers = userService.getUsersByTenantAndRole(tenantId, "manager");
        System.out.println(allManagers);
        int total = allManagers.size();

        // Get paginated managers using offset and limit
        List<User> pagedManagers = userService.getPagedUsersByTenantAndRole(
                tenantId,
                "MANAGER",
                pageable.getPageNumber(),
                pageable.getPageSize()
        );

        // Create page object
        return new PageImpl<>(pagedManagers, pageable, total);
    }

    // New method: Get paginated supervisors for a tenant
    public Page<User> getPagedSupervisors(UUID tenantId, Pageable pageable) {
        // Get total count of supervisors
        List<User> allSupervisors = userService.getUsersByTenantAndRole(tenantId, "SUPERVISOR");
        int total = allSupervisors.size();

        // Get paginated supervisors using offset and limit
        List<User> pagedSupervisors = userService.getPagedUsersByTenantAndRole(
                tenantId,
                "SUPERVISOR",
                pageable.getPageNumber(),
                pageable.getPageSize()
        );

        // Create page object
        return new PageImpl<>(pagedSupervisors, pageable, total);
    }

    // New method: Delete an employee
    @Transactional
    public boolean deleteEmployee(int employeeId, UUID tenantId) {
        // Verify the employee exists and belongs to the tenant
        Optional<User> userOpt = userService.getUserById(employeeId);

        if (userOpt.isPresent()) {
            User user = userOpt.get();

            // Verify the user belongs to the tenant and is an employee/manager/supervisor
            if (user.getTenantId() != null &&
                    user.getTenantId().equals(tenantId) &&
                    (user.getRole().equals("EMPLOYEE") ||
                            user.getRole().equals("MANAGER") ||
                            user.getRole().equals("SUPERVISOR"))) {

                // Delete the user
                return userService.deleteUser(employeeId);
            }
        }

        return false;
    }
}