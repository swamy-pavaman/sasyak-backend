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
import java.util.UUID;

@RestController
@RequestMapping("/api/task-advices")
public class TaskAdviceController {

    @Autowired
    private TaskAdviceService taskAdviceService;

    @Autowired
    private TaskService taskService;

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return (User) authentication.getPrincipal();
    }

    // Create a new task advice (returns TaskAdviceDTO)
    @PostMapping
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<?> createAdvice(@RequestBody CreateAdviceRequest request) {
        try {
            User currentUser = getCurrentUser();
            UUID tenantId = currentUser.getTenantId();

            if (!taskService.userHasAccessToTask(currentUser.getUserId(), request.getTaskId(), tenantId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("You don't have permission to provide advice for this task");
            }

            taskAdviceService.createAdvice(
                    tenantId,
                    currentUser.getUserId(),
                    request.getTaskId(),
                    request.getAdviceText()
            );

            // Return updated list of advice for the task
            List<TaskAdviceDTO> advices = taskAdviceService.getAdviceForTaskAsDTO(request.getTaskId());

            TaskAdviceListResponse response = TaskAdviceListResponse.builder()
                    .advices(advices)
                    .build();

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error creating advice: " + e.getMessage());
        }
    }

    // Get all advice for a task
    @GetMapping("/task/{taskId}")
    @PreAuthorize("hasAnyRole('MANAGER', 'SUPERVISOR', 'ADMIN', 'EMPLOYEE')")
    public ResponseEntity<?> getAdviceForTask(@PathVariable int taskId) {
        try {
            User currentUser = getCurrentUser();
            UUID tenantId = currentUser.getTenantId();

            if (!taskService.userHasAccessToTask(currentUser.getUserId(), taskId, tenantId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("You don't have permission to access advice for this task");
            }

            List<TaskAdviceDTO> advices = taskAdviceService.getAdviceForTaskAsDTO(taskId);

            TaskAdviceListResponse response = TaskAdviceListResponse.builder()
                    .advices(advices)
                    .build();

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error retrieving advice: " + e.getMessage());
        }
    }

    // Get all advice provided by the current manager
    @GetMapping("/provided")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<?> getAdviceProvidedByMe() {
        try {
            User currentUser = getCurrentUser();
            UUID tenantId = currentUser.getTenantId();

            List<TaskAdviceDTO> adviceDTOs = taskAdviceService.getAdviceByManager(tenantId, currentUser.getUserId());

            TaskAdviceListResponse response = TaskAdviceListResponse.builder()
                    .advices(adviceDTOs)
                    .build();

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error retrieving advice: " + e.getMessage());
        }
    }
}
