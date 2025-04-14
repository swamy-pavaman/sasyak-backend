package com.kapilagro.sasyak.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateTaskRequest {
    private String taskType;
    private String description;
    private String detailsJson;
    private String imagesJson;
    private Integer assignedToId; // Optional, can be null
}
