package com.kapilagro.sasyak.controller;

import com.kapilagro.sasyak.model.*;
import com.kapilagro.sasyak.services.AdminService;
import com.kapilagro.sasyak.services.EmailService;
import com.kapilagro.sasyak.utils.GeneratePasswordUtility;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@CrossOrigin("*")
@RequestMapping("/api/admin")
public class AdminController {

    @Autowired
    private AdminService adminService;

    @Autowired
    private GeneratePasswordUtility generatePasswordUtility;

    @Autowired
    private EmailService emailService;

    // Helper method to get the current tenant ID from the authenticated user
    private UUID getCurrentUserTenantId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = (User) authentication.getPrincipal();
        if (currentUser.getTenantId() == null) {
            throw new IllegalStateException("Admin user is not associated with a tenant");
        }
        return currentUser.getTenantId();
    }

    @PostMapping("/employees")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createEmployee(@RequestBody CreateEmployeeRequest request) {
        try {
            UUID  tenantId = getCurrentUserTenantId();

            // Create employee model from request
            User employee = User.builder()
                    .name(request.getName())
                    .email(request.getEmail())
                    .password(generatePasswordUtility.generateRandomPassword())
                    .phone_number(request.getPhone_number())
                    .role(request.getRole() != null ? request.getRole() : "EMPLOYEE")
                    .build();

            // Create the employee
            System.out.println(employee.getPassword());
            User createdEmployee = adminService.createEmployee(employee, tenantId);

            emailService.sendMail(employee.getEmail(),request.getCompanyName(),employee.getPassword());

            return ResponseEntity.status(HttpStatus.CREATED).body(
                    GetEmployeesResponse.EmployeeDTO.builder()
                            .id(createdEmployee.getUserId())
                            .name(createdEmployee.getName())
                            .email(createdEmployee.getEmail())
                            .role(createdEmployee.getRole())
                            .build()
            );


        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error creating employee: " + e.getMessage());
        }
    }

    @GetMapping("/employees")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GetEmployeesResponse> getAllEmployees() {
        try {
            UUID  tenantId = getCurrentUserTenantId();

            List<User> employees = adminService.getAllEmployees(tenantId);

            // Map employee entities to DTOs
            List<GetEmployeesResponse.EmployeeDTO> employeeDTOs = employees.stream()
                    .map(employee -> GetEmployeesResponse.EmployeeDTO.builder()
                            .id(employee.getUserId())
                            .name(employee.getName())
                            .email(employee.getEmail())
                            .role(employee.getRole())
                            .build())
                    .collect(Collectors.toList());

            // Build response
            GetEmployeesResponse response = GetEmployeesResponse.builder()
                    .employees(employeeDTOs)
                    .build();

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new GetEmployeesResponse());
        }
    }

    @GetMapping("/managers-supervisors")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GetEmployeesResponse> getManagersAndSupervisors() {
        try {
            UUID tenantId = getCurrentUserTenantId();

            List<User> managersAndSupervisors = adminService.getManagersAndSupervisors(tenantId);

            // Map entities to DTOs
            List<GetEmployeesResponse.EmployeeDTO> dtos = managersAndSupervisors.stream()
                    .map(user -> GetEmployeesResponse.EmployeeDTO.builder()
                            .id(user.getUserId())
                            .name(user.getName())
                            .email(user.getEmail())
                            .role(user.getRole())
                            .build())
                    .collect(Collectors.toList());

            // Build response
            GetEmployeesResponse response = GetEmployeesResponse.builder()
                    .employees(dtos)
                    .build();

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new GetEmployeesResponse());
        }
    }

    // New endpoint for paginated managers
    @GetMapping("/managers")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PagedEmployeesResponse> getManagers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            UUID tenantId = getCurrentUserTenantId();

            // Get paged managers
            Page<User> managersPage = adminService.getPagedManagers(tenantId, PageRequest.of(page, size));

            System.out.println("tenantId   re  "+tenantId);

            // Map to DTOs
            List<GetEmployeesResponse.EmployeeDTO> managerDTOs = managersPage.getContent().stream()
                    .map(manager -> GetEmployeesResponse.EmployeeDTO.builder()
                            .id(manager.getUserId())
                            .name(manager.getName())
                            .email(manager.getEmail())
                            .role(manager.getRole())
                            .build())
                    .collect(Collectors.toList());

            // Build paged response
            PagedEmployeesResponse response = PagedEmployeesResponse.builder()
                    .employees(managerDTOs)
                    .totalItems(managersPage.getTotalElements())
                    .totalPages(managersPage.getTotalPages())
                    .currentPage(managersPage.getNumber())
                    .build();

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new PagedEmployeesResponse());
        }
    }

    // New endpoint for paginated supervisors
    @GetMapping("/supervisors")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PagedEmployeesResponse> getSupervisors(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            UUID tenantId = getCurrentUserTenantId();

            // Get paged supervisors
            Page<User> supervisorsPage = adminService.getPagedSupervisors(tenantId, PageRequest.of(page, size));

            // Map to DTOs
            List<GetEmployeesResponse.EmployeeDTO> supervisorDTOs = supervisorsPage.getContent().stream()
                    .map(supervisor -> GetEmployeesResponse.EmployeeDTO.builder()
                            .id(supervisor.getUserId())
                            .name(supervisor.getName())
                            .email(supervisor.getEmail())
                            .role(supervisor.getRole())
                            .build())
                    .collect(Collectors.toList());

            // Build paged response
            PagedEmployeesResponse response = PagedEmployeesResponse.builder()
                    .employees(supervisorDTOs)
                    .totalItems(supervisorsPage.getTotalElements())
                    .totalPages(supervisorsPage.getTotalPages())
                    .currentPage(supervisorsPage.getNumber())
                    .build();

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new PagedEmployeesResponse());
        }
    }

    // New endpoint to delete an employee
    @DeleteMapping("/employees/{employeeId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteEmployee(@PathVariable int employeeId) {
        try {
            UUID tenantId = getCurrentUserTenantId();

            // Check if the employee exists and belongs to the tenant
            boolean deleted = adminService.deleteEmployee(employeeId, tenantId);

            if (deleted) {
                return ResponseEntity.ok("Employee deleted successfully");
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("Employee not found or does not belong to your tenant");
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error deleting employee: " + e.getMessage());
        }
    }
}