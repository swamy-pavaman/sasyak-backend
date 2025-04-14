package com.kapilagro.sasyak.controller;

import com.kapilagro.sasyak.model.*;
import com.kapilagro.sasyak.services.TaskAdviceService;
import com.kapilagro.sasyak.services.TaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@CrossOrigin("*")
@RequestMapping("/api/tasks")
public class TaskController {

    @Autowired
    private TaskService taskService;

    @Autowired
    private TaskAdviceService taskAdviceService;

    // Helper method to get the current user from the authentication context
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return (User) authentication.getPrincipal();
    }

    // Create a new task
    @PostMapping
    @PreAuthorize("hasAnyRole('MANAGER', 'SUPERVISOR', 'ADMIN')")
    public ResponseEntity<?> createTask(@RequestBody CreateTaskRequest request) {
        try {
            User currentUser = getCurrentUser();
            UUID tenantId = currentUser.getTenantId();

            Task createdTask = taskService.createTask(
                    tenantId,
                    currentUser.getUserId(),
                    request.getTaskType(),
                    request.getDescription(),
                    request.getDetailsJson(),
                    request.getImagesJson(),
                    request.getAssignedToId()
            );

            TaskDTO taskDTO = taskService.convertToDTO(createdTask);

            return ResponseEntity.status(HttpStatus.CREATED).body(taskDTO);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error creating task: " + e.getMessage());
        }
    }

    // Get a specific task
    @GetMapping("/{taskId}")
    @PreAuthorize("hasAnyRole('MANAGER', 'SUPERVISOR', 'ADMIN', 'EMPLOYEE')")
    public ResponseEntity<?> getTask(@PathVariable int taskId) {
        try {
            User currentUser = getCurrentUser();
            UUID tenantId = currentUser.getTenantId();

            // Check if user has access to the task
            if (!taskService.userHasAccessToTask(currentUser.getUserId(), taskId, tenantId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("You don't have permission to access this task");
            }

            Optional<Task> taskOpt = taskService.getTaskById(taskId);

            if (taskOpt.isPresent()) {
                Task task = taskOpt.get();
                TaskDTO taskDTO = taskService.convertToDTO(task);

                // Get advice for this task
                List<TaskAdviceDTO> advices = taskAdviceService.getAdviceForTaskAsDTO(taskId);

                TaskDetailResponse response = TaskDetailResponse.builder()
                        .task(taskDTO)
                        .advices(advices)
                        .build();

                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("Task not found with ID: " + taskId);
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error retrieving task: " + e.getMessage());
        }
    }

    // Get tasks created by the current user
    @GetMapping("/created")
    @PreAuthorize("hasAnyRole('MANAGER', 'SUPERVISOR', 'ADMIN')")
    public ResponseEntity<?> getTasksCreatedByMe(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            User currentUser = getCurrentUser();
            UUID tenantId = currentUser.getTenantId();

            List<Task> tasks = taskService.getTasksCreatedByUser(tenantId, currentUser.getUserId(), page, size);
            int total = taskService.countTasksByTenant(tenantId);

            List<TaskDTO> taskDTOs = tasks.stream()
                    .map(taskService::convertToDTO)
                    .collect(Collectors.toList());

            TaskListResponse response = TaskListResponse.builder()
                    .tasks(taskDTOs)
                    .totalCount(total)
                    .build();

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error retrieving tasks: " + e.getMessage());
        }
    }

    // Get tasks assigned to the current user
    @GetMapping("/assigned")
    @PreAuthorize("hasAnyRole('MANAGER', 'SUPERVISOR', 'ADMIN', 'EMPLOYEE')")
    public ResponseEntity<?> getTasksAssignedToMe(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            User currentUser = getCurrentUser();
            UUID tenantId = currentUser.getTenantId();

            List<Task> tasks = taskService.getTasksAssignedToUser(tenantId, currentUser.getUserId(), page, size);
            int total = taskService.countTasksByTenant(tenantId);

            List<TaskDTO> taskDTOs = tasks.stream()
                    .map(taskService::convertToDTO)
                    .collect(Collectors.toList());

            TaskListResponse response = TaskListResponse.builder()
                    .tasks(taskDTOs)
                    .totalCount(total)
                    .build();

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error retrieving tasks: " + e.getMessage());
        }
    }

    // Get all tasks (for admins and managers)
    @GetMapping
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<?> getAllTasks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            User currentUser = getCurrentUser();
            UUID tenantId = currentUser.getTenantId();

            List<Task> tasks = taskService.getAllTasks(tenantId, page, size);
            int total = taskService.countTasksByTenant(tenantId);

            List<TaskDTO> taskDTOs = tasks.stream()
                    .map(taskService::convertToDTO)
                    .collect(Collectors.toList());

            TaskListResponse response = TaskListResponse.builder()
                    .tasks(taskDTOs)
                    .totalCount(total)
                    .build();

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error retrieving tasks: " + e.getMessage());
        }
    }

    // Get tasks by status
    @GetMapping("/status/{status}")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<?> getTasksByStatus(
            @PathVariable String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            User currentUser = getCurrentUser();
            UUID tenantId = currentUser.getTenantId();

            List<Task> tasks = taskService.getTasksByStatus(tenantId, status, page, size);

            List<TaskDTO> taskDTOs = tasks.stream()
                    .map(taskService::convertToDTO)
                    .collect(Collectors.toList());

            TaskListResponse response = TaskListResponse.builder()
                    .tasks(taskDTOs)
                    .totalCount(taskDTOs.size()) // Simple count of returned tasks
                    .build();

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error retrieving tasks: " + e.getMessage());
        }
    }

    // Update task status
    @PutMapping("/{taskId}/status")
    @PreAuthorize("hasAnyRole('MANAGER', 'SUPERVISOR', 'ADMIN')")
    public ResponseEntity<?> updateTaskStatus(
            @PathVariable int taskId,
            @RequestBody UpdateTaskStatusRequest request) {
        try {
            User currentUser = getCurrentUser();
            UUID tenantId = currentUser.getTenantId();

            // Check if user has access to the task
            if (!taskService.userHasAccessToTask(currentUser.getUserId(), taskId, tenantId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("You don't have permission to update this task");
            }

            boolean updated = taskService.updateTaskStatus(taskId, request.getStatus(), currentUser.getUserId());

            if (updated) {
                Optional<Task> updatedTask = taskService.getTaskById(taskId);
                return ResponseEntity.ok(taskService.convertToDTO(updatedTask.get()));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("Task not found with ID: " + taskId);
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error updating task status: " + e.getMessage());
        }
    }

    // Update task implementation
    @PutMapping("/{taskId}/implementation")
    @PreAuthorize("hasAnyRole('SUPERVISOR', 'EMPLOYEE')")
    public ResponseEntity<?> updateTaskImplementation(
            @PathVariable int taskId,
            @RequestBody TaskImplementationRequest request) {
        try {
            User currentUser = getCurrentUser();
            UUID tenantId = currentUser.getTenantId();

            // Check if user has access to the task
            if (!taskService.userHasAccessToTask(currentUser.getUserId(), taskId, tenantId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("You don't have permission to update this task");
            }

            boolean updated = taskService.updateTaskImplementation(taskId, request.getImplementationJson(), currentUser.getUserId());

            if (updated) {
                Optional<Task> updatedTask = taskService.getTaskById(taskId);
                return ResponseEntity.ok(taskService.convertToDTO(updatedTask.get()));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("Task not found with ID: " + taskId);
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error updating task implementation: " + e.getMessage());
        }
    }

    // Assign task to a user
    @PutMapping("/{taskId}/assign/{assignedToId}")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<?> assignTask(
            @PathVariable int taskId,
            @PathVariable int assignedToId) {
        try {
            User currentUser = getCurrentUser();
            UUID tenantId = currentUser.getTenantId();

            // Check if user has access to the task
            if (!taskService.userHasAccessToTask(currentUser.getUserId(), taskId, tenantId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("You don't have permission to update this task");
            }

            boolean updated = taskService.assignTask(taskId, assignedToId, currentUser.getUserId());

            if (updated) {
                Optional<Task> updatedTask = taskService.getTaskById(taskId);
                return ResponseEntity.ok(taskService.convertToDTO(updatedTask.get()));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("Task not found with ID: " + taskId);
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error assigning task: " + e.getMessage());
        }
    }

    // Get task report (for managers and admins)
    @GetMapping("/report")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<?> getTaskReport() {
        try {
            User currentUser = getCurrentUser();
            UUID tenantId = currentUser.getTenantId();

            Map<String, Object> report = taskService.getTaskReport(tenantId);

            TaskReportResponse response = TaskReportResponse.builder()
                    .totalTasks((int) report.get("totalTasks"))
                    .tasksByStatus((Map<String, Integer>) report.get("tasksByStatus"))
                    .tasksByType((Map<String, Integer>) report.get("tasksByType"))
                    .tasksByUser((Map<String, Integer>) report.get("tasksByUser"))
                    .avgCompletionTimeByType((Map<String, Double>) report.get("avgCompletionTimeByType"))
                    .build();

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error generating task report: " + e.getMessage());
        }
    }
}