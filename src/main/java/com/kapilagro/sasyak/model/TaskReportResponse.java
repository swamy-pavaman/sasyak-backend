package com.kapilagro.sasyak.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskReportResponse {
    private int totalTasks;
    private Map<String, Integer> tasksByType;
    private Map<String, Integer> tasksByStatus;
    private Map<String, Integer> tasksByUser;
    private Map<String, Double> avgCompletionTimeByType; // in days
}
