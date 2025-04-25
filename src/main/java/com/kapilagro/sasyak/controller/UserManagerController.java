package com.kapilagro.sasyak.controller;

import com.kapilagro.sasyak.model.GetEmployeesResponse;
import com.kapilagro.sasyak.model.PagedEmployeesResponse;
import com.kapilagro.sasyak.model.User;
import com.kapilagro.sasyak.model.UserDTO;
import com.kapilagro.sasyak.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/manager/users")
@PreAuthorize("hasRole('MANAGER') or hasRole('ADMIN')")
public class UserManagerController {

    @Autowired
    private UserService userService;

    // Helper method to get the current tenant ID from the authenticated user
    private UUID getCurrentUserTenantId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = (User) authentication.getPrincipal();
        if (currentUser.getTenantId() == null) {
            throw new IllegalStateException("Manager user is not associated with a tenant");
        }
        return currentUser.getTenantId();
    }

    // Helper method to get the current user's ID
    private int getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = (User) authentication.getPrincipal();
        return currentUser.getUserId();
    }

    // Get all users that report to the current manager
    @GetMapping("/team")
    public ResponseEntity<GetEmployeesResponse> getTeamMembers() {
        try {
            UUID tenantId = getCurrentUserTenantId();
            int managerId = getCurrentUserId();

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
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new GetEmployeesResponse());
        }
    }


    @GetMapping("/supervisors")
    public ResponseEntity<PagedEmployeesResponse> getAllSupervisors(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            UUID tenantId = getCurrentUserTenantId();

            // Get all supervisors first to count total
            List<User> allSupervisors = userService.getUsersByTenantAndRole(tenantId, "SUPERVISOR");
            int total = allSupervisors.size();

            // Get paginated supervisors
            List<User> pagedSupervisors = userService.getPagedUsersByTenantAndRole(
                    tenantId,
                    "SUPERVISOR",
                    page,
                    size
            );

            // Map to DTOs
            List<GetEmployeesResponse.EmployeeDTO> supervisorDTOs = pagedSupervisors.stream()
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
    // Update a team member (manager can only update users in their team)
    @PutMapping("/team/{id}")
    public ResponseEntity<?> updateTeamMember(@PathVariable("id") int id, @RequestBody User userDetails) {
        try {
            UUID tenantId = getCurrentUserTenantId();
            int managerId = getCurrentUserId();

            Optional<User> existingUserOpt = userService.getUserById(id);

            if (existingUserOpt.isPresent()) {
                User existingUser = existingUserOpt.get();

                // Verify user belongs to manager's tenant and team
                if (existingUser.getTenantId() != null &&
                        existingUser.getTenantId().equals(tenantId) &&
                        existingUser.getManagerId() != null &&
                        existingUser.getManagerId() == managerId) {

                    // Update user fields (restricted set of fields for managers)
                    if (userDetails.getName() != null) {
                        existingUser.setName(userDetails.getName());
                    }

                    if (userDetails.getPhone_number() != null) {
                        existingUser.setPhone_number(userDetails.getPhone_number());
                    }

                    // Managers cannot change email, role, or tenant

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

    // Assign a supervisor to the manager's team
    @PutMapping("/assign/{supervisorId}")
    public ResponseEntity<?> assignSupervisorToTeam(@PathVariable("supervisorId") int supervisorId) {
        try {
            UUID tenantId = getCurrentUserTenantId();
            int managerId = getCurrentUserId();

            Optional<User> supervisorOpt = userService.getUserById(supervisorId);

            if (supervisorOpt.isPresent()) {
                User supervisor = supervisorOpt.get();

                // Verify supervisor belongs to manager's tenant and is a supervisor
                if (supervisor.getTenantId() != null &&
                        supervisor.getTenantId().equals(tenantId) &&
                        "SUPERVISOR".equalsIgnoreCase(supervisor.getRole())) {

                    // Assign manager ID to supervisor
                    supervisor.setManagerId(managerId);

                    // Save the updated supervisor
                    User updatedSupervisor = userService.updateUser(supervisor);

                    UserDTO userDTO = UserDTO.builder()
                            .id(updatedSupervisor.getUserId())
                            .name(updatedSupervisor.getName())
                            .email(updatedSupervisor.getEmail())
                            .role(updatedSupervisor.getRole())
                            .tenantId(updatedSupervisor.getTenantId())
                            .build();

                    return ResponseEntity.ok(userDTO);
                } else {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body("Invalid supervisor ID or user is not a supervisor in your tenant");
                }
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("Supervisor not found with ID: " + supervisorId);
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error assigning supervisor to team: " + e.getMessage());
        }
    }

    // Remove a supervisor from the manager's team
    @PutMapping("/unassign/{supervisorId}")
    public ResponseEntity<?> unassignSupervisorFromTeam(@PathVariable("supervisorId") int supervisorId) {
        try {
            UUID tenantId = getCurrentUserTenantId();
            int managerId = getCurrentUserId();

            Optional<User> supervisorOpt = userService.getUserById(supervisorId);

            if (supervisorOpt.isPresent()) {
                User supervisor = supervisorOpt.get();

                // Verify supervisor belongs to manager's tenant and is part of manager's team
                if (supervisor.getTenantId() != null &&
                        supervisor.getTenantId().equals(tenantId) &&
                        supervisor.getManagerId() != null &&
                        supervisor.getManagerId() == managerId) {

                    // Remove manager ID from supervisor
                    supervisor.setManagerId(null);

                    // Save the updated supervisor
                    User updatedSupervisor = userService.updateUser(supervisor);

                    UserDTO userDTO = UserDTO.builder()
                            .id(updatedSupervisor.getUserId())
                            .name(updatedSupervisor.getName())
                            .email(updatedSupervisor.getEmail())
                            .role(updatedSupervisor.getRole())
                            .tenantId(updatedSupervisor.getTenantId())
                            .build();

                    return ResponseEntity.ok(userDTO);
                } else {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body("Supervisor is not part of your team");
                }
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("Supervisor not found with ID: " + supervisorId);
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error removing supervisor from team: " + e.getMessage());
        }
    }
}