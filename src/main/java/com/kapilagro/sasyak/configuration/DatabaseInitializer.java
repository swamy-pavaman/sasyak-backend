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
    private PasswordEncoder passwordEncoder;


    @Value("${superadmin.email:}")
    private String superAdminEmail;

    @Value("${superadmin.password}")
    private String superAdminPassword;

    @Value("${superadmin.name}")
    private String superAdminName;

    @Value("${superadmin.tenantid}")
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

    }
}