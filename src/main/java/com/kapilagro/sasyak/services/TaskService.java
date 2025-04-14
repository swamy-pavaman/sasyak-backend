// File: TaskService.java (Extended version)
package com.kapilagro.sasyak.services;

import com.kapilagro.sasyak.model.Task;
import com.kapilagro.sasyak.model.TaskDTO;
import com.kapilagro.sasyak.model.User;
import com.kapilagro.sasyak.repository.TaskRepo;
import com.kapilagro.sasyak.repository.UserRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class TaskService {

    private final TaskRepo taskRepository;
    private final UserRepo userRepository;
    private final NotificationService notificationService;

    @Autowired
    public TaskService(TaskRepo taskRepository, UserRepo userRepository, NotificationService notificationService) {
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    // Existing methods
    public int countTasksByTenant(UUID tenantId) {
        return taskRepository.countByTenantId(tenantId);
    }

    public Map<String, Integer> getTaskStatusBreakdown(UUID tenantId) {
        return taskRepository.getTaskStatusBreakdown(tenantId);
    }

    public int countRecentTasksByTenant(UUID tenantId, int days) {
        return taskRepository.countRecentByTenantId(tenantId, days);
    }

    // New methods for task management

    // Create a new task
    @Transactional
    public Task createTask(UUID tenantId, int createdById, String taskType, String description,
                           String detailsJson, String imagesJson, Integer assignedToId) {
        // Validate if assignedToId exists and belongs to the tenant
        if (assignedToId != null) {
            Optional<User> assignedUser = userRepository.getUserById(assignedToId);
            if (assignedUser.isEmpty() || !assignedUser.get().getTenantId().equals(tenantId)) {
                throw new IllegalArgumentException("Assigned user not found or does not belong to this tenant");
            }
        }

        Task task = Task.builder()
                .tenantId(tenantId)
                .createdById(createdById)
                .assignedToId(assignedToId)
                .taskType(taskType)
                .description(description)
                .detailsJson(detailsJson)
                .imagesJson(imagesJson)
                .status("submitted")
                .build();

        int taskId = taskRepository.save(task);
        task.setTaskId(taskId);

        // If task is assigned to someone, notify them
        if (assignedToId != null) {
            // Get creator's name for the notification message
            Optional<User> creator = userRepository.getUserById(createdById);
            String creatorName = creator.map(User::getName).orElse("A user");

            notificationService.createTaskAssignmentNotification(
                    tenantId,
                    assignedToId,
                    taskId,
                    "New Task Assigned",
                    creatorName + " has assigned you a new task."
            );
        }

        return task;
    }

    // Get a specific task
    public Optional<Task> getTaskById(int taskId) {
        return taskRepository.getById(taskId);
    }

    // Check if user has access to a task (created it, assigned to it, or is a manager)
    public boolean userHasAccessToTask(int userId, int taskId, UUID tenantId) {
        Optional<Task> taskOpt = taskRepository.getById(taskId);
        if (taskOpt.isEmpty() || !taskOpt.get().getTenantId().equals(tenantId)) {
            return false;
        }

        Task task = taskOpt.get();

        // User created the task
        if (task.getCreatedById() == userId) {
            return true;
        }

        // User is assigned to the task
        if (task.getAssignedToId() != null && task.getAssignedToId() == userId) {
            return true;
        }

        // Check if user is a manager for the task creator or assignee
        Optional<User> creator = userRepository.getUserById(task.getCreatedById());
        if (creator.isPresent() && creator.get().getManagerId() != null && creator.get().getManagerId() == userId) {
            return true;
        }

        if (task.getAssignedToId() != null) {
            Optional<User> assignee = userRepository.getUserById(task.getAssignedToId());
            if (assignee.isPresent() && assignee.get().getManagerId() != null && assignee.get().getManagerId() == userId) {
                return true;
            }
        }

        // Check if user is an ADMIN
        Optional<User> user = userRepository.getUserById(userId);
        return user.filter(u -> "ADMIN".equalsIgnoreCase(u.getRole())).isPresent();
    }

    // Get tasks created by a user
    public List<Task> getTasksCreatedByUser(UUID tenantId, int userId, int page, int size) {
        return taskRepository.getByCreatedBy(tenantId, userId, page, size);
    }

    // Get tasks assigned to a user
    public List<Task> getTasksAssignedToUser(UUID tenantId, int userId, int page, int size) {
        return taskRepository.getByAssignedTo(tenantId, userId, page, size);
    }

    // Get tasks by status
    public List<Task> getTasksByStatus(UUID tenantId, String status, int page, int size) {
        return taskRepository.getByStatus(tenantId, status, page, size);
    }

    // Get all tasks for a tenant with pagination
    public List<Task> getAllTasks(UUID tenantId, int page, int size) {
        return taskRepository.getByTenantId(tenantId, page, size);
    }

    // Update task status
    @Transactional
    public boolean updateTaskStatus(int taskId, String status, int userId) {
        Optional<Task> taskOpt = taskRepository.getById(taskId);
        if (taskOpt.isEmpty()) {
            return false;
        }

        Task task = taskOpt.get();
        boolean updated = taskRepository.updateStatus(taskId, status);

        if (updated) {
            // Get the user who created the task for notification
            Optional<User> creator = userRepository.getUserById(task.getCreatedById());

            // Notify the task creator if they're not the one updating the status
            if (creator.isPresent() && creator.get().getUserId() != userId) {
                // Get updater's name
                Optional<User> updater = userRepository.getUserById(userId);
                String updaterName = updater.map(User::getName).orElse("A user");

                notificationService.createTaskStatusNotification(
                        task.getTenantId(),
                        task.getCreatedById(),
                        taskId,
                        "Task Status Updated",
                        updaterName + " has updated the status of your task to " + status
                );
            }

            // If task is assigned to someone, notify them too (if they didn't update it)
            if (task.getAssignedToId() != null && task.getAssignedToId() != userId) {
                // Get updater's name
                Optional<User> updater = userRepository.getUserById(userId);
                String updaterName = updater.map(User::getName).orElse("A user");

                notificationService.createTaskStatusNotification(
                        task.getTenantId(),
                        task.getAssignedToId(),
                        taskId,
                        "Task Status Updated",
                        updaterName + " has updated the status of a task assigned to you to " + status
                );
            }
        }

        return updated;
    }

    // Update task implementation
    @Transactional
    public boolean updateTaskImplementation(int taskId, String implementationJson, int userId) {
        Optional<Task> taskOpt = taskRepository.getById(taskId);
        if (taskOpt.isEmpty()) {
            return false;
        }

        Task task = taskOpt.get();
        boolean updated = taskRepository.updateImplementation(taskId, implementationJson);

        if (updated) {
            // Get the user who created the task for notification
            Optional<User> creator = userRepository.getUserById(task.getCreatedById());

            // Notify the task creator if they're not the one updating
            if (creator.isPresent() && creator.get().getUserId() != userId) {
                // Get implementer's name
                Optional<User> implementer = userRepository.getUserById(userId);
                String implementerName = implementer.map(User::getName).orElse("A user");

                notificationService.createTaskImplementationNotification(
                        task.getTenantId(),
                        task.getCreatedById(),
                        taskId,
                        "Task Implemented",
                        implementerName + " has implemented the task."
                );
            }

            // If there's a manager for the implementer, notify them too
            Optional<User> implementer = userRepository.getUserById(userId);
            if (implementer.isPresent() && implementer.get().getManagerId() != null) {
                // Get implementer's name
                String implementerName = implementer.map(User::getName).orElse("A user");

                notificationService.createTaskImplementationNotification(
                        task.getTenantId(),
                        implementer.get().getManagerId(),
                        taskId,
                        "Task Implemented by Team Member",
                        implementerName + " has implemented a task."
                );
            }
        }

        return updated;
    }

    // Assign task to a user
    @Transactional
    public boolean assignTask(int taskId, int assignedToId, int assignerId) {
        Optional<Task> taskOpt = taskRepository.getById(taskId);
        if (taskOpt.isEmpty()) {
            return false;
        }

        // Validate if assignedToId exists
        Optional<User> assignedUser = userRepository.getUserById(assignedToId);
        if (assignedUser.isEmpty() || !assignedUser.get().getTenantId().equals(taskOpt.get().getTenantId())) {
            throw new IllegalArgumentException("Assigned user not found or does not belong to this tenant");
        }

        boolean updated = taskRepository.assignTask(taskId, assignedToId);

        if (updated) {
            // Get assigner's name
            Optional<User> assigner = userRepository.getUserById(assignerId);
            String assignerName = assigner.map(User::getName).orElse("A user");

            notificationService.createTaskAssignmentNotification(
                    taskOpt.get().getTenantId(),
                    assignedToId,
                    taskId,
                    "Task Assigned to You",
                    assignerName + " has assigned a task to you."
            );
        }

        return updated;
    }

    // Convert Task to TaskDTO with user names
    public TaskDTO convertToDTO(Task task) {
        // Get creator name
        String createdByName = userRepository.getUserById(task.getCreatedById())
                .map(User::getName)
                .orElse("Unknown");

        // Get assignee name if present
        String assignedToName = null;
        if (task.getAssignedToId() != null) {
            assignedToName = userRepository.getUserById(task.getAssignedToId())
                    .map(User::getName)
                    .orElse("Unknown");
        }

        return TaskDTO.builder()
                .id(task.getTaskId())
                .taskType(task.getTaskType())
                .description(task.getDescription())
                .status(task.getStatus())
                .createdBy(createdByName)
                .assignedTo(assignedToName)
                .createdAt(task.getCreatedAt())
                .updatedAt(task.getUpdatedAt())
                .detailsJson(task.getDetailsJson())
                .imagesJson(task.getImagesJson())
                .implementationJson(task.getImplementationJson())
                .build();
    }

    // Get task report for a tenant
    public Map<String, Object> getTaskReport(UUID tenantId) {
        Map<String, Object> report = new HashMap<>();

        // Total tasks
        int totalTasks = taskRepository.countByTenantId(tenantId);
        report.put("totalTasks", totalTasks);

        // Tasks by status
        Map<String, Integer> statusCounts = taskRepository.getTaskStatusBreakdown(tenantId);
        report.put("tasksByStatus", statusCounts);

        // Tasks by type
        Map<String, Integer> typeCounts = taskRepository.getTaskCountsByType(tenantId);
        report.put("tasksByType", typeCounts);

        // Tasks by user
        Map<String, Integer> userCounts = taskRepository.getTaskCountsByUser(tenantId);
        report.put("tasksByUser", userCounts);

        // Average completion time by type
        Map<String, Double> avgCompletionTimes = taskRepository.getAvgCompletionTimeByType(tenantId);
        report.put("avgCompletionTimeByType", avgCompletionTimes);

        return report;
    }
}