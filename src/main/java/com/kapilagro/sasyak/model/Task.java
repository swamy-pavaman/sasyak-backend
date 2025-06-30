package com.kapilagro.sasyak.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Component
@Getter
@Setter
@Builder
public class Task {
    private int taskId;
    private UUID tenantId;
    private int createdById;
    private Integer assignedToId; // Can be null
    private String taskType;
    private String detailsJson; // JSONB in database
    private String imagesJson;  // JSONB in database
    private String description;
    private String implementationJson; // JSONB in database
    private String status; // "submitted", "approved", "rejected", "implemented"
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Kolkata")
    private OffsetDateTime createdAt;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Kolkata")
    private OffsetDateTime updatedAt;

    // Helper methods for status
    public boolean isSubmitted() {
        return "submitted".equalsIgnoreCase(status);
    }

    public boolean isApproved() {
        return "approved".equalsIgnoreCase(status);
    }

    public boolean isRejected() {
        return "rejected".equalsIgnoreCase(status);
    }

    public boolean isImplemented() {
        return "implemented".equalsIgnoreCase(status);
    }
}