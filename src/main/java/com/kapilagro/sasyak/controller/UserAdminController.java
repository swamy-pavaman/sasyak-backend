package com.kapilagro.sasyak.controller;

import com.kapilagro.sasyak.model.*;
import com.kapilagro.sasyak.services.AdminService;
import com.kapilagro.sasyak.services.EmailService;
import com.kapilagro.sasyak.services.UserService;
import com.kapilagro.sasyak.utils.GeneratePasswordUtility;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@CrossOrigin("*")
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class UserAdminController {

    @Autowired
    private AdminService adminService;

    @Autowired
    private UserService userService;

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

    // Get all users for the current tenant
    @GetMapping
    public ResponseEntity<GetEmployeesResponse> getAllUsers() {
        try {
            UUID tenantId = getCurrentUserTenantId();
            List<User> users = userService.getUsersByTenant(tenantId);

            // Map user entities to DTOs
            List<GetEmployeesResponse.EmployeeDTO> userDTOs = users.stream()
                    .map(user -> GetEmployeesResponse.EmployeeDTO.builder()
                            .id(user.getUserId())
                            .name(user.getName())
                            .email(user.getEmail())
                            .role(user.getRole())
                            .build())
                    .collect(Collectors.toList());

            // Build response
            GetEmployeesResponse response = GetEmployeesResponse.builder()
                    .employees(userDTOs)
                    .build();

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new GetEmployeesResponse());
        }
    }

    // Get user by ID (admin can only access users in their tenant)
    @GetMapping("/{id}")
    public ResponseEntity<?> getUserById(@PathVariable("id") int id) {
        try {
            UUID tenantId = getCurrentUserTenantId();
            Optional<User> userOpt = userService.getUserById(id);

            if (userOpt.isPresent()) {
                User user = userOpt.get();

                // Verify user belongs to admin's tenant
                if (user.getTenantId() != null && user.getTenantId().equals(tenantId)) {
                    UserDTO userDTO = UserDTO.builder()
                            .id(user.getUserId())
                            .name(user.getName())
                            .email(user.getEmail())
                            .role(user.getRole())
                            .tenantId(user.getTenantId())
                            .build();

                    return ResponseEntity.ok(userDTO);
                } else {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body("You don't have permission to access this user");
                }
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("User not found with ID: " + id);
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error retrieving user: " + e.getMessage());
        }
    }

    // Create a new user (admin can only create users in their tenant)

    @PostMapping
    public ResponseEntity<?> createUser(@RequestBody CreateEmployeeRequest request) {
        try {
            UUID tenantId = getCurrentUserTenantId();

            User employee = User.builder()
                    .name(request.getName())
                    .email(request.getEmail())
                    .password(generatePasswordUtility.generateRandomPassword())
                    .phone_number(request.getPhone_number())
                    .role(request.getRole() != null ? request.getRole() : "EMPLOYEE")
                    .build();

            System.out.println(employee.getPassword());

            User createdEmployee = adminService.createEmployee(employee, tenantId);

            emailService.sendMail(employee.getEmail(), request.getCompanyName(), employee.getPassword());
            System.out.println(employee.getPassword());
            return ResponseEntity.status(HttpStatus.CREATED).body(
                    GetEmployeesResponse.EmployeeDTO.builder()
                            .id(createdEmployee.getUserId())
                            .name(createdEmployee.getName())
                            .email(createdEmployee.getEmail())
                            .role(createdEmployee.getRole())
                            .build()
            );

        } catch (DataIntegrityViolationException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("User already exists with this email.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error creating employee: " + e.getMessage());
        }
    }



    // Update an existing user (admin can only update users in their tenant)
    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(@PathVariable("id") int id, @RequestBody User userDetails) {
        try {
            UUID tenantId = getCurrentUserTenantId();
            Optional<User> existingUserOpt = userService.getUserById(id);

            if (existingUserOpt.isPresent()) {
                User existingUser = existingUserOpt.get();

                // Verify user belongs to admin's tenant
                if (existingUser.getTenantId() != null && existingUser.getTenantId().equals(tenantId)) {
                    // Update user fields
                    if (userDetails.getName() != null) {
                        existingUser.setName(userDetails.getName());
                    }

                    if (userDetails.getEmail() != null) {
                        existingUser.setEmail(userDetails.getEmail());
                    }

                    if (userDetails.getPhone_number() != null) {
                        existingUser.setPhone_number(userDetails.getPhone_number());
                    }

                    if (userDetails.getRole() != null) {
                        existingUser.setRole(userDetails.getRole());
                    }

                    if (userDetails.getManagerId() != null) {
                        existingUser.setManagerId(userDetails.getManagerId());
                    }

                    // Update password only if provided
                    if (userDetails.getPassword() != null && !userDetails.getPassword().isEmpty()) {
                        existingUser.setPassword(userDetails.getPassword());
                    }

                    // Keep the tenant ID the same
                    existingUser.setTenantId(tenantId);

                    // Save the updated user
                    User updatedUser = userService.updateUser(existingUser);

                    UserDTO userDTO = UserDTO.builder()
                            .id(updatedUser.getUserId())
                            .name(updatedUser.getName())
                            .email(updatedUser.getEmail())
                            .role(updatedUser.getRole())
                            .tenantId(updatedUser.getTenantId())
                            .build();

                    return ResponseEntity.ok(userDTO);
                } else {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body("You don't have permission to modify this user");
                }
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("User not found with ID: " + id);
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error updating user: " + e.getMessage());
        }
    }

    // Delete a user (admin can only delete users in their tenant)
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable("id") int id) {
        try {
            UUID tenantId = getCurrentUserTenantId();
            Optional<User> userOpt = userService.getUserById(id);

            if (userOpt.isPresent()) {
                User user = userOpt.get();

                // Verify user belongs to admin's tenant
                if (user.getTenantId() != null && user.getTenantId().equals(tenantId)) {
                    boolean deleted = userService.deleteUser(id);

                    if (deleted) {
                        return ResponseEntity.ok("User deleted successfully");
                    } else {
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body("Failed to delete user");
                    }
                } else {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body("You don't have permission to delete this user");
                }
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("User not found with ID: " + id);
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error deleting user: " + e.getMessage());
        }
    }

    // Get users by role (admin can only get users in their tenant)
    @GetMapping("/by-role/{role}")
    public ResponseEntity<GetEmployeesResponse> getUsersByRole(@PathVariable("role") String role) {
        try {
            UUID tenantId = getCurrentUserTenantId();
            List<User> users = userService.getUsersByTenantAndRole(tenantId, role);

            // Map user entities to DTOs
            List<GetEmployeesResponse.EmployeeDTO> userDTOs = users.stream()
                    .map(user -> GetEmployeesResponse.EmployeeDTO.builder()
                            .id(user.getUserId())
                            .name(user.getName())
                            .email(user.getEmail())
                            .role(user.getRole())
                            .build())
                    .collect(Collectors.toList());

            // Build response
            GetEmployeesResponse response = GetEmployeesResponse.builder()
                    .employees(userDTOs)
                    .build();

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new GetEmployeesResponse());
        }
    }

    // Get paginated users by role (admin can only get users in their tenant)
    @GetMapping("/by-role/{role}/paged")
    public ResponseEntity<PagedEmployeesResponse> getPagedUsersByRole(
            @PathVariable("role") String role,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            UUID tenantId = getCurrentUserTenantId();
            List<User> allUsersWithRole = userService.getUsersByTenantAndRole(tenantId, role);
            int total = allUsersWithRole.size();

            List<User> pagedUsers = userService.getPagedUsersByTenantAndRole(tenantId, role, page, size);

            // Map user entities to DTOs
            List<GetEmployeesResponse.EmployeeDTO> userDTOs = pagedUsers.stream()
                    .map(user -> GetEmployeesResponse.EmployeeDTO.builder()
                            .id(user.getUserId())
                            .name(user.getName())
                            .email(user.getEmail())
                            .role(user.getRole())
                            .build())
                    .collect(Collectors.toList());

            // Build paged response
            PagedEmployeesResponse response = PagedEmployeesResponse.builder()
                    .employees(userDTOs)
                    .totalItems(total)
                    .totalPages((int) Math.ceil((double) total / size))
                    .currentPage(page)
                    .build();

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new PagedEmployeesResponse());
        }
    }

    // Assign manager to a user
    @PutMapping("/{userId}/assign-manager/{managerId}")
    public ResponseEntity<?> assignManagerToUser(
            @PathVariable("userId") int userId,
            @PathVariable("managerId") int managerId) {
        try {
            UUID tenantId = getCurrentUserTenantId();

            // Check if the user exists and belongs to the admin's tenant
            Optional<User> userOpt = userService.getUserById(userId);
            if (!userOpt.isPresent() || !userOpt.get().getTenantId().equals(tenantId)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("User not found or does not belong to your tenant");
            }

            // Check if the manager exists and belongs to the admin's tenant
            Optional<User> managerOpt = userService.getUserById(managerId);
            if (!managerOpt.isPresent() || !managerOpt.get().getTenantId().equals(tenantId)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("Manager not found or does not belong to your tenant");
            }

            // Check if the manager has the correct role
            if (!"MANAGER".equalsIgnoreCase(managerOpt.get().getRole())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("The specified user is not a manager");
            }

            // Update the user's manager ID
            User user = userOpt.get();
            user.setManagerId(managerId);

            // Save the updated user
            User updatedUser = userService.updateUser(user);

            UserDTO userDTO = UserDTO.builder()
                    .id(updatedUser.getUserId())
                    .name(updatedUser.getName())
                    .email(updatedUser.getEmail())
                    .role(updatedUser.getRole())
                    .tenantId(updatedUser.getTenantId())
                    .build();

            return ResponseEntity.ok(userDTO);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error assigning manager: " + e.getMessage());
        }
    }

    // Remove manager from a user
    @PutMapping("/{userId}/remove-manager")
    public ResponseEntity<?> removeManagerFromUser(@PathVariable("userId") int userId) {
        try {
            UUID tenantId = getCurrentUserTenantId();

            // Check if the user exists and belongs to the admin's tenant
            Optional<User> userOpt = userService.getUserById(userId);
            if (!userOpt.isPresent() || !userOpt.get().getTenantId().equals(tenantId)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("User not found or does not belong to your tenant");
            }

            // Update the user's manager ID to null
            User user = userOpt.get();
            user.setManagerId(null);

            // Save the updated user
            User updatedUser = userService.updateUser(user);

            UserDTO userDTO = UserDTO.builder()
                    .id(updatedUser.getUserId())
                    .name(updatedUser.getName())
                    .email(updatedUser.getEmail())
                    .role(updatedUser.getRole())
                    .tenantId(updatedUser.getTenantId())
                    .build();

            return ResponseEntity.ok(userDTO);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error removing manager: " + e.getMessage());
        }
    }

    // Get users assigned to a specific manager
    @GetMapping("/manager/{managerId}")
    public ResponseEntity<?> getUsersByManager(@PathVariable("managerId") int managerId) {
        try {
            UUID tenantId = getCurrentUserTenantId();

            // Check if the manager exists and belongs to the admin's tenant
            Optional<User> managerOpt = userService.getUserById(managerId);
            if (!managerOpt.isPresent() || !managerOpt.get().getTenantId().equals(tenantId)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("Manager not found or does not belong to your tenant");
            }

            // Check if the manager has the correct role
            if (!"MANAGER".equalsIgnoreCase(managerOpt.get().getRole())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("The specified user is not a manager");
            }

            // Get users assigned to this manager
            List<User> teamMembers = userService.getUsersByManagerId(managerId);

            // Map user entities to DTOs
            List<GetEmployeesResponse.EmployeeDTO> userDTOs = teamMembers.stream()
                    .map(user -> GetEmployeesResponse.EmployeeDTO.builder()
                            .id(user.getUserId())
                            .name(user.getName())
                            .email(user.getEmail())
                            .role(user.getRole())
                            .build())
                    .collect(Collectors.toList());

            // Build response
            GetEmployeesResponse response = GetEmployeesResponse.builder()
                    .employees(userDTOs)
                    .build();

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error retrieving team members: " + e.getMessage());
        }
    }
}