package com.kapilagro.sasyak.services;

import com.kapilagro.sasyak.model.Tenant;
import com.kapilagro.sasyak.model.User;
import com.kapilagro.sasyak.repository.TenantRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class SuperAdminService {

    @Autowired
    private TenantService tenantService;

    @Autowired
    private UserService userService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private TenantRepo tenantRepo;

    /**
     * Create a new tenant along with its admin
     */
    public TenantService.TenantCreationResult createTenant(Tenant tenant, User adminUser) {
        // Validate that this operation is only performed by a super admin
        // This validation should occur at the controller level with @PreAuthorize

        // Create the tenant and admin
        return tenantService.createTenantWithAdmin(tenant, adminUser);
    }

    /**
     * Get all tenants
     */
    public List<Tenant> getAllTenants() {
        return tenantService.getAllTenants();
    }

    /**
     * Get tenant by ID
     */
    public Optional<Tenant> getTenantById(UUID id) {
        return tenantService.getTenantById(id);
    }

    /**
     * Check if a tenant exists
     */
    public boolean tenantExists(UUID id) {
        return tenantService.getTenantById(id).isPresent();
    }

    /**
     * Get all users for a specific tenant
     */
    public List<User> getUsersByTenant(UUID tenantId) {
        return userService.getUsersByTenant(tenantId);
    }

    /**
     * Deactivate a tenant
     * This could involve setting a status flag or other operations
     */
    @Transactional
    public boolean deactivateTenant(int tenantId) {
        return tenantService.updateTenantStatus(tenantId, false);
    }

    /**
     * Activate a tenant
     */
    @Transactional
    public boolean activateTenant(int tenantId) {
        return tenantService.updateTenantStatus(tenantId, true);
    }

    /**
     * Initialize a super admin user if none exists
     */
    public User initializeSuperAdmin(String email, String password, String name, UUID tenantId) {
        // Check if a super admin already exists
        User existingSuperAdmin = userService.getSuperAdminByEmail(email);

        if (existingSuperAdmin != null) {
            return existingSuperAdmin;
        }

        // Create a new super admin
        User superAdmin = new User();
        superAdmin.setEmail(email);
        superAdmin.setPassword(password);
        superAdmin.setName(name);
        superAdmin.setRole("SUPER_ADMIN");
        superAdmin.setTenantId(tenantId);

        // Register without setting a tenant ID
        return userService.registerUser(superAdmin);
    }

    public boolean existsByContactEmail(String contactEmail) {
        return tenantRepo.existsByContactEmail(contactEmail);
    }
}