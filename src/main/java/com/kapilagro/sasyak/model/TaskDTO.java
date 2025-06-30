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
@NoArgsConstructor
@AllArgsConstructor
public class TaskDTO {
    private int id;
    private String taskType;
    private String description;
    private String status;
    private String createdBy; // Name of person who created
    private String assignedTo; // Name of person assigned to (if any)

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Kolkata")
    private OffsetDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Kolkata")
    private OffsetDateTime updatedAt;
    private String detailsJson;
    private String imagesJson;
    private String implementationJson;
}