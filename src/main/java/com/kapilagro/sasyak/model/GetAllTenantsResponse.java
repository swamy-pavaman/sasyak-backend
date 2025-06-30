package com.kapilagro.sasyak.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GetAllTenantsResponse {
    private java.util.List<TenantDTO> tenants;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class TenantDTO {
        private UUID id;
        private String companyName;
        private String contactEmail;

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Kolkata")
        private OffsetDateTime createdAt;
    }
}