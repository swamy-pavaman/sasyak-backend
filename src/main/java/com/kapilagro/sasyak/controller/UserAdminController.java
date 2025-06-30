package com.kapilagro.sasyak.controller;

import com.kapilagro.sasyak.model.*;
import com.kapilagro.sasyak.services.AdminService;
import com.kapilagro.sasyak.services.CatalogService;
import com.kapilagro.sasyak.services.EmailService;
import com.kapilagro.sasyak.services.UserService;
import com.kapilagro.sasyak.utils.GeneratePasswordUtility;
import com.kapilagro.sasyak.utils.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
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


    // get  current user id
    private int getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = (User) authentication.getPrincipal();
        return currentUser.getUserId();
    }

    @GetMapping("/manager-list")
    public ResponseEntity<List<GetManagerListResponse>> getManagerList() {
        try {
            UUID tenantId = getCurrentUserTenantId();
            List<GetManagerListResponse> managers = userService.getManagersList(tenantId);
            return ResponseEntity.ok(managers);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @GetMapping("/supervisor-list")
    public ResponseEntity<List<GetSupervisorsListResponse>> getSupervisorList() {
        try {
            UUID tenantId = getCurrentUserTenantId();
            List<GetSupervisorsListResponse> supervisors = userService.getSupervisorsList(tenantId);
            return ResponseEntity.ok(supervisors);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
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
                            .phoneNumber(user.getPhoneNumber())
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

// already there assign to endpoint is working in same way
//    @PostMapping("/reassign-supervisor/{supervisorId}/to-manager/{managerId}")
//    public ResponseEntity<?> reAssignManager(
//            @PathVariable("supervisorId") int supervisorId,
//            @PathVariable("managerId") int managerId) {
//        Logger log = LoggerFactory.getLogger(getClass());
//        log.debug("Entering reAssignManager with supervisorId: {}, managerId: {}", supervisorId, managerId);
//
//        try {
//            UUID tenantId = getCurrentUserTenantId();
//            log.debug("Retrieved tenantId: {}", tenantId);
//
//            // Check if the supervisor exists and belongs to the admin's tenant
//            Optional<User> supervisorOpt = userService.getUserById(supervisorId);
//            if (!supervisorOpt.isPresent() || !supervisorOpt.get().getTenantId().equals(tenantId)) {
//                log.error("Supervisor not found or does not belong to tenant: supervisorId={}, tenantId={}", supervisorId, tenantId);
//                return ResponseEntity.status(HttpStatus.NOT_FOUND)
//                        .body("Supervisor not found or does not belong to your tenant");
//            }
//
//            User supervisor = supervisorOpt.get();
//            // Verify the user is a supervisor
//            if (!"SUPERVISOR".equalsIgnoreCase(supervisor.getRole())) {
//                log.error("User is not a supervisor: userId={}, role={}", supervisorId, supervisor.getRole());
//                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
//                        .body("The specified user is not a supervisor");
//            }
//
//            // Check if the manager exists and belongs to the admin's tenant
//            Optional<User> managerOpt = userService.getUserById(managerId);
//            if (!managerOpt.isPresent() || !managerOpt.get().getTenantId().equals(tenantId)) {
//                log.error("Manager not found or does not belong to tenant: managerId={}, tenantId={}", managerId, tenantId);
//                return ResponseEntity.status(HttpStatus.NOT_FOUND)
//                        .body("Manager not found or does not belong to your tenant");
//            }
//
//            // Verify the user is a manager
//            if (!"MANAGER".equalsIgnoreCase(managerOpt.get().getRole())) {
//                log.error("User is not a manager: userId={}, role={}", managerId, managerOpt.get().getRole());
//                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
//                        .body("The specified user is not a manager");
//            }
//
//            // Update the supervisor's manager ID
//            supervisor.setManagerId(managerId);
//            User updatedSupervisor = userService.updateUser(supervisor);
//            log.debug("Supervisor reassigned successfully: supervisorId={}, newManagerId={}", supervisorId, managerId);
//
//            // Prepare response
//            UserDTO userDTO = UserDTO.builder()
//                    .id(updatedSupervisor.getUserId())
//                    .name(updatedSupervisor.getName())
//                    .email(updatedSupervisor.getEmail())
//                    .role(updatedSupervisor.getRole())
//                    .phoneNumber(updatedSupervisor.getPhoneNumber())
//                    .tenantId(updatedSupervisor.getTenantId())
//                    .build();
//
//            log.debug("Returning response: {}", userDTO);
//            return ResponseEntity.ok(userDTO);
//
//        } catch (Exception e) {
//            log.error("Error reassigning manager: supervisorId={}, managerId={}, message={}",
//                    supervisorId, managerId, e.getMessage(), e);
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
//                    .body("Error reassigning manager: " + e.getMessage());
//        } finally {
//            log.debug("Exiting reAssignManager");
//        }
//    }



    // Create a new user (admin can only create users in their tenant)


    @GetMapping("/supervisor/{supervisor_id}/assigned-manager")
    public ResponseEntity<SupervisorsManagerResponse> getSupervisorsManager(@PathVariable("supervisor_id") int supervisorId) {
        try {
            UUID tenantId = getCurrentUserTenantId();
            Optional<User> supervisorOpt = userService.getUserById(supervisorId);

            if (supervisorOpt.isPresent()) {
                User supervisor = supervisorOpt.get();

                // Verify supervisor belongs to admin's tenant
                if (!supervisor.getTenantId().equals(tenantId)) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body(new SupervisorsManagerResponse(null, null));
                }

                // Check if supervisor has a manager assigned
                if (supervisor.getManagerId() != null) {
                    Optional<User> managerOpt = userService.getUserById(supervisor.getManagerId());

                    if (managerOpt.isPresent()) {
                        User manager = managerOpt.get();
                        SupervisorsManagerResponse response = new SupervisorsManagerResponse(
                                manager.getName(),
                                manager.getEmail()
                        );
                        return ResponseEntity.ok(response);
                    }
                }

                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new SupervisorsManagerResponse(null, null));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new SupervisorsManagerResponse(null, null));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new SupervisorsManagerResponse(null, null));
        }
    }

    @PostMapping
    public ResponseEntity<?> createUser(@RequestBody CreateEmployeeRequest request) {
        Logger log = LoggerFactory.getLogger(getClass());
        log.debug("Entering createUser with request: {}", request);
        try {
            UUID tenantId = getCurrentUserTenantId();
            log.debug("Retrieved tenantId: {}", tenantId);

            // Validate managerId based on role
            Integer managerId = request.getManagerId();
            String role = request.getRole() != null ? request.getRole().toUpperCase() : "EMPLOYEE";

            // Only SUPERVISOR can have a managerId
            if (!"SUPERVISOR".equals(role)) {
                managerId = null; // Force managerId to null for non-SUPERVISOR roles
            } else if (managerId != null && managerId != 0) {
                // Validate that managerId exists in the users table
                boolean managerExists = adminService.userExitsById(managerId);
                if (!managerExists) {
                    log.error("Invalid managerId: {} for email: {}", managerId, request.getEmail());
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body("Invalid managerId: Manager does not exist.");
                }
            } else if (managerId == 0) {
                // Explicitly handle managerId = 0
                log.error("Invalid managerId: 0 provided for email: {}", request.getEmail());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Invalid managerId: 0 is not a valid manager ID.");
            }

            User employee = User.builder()
                    .name(request.getName())
                    .email(request.getEmail().toLowerCase())
                    .password(generatePasswordUtility.generateRandomPassword())
                    .phoneNumber(request.getPhone_number())
                    .role(role)
                    .managerId(managerId)
                    .build();
            log.debug("Created User object: name={}, email={}, role={}, managerId={}",
                    employee.getName(), employee.getEmail(), employee.getRole(), employee.getManagerId());
            log.debug("Generated password: {}", employee.getPassword());

            String password = employee.getPassword();
            log.debug("Calling adminService.createEmployee with email: {}, tenantId: {}", employee.getEmail(), tenantId);
            User createdEmployee = adminService.createEmployee(employee, tenantId);
            log.debug("Employee created successfully: userId={}, email={}", createdEmployee.getUserId(), createdEmployee.getEmail());

            String company = userService.getCompanyName(tenantId);
            log.debug("Sending email to: {}, company: {}", employee.getEmail(), company);
            emailService.sendMail(employee.getEmail(), company, password,createdEmployee.getRole(),createdEmployee.getName());
            log.debug("Email sent successfully to: {}", employee.getEmail());
            GetEmployeesResponse.EmployeeDTO response = GetEmployeesResponse.EmployeeDTO.builder()
                    .id(createdEmployee.getUserId())
                    .name(createdEmployee.getName())
                    .email(createdEmployee.getEmail())
                    .role(createdEmployee.getRole())
                    .build();
            log.debug("Returning response: {}", response);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException ex) {
            log.error("Conflict in createUser: {}", ex.getMessage(), ex);
            return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage());
        } catch (DataIntegrityViolationException ex) {
            log.error("Data integrity violation in createUser: email={}, message={}", request.getEmail(), ex.getMessage(), ex);
            return ResponseEntity.status(HttpStatus.CONFLICT).body("User already exists with this email.");
        } catch (Exception e) {
            log.error("Unexpected error in createUser: email={}, message={}", request.getEmail(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error creating employee: " + e.getMessage());
        } finally {
            log.debug("Exiting createUser");
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

                    if (userDetails.getPhoneNumber() != null) {
                        existingUser.setPhoneNumber(userDetails.getPhoneNumber());
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

                    if(userDetails.getPhoneNumber()!=null){
                        existingUser.setPhoneNumber(userDetails.getPhoneNumber());
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
                            .phoneNumber(updatedUser.getPhoneNumber())
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
//    @GetMapping("/by-role/{role}")
//    public ResponseEntity<GetEmployeesResponse> getUsersByRole(@PathVariable("role") String role) {
//        try {
//            UUID tenantId = getCurrentUserTenantId();
//            List<User> users = userService.getUsersByTenantAndRole(tenantId, role);
//
//            // Map user entities to DTOs
//            List<GetEmployeesResponse.EmployeeDTO> userDTOs = users.stream()
//                    .map(user -> GetEmployeesResponse.EmployeeDTO.builder()
//                            .id(user.getUserId())
//                            .name(user.getName())
//                            .email(user.getEmail())
//                            .role(user.getRole())
//                            .build())
//                    .collect(Collectors.toList());
//
//            // Build response
//            GetEmployeesResponse response = GetEmployeesResponse.builder()
//                    .employees(userDTOs)
//                    .build();
//
//            return ResponseEntity.ok(response);
//        } catch (Exception e) {
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
//                    .body(new GetEmployeesResponse());
//        }
//    }

    @GetMapping("/by-role/{role}")
    public ResponseEntity<?> getUsersByRole(@PathVariable("role") String role) {
        try {
            UUID tenantId = getCurrentUserTenantId();

            if (role == null || role.trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Role parameter must not be empty."));
            }

            List<User> users = userService.getUsersByTenantAndRole(tenantId, role.toUpperCase());

            if (users.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "No users found with role: " + role));
            }

            List<GetEmployeesResponse.EmployeeDTO> userDTOs = users.stream()
                    .map(user -> GetEmployeesResponse.EmployeeDTO.builder()
                            .id(user.getUserId())
                            .name(user.getName())
                            .email(user.getEmail())
                            .role(user.getRole())
                            .build())
                    .collect(Collectors.toList());

            GetEmployeesResponse response = GetEmployeesResponse.builder()
                    .employees(userDTOs)
                    .build();

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Invalid role provided: " + ex.getMessage()));
        } catch (Exception e) {
            //e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch users by role: " + e.getMessage()));
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


    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getDashboard() {
        UUID  tenantId = getCurrentUserTenantId();
        try {
            DashBoardResponse dashboardStats = adminService.getDashboardStats(tenantId);
            return ResponseEntity.ok(dashboardStats);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(DashBoardResponse.builder()
                            .errorMessage("Failed to fetch dashboard stats: " + e.getMessage())
                            .build());
        }
    }


    @GetMapping("/managers")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PagedEmployeesResponse> getManagers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            UUID tenantId = getCurrentUserTenantId();

            // Get paged managers
            Page<User> managersPage = adminService.getPagedManagers(tenantId, PageRequest.of(page, size));

            System.out.println("in admin controller tenantId    "+tenantId);

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


}