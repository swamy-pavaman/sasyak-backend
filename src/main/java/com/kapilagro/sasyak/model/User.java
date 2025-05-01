package com.kapilagro.sasyak.model;

import lombok.*;
import org.springframework.stereotype.Component;

import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Component
@Getter
@Setter
@Builder
public class User {
    private int userId;
    private String name;
    private String email;
    private String phone_number;
    private String role; // "SUPER_ADMIN", "ADMIN", "MANAGER", "SUPERVISOR", "EMPLOYEE"
    private String password;
    private String profile;
    private String location;

//    private String oauthProvider;
//    private String oAuthProviderId;
    private UUID tenantId; // New field to link user to a tenant
    private Integer managerId;
}