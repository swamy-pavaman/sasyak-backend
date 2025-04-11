package com.kapilagro.sasyak.services;

import com.kapilagro.sasyak.repository.TaskRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Service
public class TaskService {

    private final TaskRepo taskRepository;

    @Autowired
    public TaskService(TaskRepo taskRepository) {
        this.taskRepository = taskRepository;
    }

    public int countTasksByTenant(UUID tenantId) {
        return taskRepository.countByTenantId(tenantId);
    }

    public Map<String, Integer> getTaskStatusBreakdown(UUID tenantId) {
        return taskRepository.getTaskStatusBreakdown(tenantId);
    }

    public int countRecentTasksByTenant(UUID tenantId, int days) {
        return taskRepository.countRecentByTenantId(tenantId, days);
    }
}