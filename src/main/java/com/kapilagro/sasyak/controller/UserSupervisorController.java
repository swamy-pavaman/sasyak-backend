package com.kapilagro.sasyak.controller;

import com.kapilagro.sasyak.model.GetEmployeesResponse;
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

import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/supervisor")
@PreAuthorize("hasRole('SUPERVISOR')")
public class UserSupervisorController {

    @Autowired
    private UserService userService;

    // Helper method to get the current tenant ID from the authenticated user
    private UUID getCurrentUserTenantId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = (User) authentication.getPrincipal();
        if (currentUser.getTenantId() == null) {
            throw new IllegalStateException("Supervisor user is not associated with a tenant");
        }
        return currentUser.getTenantId();
    }

    // Helper method to get the current user's ID
    private int getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = (User) authentication.getPrincipal();
        return currentUser.getUserId();
    }

    // Get the supervisor's manager
    @GetMapping("/manager")
    public ResponseEntity<?> getMyManager() {
        try {
            int supervisorId = getCurrentUserId();
            Optional<User> supervisorOpt = userService.getUserById(supervisorId);

            if (supervisorOpt.isPresent()) {
                User supervisor = supervisorOpt.get();

                if (supervisor.getManagerId() != null) {
                    Optional<User> managerOpt = userService.getUserById(supervisor.getManagerId());

                    if (managerOpt.isPresent()) {
                        User manager = managerOpt.get();

                        UserDTO managerDTO = UserDTO.builder()
                                .id(manager.getUserId())
                                .name(manager.getName())
                                .email(manager.getEmail())
                                .role(manager.getRole())
                                .tenantId(manager.getTenantId())
                                .build();

                        return ResponseEntity.ok(managerDTO);
                    }
                }

                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("No manager assigned");
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Error retrieving supervisor information");
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error retrieving manager information: " + e.getMessage());
        }
    }

    // Update the supervisor's own profile
    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(@RequestBody User userDetails) {
        try {
            int supervisorId = getCurrentUserId();
            Optional<User> supervisorOpt = userService.getUserById(supervisorId);

            if (supervisorOpt.isPresent()) {
                User supervisor = supervisorOpt.get();

                // Update only allowed fields
                if (userDetails.getName() != null) {
                    supervisor.setName(userDetails.getName());
                }

                if (userDetails.getPhone_number() != null) {
                    supervisor.setPhone_number(userDetails.getPhone_number());
                }

                // Update password if provided
                if (userDetails.getPassword() != null && !userDetails.getPassword().isEmpty()) {
                    supervisor.setPassword(userDetails.getPassword());
                }

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
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Error retrieving supervisor information");
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error updating profile: " + e.getMessage());
        }
    }

    // Get supervisor's profile
    @GetMapping("/profile")
    public ResponseEntity<?> getProfile() {
        try {
            int supervisorId = getCurrentUserId();
            Optional<User> supervisorOpt = userService.getUserById(supervisorId);

            if (supervisorOpt.isPresent()) {
                User supervisor = supervisorOpt.get();

                UserDTO userDTO = UserDTO.builder()
                        .id(supervisor.getUserId())
                        .name(supervisor.getName())
                        .email(supervisor.getEmail())
                        .role(supervisor.getRole())
                        .tenantId(supervisor.getTenantId())
                        .build();

                return ResponseEntity.ok(userDTO);
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Error retrieving supervisor information");
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error retrieving profile: " + e.getMessage());
        }
    }
}