package com.kapilagro.sasyak.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CreateTenantRequest {
    private String companyName;
    private String contactEmail;
    private String adminName;
    private String adminEmail;
    private String role; // Optional, defaults to "ADMIN"
}