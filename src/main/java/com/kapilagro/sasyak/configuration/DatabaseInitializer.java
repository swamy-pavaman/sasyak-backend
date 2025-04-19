package com.kapilagro.sasyak.configuration;

import com.kapilagro.sasyak.model.User;
import com.kapilagro.sasyak.services.SuperAdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class DatabaseInitializer implements CommandLineRunner {

    @Autowired
    private SuperAdminService superAdminService;

    @Autowired
    private PasswordEncoder passwordEncoder; // ✅ Add this line


    @Value("${superadmin.email:superadmin@example.com}")
    private String superAdminEmail;

    @Value("${superadmin.password:1010}")
    private String superAdminPassword;

    @Value("${superadmin.name:Super Admin}")
    private String superAdminName;

    @Value("${superadmin.tenantid:91ddd580-f145-4a94-a0b8-d20e4c72c662}")
    private String tenantId;


    @Override
    public void run(String... args) {
        // Initialize super admin if it doesn't already exist


        User superAdmin = superAdminService.initializeSuperAdmin(
                superAdminEmail,
                superAdminPassword,
                superAdminName,
                UUID.fromString(tenantId)
        );

        System.out.println("Super Admin initialized with email: " + superAdmin.getEmail()+" "+superAdminPassword);
    }
}