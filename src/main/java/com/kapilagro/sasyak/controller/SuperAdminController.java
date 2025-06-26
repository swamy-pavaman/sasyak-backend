package com.kapilagro.sasyak.controller;

import com.kapilagro.sasyak.exceptions.TenantAlreadyExistsException;
import com.kapilagro.sasyak.model.*;
import com.kapilagro.sasyak.services.EmailService;
import com.kapilagro.sasyak.services.SuperAdminService;
import com.kapilagro.sasyak.services.TenantService;
import com.kapilagro.sasyak.services.UserService;
import com.kapilagro.sasyak.utils.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class SuperAdminController {

    @Autowired
    private SuperAdminService superAdminService;

    @Autowired
    private EmailService emailService;

    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final JwtUtil jwtUtil;
    private final UserService userService;


    public SuperAdminController(PasswordEncoder passwordEncoder, AuthenticationManager authenticationManager, UserDetailsService userDetailsService, JwtUtil jwtUtil, UserService userService) {
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
        this.jwtUtil = jwtUtil;
        this.userService = userService;
    }



    /**
     * Create a new tenant with admin user
     * Only accessible by super admin
     */


    @PostMapping("/tenants")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<CreateTenantResponse> createTenant(@RequestBody CreateTenantRequest request) throws TenantAlreadyExistsException {
        long start = System.currentTimeMillis();
        System.out.println("🚀 [createTenant] Started at " + start);

        if (superAdminService.existsByContactEmail(request.getContactEmail())) {
            System.out.println("❌ [createTenant] Tenant already exists: " + request.getContactEmail());
            throw new TenantAlreadyExistsException("A tenant with this contact email already exists.");
        }

        try {
            System.out.println("📦 [createTenant] Building Tenant & AdminUser models");

            Tenant tenant = Tenant.builder()
                    .companyName(request.getCompanyName())
                    .contactEmail(request.getContactEmail())
                    .build();

            User adminUser = User.builder()
                    .name(request.getAdminName())
                    .email(request.getAdminEmail())
                    .role(request.getRole() != null ? request.getRole() : "ADMIN")
                    .build();

            System.out.println("⚙️ [createTenant] Calling superAdminService.createTenant...");
            TenantService.TenantCreationResult result = superAdminService.createTenant(tenant, adminUser);
            System.out.println("✅ [createTenant] Tenant created successfully in " + (System.currentTimeMillis() - start) + "ms");

            CreateTenantResponse response = CreateTenantResponse.builder()
                    .message("Tenant created successfully.")
                    .tenant(CreateTenantResponse.TenantDTO.builder()
                            .id(String.valueOf(result.getTenant().getTenantId()))
                            .companyName(result.getTenant().getCompanyName())
                            .contactEmail(result.getTenant().getContactEmail())
                            .build())
                    .adminUser(CreateTenantResponse.AdminUserDTO.builder()
                            .id(result.getAdminUser().getUserId())
                            .name(result.getAdminUser().getName())
                            .email(result.getAdminUser().getEmail())
                            .build())
                    .build();

            System.out.println("📧 [createTenant] Sending email to " + request.getAdminEmail());
            try {
                emailService.sendMail(
                        request.getAdminEmail(),
                        request.getCompanyName(),
                        result.getGeneratedPassword(),
                        "Admin",
                        result.getAdminUser().getName()
                );

                System.out.println(result.getAdminUser().getName()+"  nameeeeeeeeeeeeeeeeeeeeeeeeeeeeeee");
                System.out.println("📨 [createTenant] Email sent in " + (System.currentTimeMillis() - start) + "ms");
            } catch (Exception emailEx) {
                System.err.println("❌ [createTenant] Failed to send email: " + emailEx.getMessage());
                emailEx.printStackTrace();
            }

            System.out.println("✅ [createTenant] Completed in " + (System.currentTimeMillis() - start) + "ms");
            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (Exception e) {
            System.err.println("❌ [createTenant] Exception occurred: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CreateTenantResponse.builder()
                            .message("Error creating tenant: " + e.getMessage())
                            .build());
        }
    }


//    @PostMapping("/tenants")
//    @PreAuthorize("hasRole('SUPER_ADMIN')")
//    public ResponseEntity<CreateTenantResponse> createTenant(@RequestBody CreateTenantRequest request) throws TenantAlreadyExistsException {
//        if (superAdminService.existsByContactEmail(request.getContactEmail())) {
//            throw new TenantAlreadyExistsException("A tenant with this contact email already exists.");
//        }
//
//        try {
//            // Create tenant model from request
//            Tenant tenant = Tenant.builder()
//                    .companyName(request.getCompanyName())
//                    .contactEmail(request.getContactEmail())
//                    .build();
//
//            // Create admin user model from request
//            User adminUser = User.builder()
//                    .name(request.getAdminName())
//                    .email(request.getAdminEmail())
//                    .role(request.getRole() != null ? request.getRole() : "ADMIN")
//                    .build();
//
//            // Create the tenant with admin
//            TenantService.TenantCreationResult result = superAdminService.createTenant(tenant, adminUser);
//
//
//
//            // Build response
//            CreateTenantResponse response = CreateTenantResponse.builder()
//                    .message("Tenant created successfully.")
//                    .tenant(CreateTenantResponse.TenantDTO.builder()
//                            .id(String.valueOf(result.getTenant().getTenantId()))
//                            .companyName(result.getTenant().getCompanyName())
//                            .contactEmail(result.getTenant().getContactEmail())
//                            .build())
//                    .adminUser(CreateTenantResponse.AdminUserDTO.builder()
//                            .id(result.getAdminUser().getUserId())
//                            .name(result.getAdminUser().getName())
//                            .email(result.getAdminUser().getEmail())
//                            .build())
//                    .build();
//            //TODO send mail here
//            emailService.sendMail(request.getAdminEmail(),request.getCompanyName(), result.getGeneratedPassword(),"Admin",result.getAdminUser().getName());
//
//            return ResponseEntity.status(HttpStatus.CREATED).body(response);
//        } catch (Exception e) {
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
//                    .body(CreateTenantResponse.builder()
//                            .message("Error creating tenant: " + e.getMessage())
//                            .build());
//        }
//    }
    /**
     * Get all tenants
     * Only accessible by super admin
     */
    @GetMapping("/tenants")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<GetAllTenantsResponse> getAllTenants() {
        try {
            List<Tenant> tenants = superAdminService.getAllTenants();
            System.out.println(tenants);
            // Map tenant entities to DTOs
            List<GetAllTenantsResponse.TenantDTO> tenantDTOs = tenants.stream()
                    .map(tenant -> GetAllTenantsResponse.TenantDTO.builder()
                            .id(tenant.getTenantId())
                            .companyName(tenant.getCompanyName())
                            .contactEmail(tenant.getContactEmail())
                            .createdAt(tenant.getCreatedAt())
                            .build())
                    .collect(Collectors.toList());

            // Build resp
            GetAllTenantsResponse response = GetAllTenantsResponse.builder()
                    .tenants(tenantDTOs)
                    .build();

            return ResponseEntity.ok(response);
        } catch (Exception e) {
           // e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new GetAllTenantsResponse());
        }
    }

    /**
     * Get tenant by ID
     * Only accessible by super admin
     */
    @GetMapping("/tenants/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<?> getTenantById(@PathVariable("id") UUID id) {
        try {
            return superAdminService.getTenantById(id)
                    .map(tenant -> {
                        GetAllTenantsResponse.TenantDTO tenantDTO = GetAllTenantsResponse.TenantDTO.builder()
                                .id(tenant.getTenantId())
                                .companyName(tenant.getCompanyName())
                                .contactEmail(tenant.getContactEmail())
                                .createdAt(tenant.getCreatedAt())
                                .build();
                        return ResponseEntity.ok().body(tenantDTO);
                    })
                    .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body((GetAllTenantsResponse.TenantDTO) Map.of("error", "Tenant not found with ID: " + id)));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error retrieving tenant: " + e.getMessage()));
        }
    }
    /**
     * Get tenant's users
     * Only accessible by super admin
     */
    @GetMapping("/tenants/{id}/users")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<?> getTenantUsers(@PathVariable("id") UUID id) {
        try {
            // Check if tenant exists
            if (!superAdminService.tenantExists(id)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("Tenant not found with ID: " + id);
            }
            // Get users for the tenant
            List<User> users = superAdminService.getUsersByTenant(id);

            // Map to DTO
            List<GetEmployeesResponse.EmployeeDTO> userDTOs = users.stream()
                    .map(user -> GetEmployeesResponse.EmployeeDTO.builder()
                            .id(user.getUserId())
                            .name(user.getName())
                            .email(user.getEmail())
                            .role(user.getRole())
                            .build())
                    .collect(Collectors.toList());

            return ResponseEntity.ok(GetEmployeesResponse.builder()
                    .employees(userDTOs)
                    .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error retrieving tenant users: " + e.getMessage());
        }
    }

    /**
     * Deactivate a tenant
     * Only accessible by super admin
     */
    @PutMapping("/tenants/{id}/deactivate")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<?> deactivateTenant(@PathVariable("id") int id) {
        try {
            boolean deactivated = superAdminService.deactivateTenant(id);

            if (deactivated) {
                return ResponseEntity.ok("Tenant deactivated successfully");
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("Tenant not found with ID: " + id);
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error deactivating tenant: " + e.getMessage());
        }
    }

    /**
     * Reactivate a tenant
     * Only accessible by super admin
     */
    @PutMapping("/tenants/{id}/activate")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<?> activateTenant(@PathVariable("id") int id) {
        try {
            boolean activated = superAdminService.activateTenant(id);

            if (activated) {
                return ResponseEntity.ok("Tenant activated successfully");
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("Tenant not found with ID: " + id);
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error activating tenant: " + e.getMessage());
        }
    }
}