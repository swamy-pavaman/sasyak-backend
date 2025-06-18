package com.kapilagro.sasyak.model;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Catalog {
    private int id;
    private String category;
    private String value;
    private String details;
    private UUID tenantId;
    private int createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
