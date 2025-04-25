package com.kapilagro.sasyak.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserManagerDTO {
    private Integer id;
    private String name;
    private String email;
    private String role;
    private UUID tenantId;
    private String phoneNumber;

}