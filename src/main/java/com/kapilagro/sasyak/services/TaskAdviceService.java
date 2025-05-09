package com.kapilagro.sasyak.services;

import com.kapilagro.sasyak.model.Task;
import com.kapilagro.sasyak.model.TaskAdviceDTO;
import com.kapilagro.sasyak.repository.TaskAdviceRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class TaskAdviceService {

    private final TaskAdviceRepo taskAdviceRepo;

    @Autowired
    public TaskAdviceService(TaskAdviceRepo taskAdviceRepo) {
        this.taskAdviceRepo = taskAdviceRepo;
    }

    // Get advice tasks provided by manager → return as DTO list
    public List<TaskAdviceDTO> getAdviceByManager(UUID tenantId, int managerId) {
        // Fetch tasks (page=0, size=1000 for simplicity; adjust as needed)
        List<Task> tasksWithAdvice = taskAdviceRepo.getTasksWithAdviceByManager(tenantId, managerId, 0, 1000);

        // Convert to DTOs
        return tasksWithAdvice.stream()
                .map(this::convertTaskToAdviceDTO)
                .collect(Collectors.toList());
    }

    // Get advice for a specific task → return as DTO list
    public List<TaskAdviceDTO> getAdviceForTaskAsDTO(int taskId) {
        // For single task — just fetch it directly (simulate method)
        // NOTE: You don't have a repo method yet for task by ID + advice, so I'll show that next

        Task task = taskAdviceRepo.getTaskByIdIfHasAdvice(taskId);
        if (task == null) {
            return List.of(); // Return empty if no advice found
        }

        return List.of(convertTaskToAdviceDTO(task));
    }

    // Create advice for a task
    public void createAdvice(UUID tenantId, int managerId, int taskId, String adviceText) {
        taskAdviceRepo.updateAdviceWithManager(taskId, adviceText, managerId);
    }

    // Helper method: Convert Task → TaskAdviceDTO
    private TaskAdviceDTO convertTaskToAdviceDTO(Task task) {
        return TaskAdviceDTO.builder()
                .taskId(task.getTaskId())
                .adviceText(task.getAdvice())
                .createdAt(task.getAdviceCreatedAt())
                .build();
    }
}
