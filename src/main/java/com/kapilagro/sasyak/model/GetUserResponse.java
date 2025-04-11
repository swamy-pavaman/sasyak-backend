package com.kapilagro.sasyak.model;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;
@Data
@Component
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GetUserResponse {

    private int userId;
    private String name;
    private String email;
    private String phoneNumber;
    private String role;
    //TODO hiding the tenant id for security resons if it nesssesary ill add it again
    //private UUID tenantId;
    private Integer managerId;
}

