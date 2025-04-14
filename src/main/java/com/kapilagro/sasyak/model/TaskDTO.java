package com.kapilagro.sasyak.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskDTO {
    private int id;
    private String taskType;
    private String description;
    private String status;
    private String createdBy; // Name of person who created
    private String assignedTo; // Name of person assigned to (if any)
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String detailsJson;
    private String imagesJson;
    private String implementationJson;
}