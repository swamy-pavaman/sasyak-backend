package com.kapilagro.sasyak.services;

import com.kapilagro.sasyak.model.Task;
import com.kapilagro.sasyak.model.TaskAdvice;
import com.kapilagro.sasyak.model.TaskAdviceDTO;
import com.kapilagro.sasyak.model.User;
import com.kapilagro.sasyak.repository.TaskAdviceRepo;
import com.kapilagro.sasyak.repository.TaskRepo;
import com.kapilagro.sasyak.repository.UserRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class TaskAdviceService {

    private final TaskAdviceRepo taskAdviceRepository;
    private final TaskRepo taskRepository;
    private final UserRepo userRepository;
    private final NotificationService notificationService;

    @Autowired
    public TaskAdviceService(TaskAdviceRepo taskAdviceRepository,
                             TaskRepo taskRepository,
                             UserRepo userRepository,
                             NotificationService notificationService) {
        this.taskAdviceRepository = taskAdviceRepository;
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    // Existing method
    public int countAdviceByTenant(UUID tenantId) {
        return taskAdviceRepository.countByTenantId(tenantId);
    }

    // New methods for task advice management

    // Create a new task advice
    @Transactional
    public TaskAdvice createAdvice(UUID tenantId, int managerId, int taskId, String adviceText) {
        // Verify task exists and belongs to the tenant
        Optional<Task> taskOpt = taskRepository.getById(taskId);
        if (taskOpt.isEmpty() || !taskOpt.get().getTenantId().equals(tenantId)) {
            throw new IllegalArgumentException("Task not found or does not belong to this tenant");
        }

        // Create the advice
        TaskAdvice advice = TaskAdvice.builder()
                .tenantId(tenantId)
                .taskId(taskId)
                .managerId(managerId)
                .adviceText(adviceText)
                .build();

        int adviceId = taskAdviceRepository.save(advice);
        advice.setAdviceId(adviceId);

        // Notify the task creator
        Task task = taskOpt.get();

        // Get manager's name
        Optional<User> manager = userRepository.getUserById(managerId);
        String managerName = manager.map(User::getName).orElse("A manager");

        notificationService.createAdviceNotification(
                tenantId,
                task.getCreatedById(),
                taskId,
                "New Advice Received",
                managerName + " has provided advice on your task."
        );

        // If task is assigned to someone, notify them too (if they're not the creator)
        if (task.getAssignedToId() != null && task.getAssignedToId() != task.getCreatedById()) {
            notificationService.createAdviceNotification(
                    tenantId,
                    task.getAssignedToId(),
                    taskId,
                    "New Advice Received",
                    managerName + " has provided advice on a task assigned to you."
            );
        }

        return advice;
    }

    // Get all advice for a task
    public List<TaskAdvice> getAdviceForTask(int taskId) {
        return taskAdviceRepository.getByTaskId(taskId);
    }

    // Get all advice provided by a manager
    public List<TaskAdvice> getAdviceByManager(UUID tenantId, int managerId) {
        return taskAdviceRepository.getByManagerId(tenantId, managerId);
    }

    // Convert TaskAdvice to TaskAdviceDTO
    public TaskAdviceDTO convertToDTO(TaskAdvice advice) {
        // Get manager name
        String managerName = userRepository.getUserById(advice.getManagerId())
                .map(User::getName)
                .orElse("Unknown");

        return TaskAdviceDTO.builder()
                .id(advice.getAdviceId())
                .taskId(advice.getTaskId())
                .managerName(managerName)
                .adviceText(advice.getAdviceText())
                .createdAt(advice.getCreatedAt())
                .build();
    }

    // Get all advice for a task as DTOs
    public List<TaskAdviceDTO> getAdviceForTaskAsDTO(int taskId) {
        return taskAdviceRepository.getByTaskId(taskId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
}