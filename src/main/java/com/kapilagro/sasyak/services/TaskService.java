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
import java.util.stream.Collectors;

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

    // Count tasks by tenant
    public int countTasksByTenant(UUID tenantId) {
        return taskRepository.countByTenantId(tenantId);
    }

    // Count tasks created by a user
    public int countByCreatedBy(UUID tenantId, int userId) {
        return taskRepository.countByCreatedBy(tenantId, userId);
    }

    // Count tasks assigned to a user
    public int countByAssignedTo(UUID tenantId, int userId) {
        return taskRepository.countByAssignedTo(tenantId, userId);
    }

    public Map<String, Integer> getTaskStatusBreakdown(UUID tenantId) {
        return taskRepository.getTaskStatusBreakdown(tenantId);
    }

    public int countRecentTasksByTenant(UUID tenantId, int days) {
        return taskRepository.countRecentByTenantId(tenantId, days);
    }

    @Transactional
    public Task createTask(UUID tenantId, int createdById, String taskType, String description,
                           String detailsJson, String imagesJson, Integer assignedToId) {

        System.out.println("==== CREATE TASK START ====");
        System.out.println("TenantId: " + tenantId);
        System.out.println("CreatedById: " + createdById);
        System.out.println("TaskType: " + taskType);
        System.out.println("Description: " + description);

        // Create the task
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

        try {
            int taskId = taskRepository.save(task);
            task.setTaskId(taskId);
            System.out.println("Task saved with ID: " + taskId);

            // Get creator info for notifications
            System.out.println("Fetching creator info for user ID: " + createdById);
            Optional<User> creator = userRepository.getUserById(createdById);
            System.out.println("Creator present: " + creator.isPresent());

            String creatorName = creator.map(User::getName).orElse("A user");
            String creatorRole = creator.map(User::getRole).orElse("");

            System.out.println("Creator Name: " + creatorName);
            System.out.println("Creator Role: " + creatorRole);

            // Check if task creator is a supervisor or manager
            boolean isSupervisor = creator.isPresent() && "supervisor".equalsIgnoreCase(creator.get().getRole());
            boolean isManager = creator.isPresent() && "MANAGER".equalsIgnoreCase(creator.get().getRole());

            System.out.println("Is Supervisor: " + isSupervisor);
            System.out.println("Is Manager: " + isManager);

            if (isSupervisor) {
                // For supervisor-created tasks, notify only their direct manager
                System.out.println("User is a supervisor, notifying their manager");
                try {
                    notificationService.notifySupervisorManagerOfTaskCreation(
                            tenantId,
                            taskId,
                            taskType,
                            description,
                            createdById,
                            creatorName
                    );
                } catch (Exception e) {
                    System.out.println("Error in notifySupervisorManagerOfTaskCreation: " + e.getMessage());
                    e.printStackTrace();
                }
            } else if (isManager && assignedToId != null) {
                // For manager-created tasks, notify the assigned supervisor
                System.out.println("User is a manager, notifying assigned supervisor ID: " + assignedToId);
                try {
                    notificationService.createTaskAssignmentNotification(
                            tenantId,
                            assignedToId,
                            taskId,
                            "New Task Assigned",
                            creatorName + " has assigned you a new task."
                    );
                } catch (Exception e) {
                    System.out.println("Error in createTaskAssignmentNotification: " + e.getMessage());
                    e.printStackTrace();
                }
            } else {
                System.out.println("No specific notification required for this role or assignment");
            }

        } catch (Exception e) {
            System.out.println("Error in task creation: " + e.getMessage());
            e.printStackTrace();
            throw e; // Re-throw to maintain transaction behavior
        }

        System.out.println("==== CREATE TASK END ====");
        return task;
    }

    public Optional<Task> getTaskById(int taskId) {
        return taskRepository.getById(taskId);
    }

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

    public List<Task> getTasksCreatedByUser(UUID tenantId, int userId, int page, int size) {
        return taskRepository.getByCreatedBy(tenantId, userId, page, size);
    }

    public List<Task> getTasksAssignedToUser(UUID tenantId, int userId, int page, int size) {
        return taskRepository.getByAssignedTo(tenantId, userId, page, size);
    }

    public List<Task> getTasksByStatus(UUID tenantId, String status, int createdById, int page, int size) {
        return taskRepository.getByStatus(tenantId, status, createdById, page, size);
    }

    public List<Task> getAllTasks(UUID tenantId, int page, int size) {
        return taskRepository.getByTenantId(tenantId, page, size);
    }

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
            int toSendManagerId;
            if(creator.isPresent()&& creator.get().getRole().equalsIgnoreCase("manager")){
                toSendManagerId=creator.get().getUserId();
            }else{
                toSendManagerId=creator.get().getManagerId();
            }
                // Get implementer's name
            Optional<User> implementer = userRepository.getUserById(userId);
            String implementerName = implementer.map(User::getName).orElse("A user");
            notificationService.createTaskImplementationNotification(
                        task.getTenantId(),
                        toSendManagerId,
                        taskId,
                        "Task Implemented",
                        implementerName + " has implemented the task."
            );


//            // If there's a manager for the implementer, notify them too
//            Optional<User> implementer = userRepository.getUserById(userId);
//            if (implementer.isPresent() && implementer.get().getManagerId() != null) {
//                // Get implementer's name
//                String implementerName = implementer.map(User::getName).orElse("A user");
//
//                notificationService.createTaskImplementationNotification(
//                        task.getTenantId(),
//                        implementer.get().getManagerId(),
//                        taskId,
//                        "Task Implemented by Team Member",
//                        implementerName + " has implemented a task."
//                );
//            }
        }

        return updated;
    }

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

//    public List<Task> getTasksByType(UUID tenantId, String taskType, int page, int size) {
//        return taskRepository.getByTaskType(tenantId, taskType, page, size);
//    }
public List<Task> getTasksByType(UUID tenantId, String taskType, int createdById, int page, int size) {
    return taskRepository.getByTaskType(tenantId, taskType, createdById, page, size);
}


    public int countTasksByType(UUID tenantId, String taskType) {
        return taskRepository.countByTaskType(tenantId, taskType);
    }

    public List<Map<String, Object>> getTasksCompletedPerDay(UUID tenantId, int days) {
        LocalDateTime endDate = LocalDateTime.now();
        LocalDateTime startDate = endDate.minusDays(days);
        return taskRepository.getTasksCompletedPerDay(tenantId, startDate, endDate);
    }

    public List<Map<String, Object>> getTasksCreatedPerDay(UUID tenantId, int days) {
        LocalDateTime endDate = LocalDateTime.now();
        LocalDateTime startDate = endDate.minusDays(days);
        return taskRepository.getTasksCreatedPerDay(tenantId, startDate, endDate);
    }

    public Map<String, Object> getTaskCompletionRateByUser(UUID tenantId) {
        return taskRepository.getTaskCompletionRateByUser(tenantId);
    }

    public Map<String, Object> getDetailedTaskReport(UUID tenantId, int days) {
        Map<String, Object> report = new HashMap<>();

        // Basic report data
        report.putAll(getTaskReport(tenantId));

        // Add trend data
        report.put("tasksCompletedTrend", getTasksCompletedPerDay(tenantId, days));
        report.put("tasksCreatedTrend", getTasksCreatedPerDay(tenantId, days));

        // Add user performance data
        report.put("avgCompletionTimeByUser", taskRepository.getAvgCompletionTimeByUser(tenantId));
        report.put("taskCompletionRateByUser", getTaskCompletionRateByUser(tenantId));

        return report;
    }

    public Map<String, Object> getEfficiencyReport(UUID tenantId) {
        Map<String, Object> report = new HashMap<>();

        // Get average completion times by type
        Map<String, Double> avgTimesByType = taskRepository.getAvgCompletionTimeByType(tenantId);
        report.put("avgTimesByType", avgTimesByType);

        // Get average completion times by user
        Map<String, Double> avgTimesByUser = taskRepository.getAvgCompletionTimeByUser(tenantId);
        report.put("avgTimesByUser", avgTimesByUser);

        // Get the best performers (users with the fastest completion times)
        report.put("bestPerformers", avgTimesByUser.entrySet().stream()
                .sorted(Map.Entry.comparingByValue())
                .limit(5)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));

        return report;
    }

    public List<Task> getTasksByManager(UUID tenantId, int managerId, int page, int size) {
        return taskRepository.getTasksByManager(tenantId, managerId, page, size);
    }
}