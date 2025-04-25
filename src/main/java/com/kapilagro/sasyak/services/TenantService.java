package com.kapilagro.sasyak.services;

import com.kapilagro.sasyak.model.Tenant;
import com.kapilagro.sasyak.model.User;
import com.kapilagro.sasyak.repository.TenantRepo;
import com.kapilagro.sasyak.utils.GeneratePasswordUtility;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

@Service
public class TenantService {

    @Autowired
    private TenantRepo tenantRepo;

    @Autowired
    private UserService userService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private GeneratePasswordUtility generateRandomPassword;

    /**
     * Get all tenants
     */
    public List<Tenant> getAllTenants() {
        return tenantRepo.getAllTenants();
    }

    /**
     * Get tenant by ID
     */
    public Optional<Tenant> getTenantById(UUID id) {
        return tenantRepo.getTenantById(id);
    }

    /**
     * Create a new tenant along with its admin user
     */
    @Transactional
    public TenantCreationResult createTenantWithAdmin(Tenant tenant, User adminUser) {
        // Set current time for tenant creation
        tenant.setCreatedAt(LocalDateTime.now());

        // Set active status for new tenant
        tenant.setActive(true);

        // Save the tenant first
        UUID tenantId = tenantRepo.save(tenant);
        tenant.setTenantId(tenantId);

        // Generate a random password for the admin
        String generatedPassword = generateRandomPassword.generateRandomPassword();
        String rawPassword = generatedPassword;



        // Set up the admin user
        adminUser.setTenantId(tenantId);
        adminUser.setRole("ADMIN"); // Ensure role is set to ADMIN

        adminUser.setPassword(passwordEncoder.encode(generatedPassword));

        // Save the admin user
        int userId = userService.registerUserWithoutAuthentication(adminUser);
        adminUser.setUserId(userId);

        // Return both the tenant and admin info with the raw password
        return new TenantCreationResult(tenant, adminUser, rawPassword);
    }

    /**
     * Update tenant status (active/inactive)
     */
    @Transactional
    public boolean updateTenantStatus(int tenantId, boolean active) {
        return tenantRepo.updateTenantStatus(tenantId, active);
    }

    /**
     * Helper class to return both tenant and admin user with the generated password
     */
    public static class TenantCreationResult {
        private final Tenant tenant;
        private final User adminUser;
        private final String generatedPassword;

        public TenantCreationResult(Tenant tenant, User adminUser, String generatedPassword) {
            this.tenant = tenant;
            this.adminUser = adminUser;
            this.generatedPassword = generatedPassword;
        }

        public Tenant getTenant() {
            return tenant;
        }

        public User getAdminUser() {
            return adminUser;
        }

        public String getGeneratedPassword() {
            return generatedPassword;
        }
    }

    /**
     * Generate a random secure password
     //     */
//    private String generateRandomPassword() {
//        String upperCaseLetters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
//        String lowerCaseLetters = "abcdefghijklmnopqrstuvwxyz";
//        String numbers = "0123456789";
//        String specialCharacters = "!@#$%^&*()-_=+[]{}|;:,.<>?";
//
//        String allChars = upperCaseLetters + lowerCaseLetters + numbers + specialCharacters;
//        Random random = new Random();
//
//        // Generate a password of length 12
//        char[] password = new char[12];
//
//        // Ensure at least one character from each category
//        password[0] = upperCaseLetters.charAt(random.nextInt(upperCaseLetters.length()));
//        password[1] = lowerCaseLetters.charAt(random.nextInt(lowerCaseLetters.length()));
//        password[2] = numbers.charAt(random.nextInt(numbers.length()));
//        password[3] = specialCharacters.charAt(random.nextInt(specialCharacters.length()));
//
//        // Fill the rest randomly
//        for (int i = 4; i < 12; i++) {
//            password[i] = allChars.charAt(random.nextInt(allChars.length()));
//        }
//
//        // Shuffle the password characters
//        for (int i = 0; i < password.length; i++) {
//            int j = random.nextInt(password.length);
//            char temp = password[i];
//            password[i] = password[j];
//            password[j] = temp;
//        }
//
//        return new String(password);
//    }
}