package com.kapilagro.sasyak.services;

import com.kapilagro.sasyak.model.DashBoardResponse;
import com.kapilagro.sasyak.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class AdminService {

    @Autowired
    private UserService userService;

    @Autowired
    private TaskService taskService;

    @Autowired
    AdviceService adviceService;

    // Create a new employee
    @Transactional
    public User createEmployee(User employee, UUID tenantId) {
        Logger log = LoggerFactory.getLogger(getClass());
        log.debug("Entering createEmployee with email: {}, tenantId: {}", employee.getEmail(), tenantId);
        try {
            log.debug("Calling userService.createEmployee with email: {}, tenantId: {}", employee.getEmail(), tenantId);
            User createdUser = userService.createEmployee(employee, tenantId);
            log.debug("Employee created successfully: userId={}, email={}", createdUser.getUserId(), createdUser.getEmail());
            return createdUser;
        } catch (Exception e) {
            log.error("Error in createEmployee: email={}, tenantId={}, message={}", employee.getEmail(), tenantId, e.getMessage(), e);
            throw e;
        } finally {
            log.debug("Exiting createEmployee");
        }
    }

    // Get all employees for a tenant
    public List<User> getAllEmployees(UUID tenantId) {
        //TODO chnage this into get users by tanent id only
        //return userService.getUsersByTenant(tenantId);
        return userService.getUsersByTenant(tenantId);
    }


    // Get dashboard stats for a tenant
    public DashBoardResponse getDashboardStats(UUID tenantId) {
        // Get counts for different user roles
        int employeeCount = userService.countUsersByTenant(tenantId);
        int managerCount = userService.countUsersByTenantAndRole(tenantId, "Manager");
        int supervisorCount = userService.countUsersByTenantAndRole(tenantId, "Supervisor");

        // Get task counts (you'll need to create a TaskService if you don't have one)

        int totalTasks = taskService.countTasksByTenant(tenantId);

        // Get task status breakdown
        Map<String, Integer> taskStatusBreakdown = taskService.getTaskStatusBreakdown(tenantId);

        // Get recent tasks (e.g., from last 7 days)
        int recentTasks = taskService.countRecentTasksByTenant(tenantId, 7);

        // Get advice count
        int adviceCount = adviceService.countAdviceByTenant(tenantId);

        // Build and return the response
        return DashBoardResponse.builder()
                .totalEmployees(employeeCount)
                .totalManagers(managerCount)
                .totalSupervisors(supervisorCount)
                .totalTasks(totalTasks)
                .taskStatusBreakdown(taskStatusBreakdown)
                .recentTasks(recentTasks)
                .adviceCount(adviceCount)
                .build();
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

    // Get paginated managers for a tenant, with case-insensitive role handling
    public Page<User> getPagedManagers(UUID tenantId, Pageable pageable) {
        // Debug: Print total count and tenant ID
        System.out.println("Tenant ID: " + tenantId);

        // Get total count of managers
        List<User> allManagers = userService.getUsersByTenantAndRole(tenantId, "Manager");
        int total = allManagers.size();
        System.out.println("Total managers found: " + total);

        // Get paginated managers using offset and limit
        List<User> pagedManagers = userService.getPagedUsersByTenantAndRole(
                tenantId,
                "Manager", // Case matches what's in the database
                pageable.getPageNumber(),
                pageable.getPageSize()
        );

        System.out.println("Paged managers found: " + pagedManagers.size());

        // Create page object
        return new PageImpl<>(pagedManagers, pageable, total);
    }

    // Get paginated supervisors for a tenant
    public Page<User> getPagedSupervisors(UUID tenantId, Pageable pageable) {
        // Get total count of supervisors
        List<User> allSupervisors = userService.getUsersByTenantAndRole(tenantId, "Supervisor");
        int total = allSupervisors.size();

        // Get paginated supervisors using offset and limit
        List<User> pagedSupervisors = userService.getPagedUsersByTenantAndRole(
                tenantId,
                "Supervisor",
                pageable.getPageNumber(),
                pageable.getPageSize()
        );

        // Create page object
        return new PageImpl<>(pagedSupervisors, pageable, total);
    }

    // Delete an employee
    @Transactional
    public boolean deleteEmployee(int employeeId, UUID tenantId) {
        // Verify the employee exists and belongs to the tenant
        Optional<User> userOpt = userService.getUserById(employeeId);

        if (userOpt.isPresent()) {
            User user = userOpt.get();

            // Verify the user belongs to the tenant and is an employee/manager/supervisor
            // Using case-insensitive comparison
            if (user.getTenantId() != null &&
                    user.getTenantId().equals(tenantId) &&
                    (user.getRole().equalsIgnoreCase("EMPLOYEE") ||
                            user.getRole().equalsIgnoreCase("MANAGER") ||
                            user.getRole().equalsIgnoreCase("SUPERVISOR"))) {

                // Delete the user
                return userService.deleteUser(employeeId);
            }
        }

        return false;
    }

    // Debug method to get the count of managers
    public int getManagerCount(UUID tenantId) {
        return userService.countManagersByTenant(tenantId);
    }
}