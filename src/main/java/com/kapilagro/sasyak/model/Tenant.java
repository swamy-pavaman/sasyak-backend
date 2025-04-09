package com.kapilagro.sasyak.model;

import lombok.*;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Component
@Getter
@Setter
@Builder
public class Tenant {
    private UUID tenantId;
    private String companyName;
    private String contactEmail;
    private LocalDateTime createdAt;
    private boolean active;
}