package com.kapilagro.sasyak.model;

import lombok.*;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Component
@Getter
@Setter
@Builder
public class TaskAdvice {
    private int adviceId;
    private UUID tenantId;
    private int taskId;
    private int managerId;
    private String adviceText;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}