package com.kapilagro.sasyak.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CreateTenantResponse {
    private String message;
    private TenantDTO tenant;
    private AdminUserDTO adminUser;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class TenantDTO {
        private String id;
        private String companyName;
        private String contactEmail;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class AdminUserDTO {
        private int id;
        private String name;
        private String email;
        private String autogeneratedPassword;
    }
}
