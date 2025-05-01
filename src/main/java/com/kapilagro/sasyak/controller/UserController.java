package com.kapilagro.sasyak.controller;


import com.kapilagro.sasyak.model.GetUserResponse;
import com.kapilagro.sasyak.model.User;
import com.kapilagro.sasyak.model.UserDTO;
import com.kapilagro.sasyak.services.MyUserPrincipal;
import com.kapilagro.sasyak.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("api/user")
public class UserController {

    @Autowired
    private UserService userService;

    private UUID getCurrentUserTenantId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = (User) authentication.getPrincipal();
        if (currentUser.getTenantId() == null) {
            throw new IllegalStateException("Supervisor user is not associated with a tenant");
        }
        return currentUser.getTenantId();
    }

    private int getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = (User) authentication.getPrincipal();
        return currentUser.getUserId();
    }

//    @GetMapping("/{id}")
//    public ResponseEntity<?> getUser(@PathVariable("id") int id){
//        Optional<User> user = userService.getUserById(id);
//        Optional<GetUserResponse> response = user.map(u -> GetUserResponse.builder()
//                .userId(u.getUserId())
//                .name(u.getName())
//                .email(u.getEmail())
//                .phoneNumber(u.getPhone_number())
//                .role(u.getRole())
//                .managerId(u.getManagerId())
//                .build());
//
//        return response.<ResponseEntity<?>>map(ResponseEntity::ok)
//                .orElseGet(()->ResponseEntity.status(HttpStatus.NOT_FOUND).body("User Not Found"));
//    }

    @GetMapping("/me")
    public ResponseEntity<?> getMe() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not authenticated");
            }

            // Check if the principal is a User object directly
            if (authentication.getPrincipal() instanceof User) {
                User user = (User) authentication.getPrincipal();

                UserDTO userDTO = UserDTO.builder()
                        .id(user.getUserId())
                        .name(user.getName())
                        .email(user.getEmail())
                        .role(user.getRole())
                        .tenantId(user.getTenantId())
                        .phoneNumber(user.getPhoneNumber())
                        .build();

                return ResponseEntity.ok(userDTO);
            } else {
                // Fallback to username lookup if principal isn't a User
                String username = authentication.getName();
                User user = userService.getUserByUserEmail(username);

                if (user != null) {
                    UserDTO userDTO = UserDTO.builder()
                            .id(user.getUserId())
                            .name(user.getName())
                            .email(user.getEmail())
                            .role(user.getRole())
                            .tenantId(user.getTenantId())
                            .build();

                    return ResponseEntity.ok(userDTO);
                }
            }

            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error retrieving user information");
        }
    }
    // New CRUD operations for users


    @GetMapping("/profile")
    public ResponseEntity<?> getProfile() {
        try {
            int userId = getCurrentUserId();
            Optional<User> userOpt = userService.getUserById(userId);

            if (userOpt.isPresent()) {
                User supervisor = userOpt.get();

                UserDTO userDTO = UserDTO.builder()
                        .id(supervisor.getUserId())
                        .name(supervisor.getName())
                        .email(supervisor.getEmail())
                        .phoneNumber(supervisor.getPhoneNumber())
                        .role(supervisor.getRole())
                        .tenantId(supervisor.getTenantId())
                        .profile(supervisor.getProfile())
                        .location(supervisor.getLocation())
                        .build();
                return ResponseEntity.ok(userDTO);
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Error retrieving user information");
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error retrieving profile: " + e.getMessage());
        }
    }


    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(@RequestBody User userDetails) {
        try {
            int userId = getCurrentUserId();
            Optional<User> userOpt = userService.getUserById(userId);

            if (userOpt.isPresent()) {
                User user = userOpt.get();

                // Update fields
                if (userDetails.getName() != null) {
                    user.setName(userDetails.getName());
                }

                if (userDetails.getPhoneNumber() != null) {
                    user.setPhoneNumber(userDetails.getPhoneNumber());
                }

                if (userDetails.getPassword() != null && !userDetails.getPassword().isEmpty()) {
                    user.setPassword(userDetails.getPassword());
                }

                if (userDetails.getLocation() != null) {
                    user.setLocation(userDetails.getLocation());
                }

                if (userDetails.getProfile() != null) {
                    user.setProfile(userDetails.getProfile());
                }

                User updatedUser = userService.updateUser(user);

                UserDTO userDTO = UserDTO.builder()
                        .id(updatedUser.getUserId())
                        .name(updatedUser.getName())
                        .email(updatedUser.getEmail())
                        .role(updatedUser.getRole())
                        .tenantId(updatedUser.getTenantId())
                        .phoneNumber(updatedUser.getPhoneNumber())
                        .location(updatedUser.getLocation())
                        .profile(updatedUser.getProfile())
                        .build();

                return ResponseEntity.ok(userDTO);
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Error retrieving user information");
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error updating profile: " + e.getMessage());
        }
    }

//    @PostMapping
//    public ResponseEntity<?> createUser(@RequestBody User user) {
//        try {
//            User createdUser = userService.registerUser(user);
//            return ResponseEntity.status(HttpStatus.CREATED).body(
//                    UserDTO.builder()
//                            .id(createdUser.getUserId())
//                            .name(createdUser.getName())
//                            .email(createdUser.getEmail())
//                            .role(createdUser.getRole())
//                            .tenantId(createdUser.getTenantId())
//                            .build()
//            );
//        } catch (Exception e) {
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
//                    .body("Error creating user: " + e.getMessage());
//        }
//    }
//
//    @PutMapping("/{id}")
//    public ResponseEntity<?> updateUser(@PathVariable("id") int id, @RequestBody User userDetails) {
//        try {
//            Optional<User> existingUser = userService.getUserById(id);
//
//            if (existingUser.isPresent()) {
//                User user = existingUser.get();
//
//                // Update user fields
//                if (userDetails.getName() != null) {
//                    user.setName(userDetails.getName());
//                }
//
//                if (userDetails.getEmail() != null) {
//                    user.setEmail(userDetails.getEmail());
//                }
//
//                if (userDetails.getPhone_number() != null) {
//                    user.setPhone_number(userDetails.getPhone_number());
//                }
//
//                if (userDetails.getRole() != null) {
//                    user.setRole(userDetails.getRole());
//                }
//
//                if (userDetails.getManagerId() != null) {
//                    user.setManagerId(userDetails.getManagerId());
//                }
//
//                // Update password only if provided
//                if (userDetails.getPassword() != null && !userDetails.getPassword().isEmpty()) {
//                    // Password will be encoded in the service
//                    user.setPassword(userDetails.getPassword());
//                }
//
//                // Save the updated user
//                User updatedUser = userService.updateUser(user);
//
//                return ResponseEntity.ok(
//                        UserDTO.builder()
//                                .id(updatedUser.getUserId())
//                                .name(updatedUser.getName())
//                                .email(updatedUser.getEmail())
//                                .role(updatedUser.getRole())
//                                .tenantId(updatedUser.getTenantId())
//                                .build()
//                );
//            } else {
//                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found with ID: " + id);
//            }
//        } catch (Exception e) {
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
//                    .body("Error updating user: " + e.getMessage());
//        }
//    }

//    @DeleteMapping("/{id}")
//    public ResponseEntity<?> deleteUser(@PathVariable("id") int id) {
//        try {
//            boolean deleted = userService.deleteUser(id);
//
//            if (deleted) {
//                return ResponseEntity.ok("User deleted successfully");
//            } else {
//                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found with ID: " + id);
//            }
//        } catch (Exception e) {
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
//                    .body("Error deleting user: " + e.getMessage());
//        }
//    }
}