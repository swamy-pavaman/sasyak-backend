package com.kapilagro.sasyak.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DashBoardResponse {
    private int totalEmployees;
    private int totalSupervisors;
    private int totalManagers;
    private int totalTasks;
    private Map<String, Integer> taskStatusBreakdown;
    private int recentTasks;
    private int adviceCount;
    private String errorMessage;
}